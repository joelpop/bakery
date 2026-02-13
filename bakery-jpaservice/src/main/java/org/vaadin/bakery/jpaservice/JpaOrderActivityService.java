package org.vaadin.bakery.jpaservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.bakery.jpamodel.code.OrderActivityTypeCode;
import org.vaadin.bakery.jpamodel.entity.OrderActivityEntity;
import org.vaadin.bakery.jpaclient.repository.OrderActivityRepository;
import org.vaadin.bakery.jpaclient.repository.OrderItemRepository;
import org.vaadin.bakery.jpaclient.repository.OrderRepository;
import org.vaadin.bakery.jpaclient.repository.UserRepository;
import org.vaadin.bakery.jpaservice.mapper.InstantMapper;
import org.vaadin.bakery.jpaservice.mapper.OrderActivityMapper;
import org.vaadin.bakery.service.CurrentUserService;
import org.vaadin.bakery.service.DataChangeNotifier;
import org.vaadin.bakery.service.MessageNotification;
import org.vaadin.bakery.service.OrderActivityService;
import org.vaadin.bakery.service.UnreadMessageTracker;
import org.vaadin.bakery.uimodel.data.OrderActivity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * JPA implementation of the order activity service.
 * <p>
 * Uses a session-scoped {@link UnreadMessageTracker} to determine per-user
 * read state, rather than relying solely on the global persisted {@code read} flag.
 */
@Service
@Transactional
public class JpaOrderActivityService implements OrderActivityService {

    private static final int TEXT_PREVIEW_MAX_LENGTH = 80;

    private final OrderActivityRepository orderActivityRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderActivityMapper orderActivityMapper;
    private final InstantMapper instantMapper;
    private final CurrentUserService currentUserService;
    private final DataChangeNotifier dataChangeNotifier;
    private final UnreadMessageTracker unreadMessageTracker;

    /**
     * Creates the service with all required dependencies.
     */
    public JpaOrderActivityService(OrderActivityRepository orderActivityRepository,
                                   OrderRepository orderRepository,
                                   UserRepository userRepository,
                                   OrderItemRepository orderItemRepository,
                                   OrderActivityMapper orderActivityMapper,
                                   InstantMapper instantMapper,
                                   CurrentUserService currentUserService,
                                   DataChangeNotifier dataChangeNotifier,
                                   UnreadMessageTracker unreadMessageTracker) {
        this.orderActivityRepository = orderActivityRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderActivityMapper = orderActivityMapper;
        this.instantMapper = instantMapper;
        this.currentUserService = currentUserService;
        this.dataChangeNotifier = dataChangeNotifier;
        this.unreadMessageTracker = unreadMessageTracker;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderActivity> listByOrder(Long orderId) {
        ensureTrackerInitialized();
        var entities = orderActivityRepository.findByOrderIdOrderByPostedAtAsc(orderId);
        return mapWithReadState(entities, orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderActivity> listByOrderSince(Long orderId, LocalDateTime since) {
        ensureTrackerInitialized();
        var sinceInstant = instantMapper.toServerTime(since);
        var entities = orderActivityRepository.findByOrderIdAndPostedAtAfterOrderByPostedAtAsc(orderId, sinceInstant);
        return mapWithReadState(entities, orderId);
    }

    @Override
    public OrderActivity postMessage(Long orderId, String text, Long referencedItemId) {
        var currentUser = currentUserService.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No authenticated user"));

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        var author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new IllegalStateException("User entity not found: " + currentUser.getId()));

        var activity = new OrderActivityEntity();
        activity.setOrder(order);
        activity.setType(OrderActivityTypeCode.STAFF_MESSAGE);
        activity.setText(text);
        activity.setAuthor(author);
        activity.setPostedAt(Instant.now());
        activity.setRead(false);

        if (referencedItemId != null) {
            var item = orderItemRepository.findById(referencedItemId)
                    .orElseThrow(() -> new IllegalArgumentException("Order item not found: " + referencedItemId));
            activity.setReferencedItem(item);
        }

        var saved = orderActivityRepository.save(activity);

        var textPreview = text.length() > TEXT_PREVIEW_MAX_LENGTH
                ? text.substring(0, TEXT_PREVIEW_MAX_LENGTH) + "\u2026"
                : text;
        var notification = new MessageNotification(
                orderId,
                order.getLocation().getId(),
                currentUser.getId(),
                currentUser.getFirstName() + " " + currentUser.getLastName(),
                textPreview
        );
        dataChangeNotifier.notifyMessage(notification);

        var model = orderActivityMapper.toModel(saved);
        model.setRead(true); // author sees their own message as read
        return model;
    }

    @Override
    public void markActivitiesAsRead(Set<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return;
        }
        unreadMessageTracker.markAsRead(activityIds);
        orderActivityRepository.markAsReadByIds(activityIds);
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.MESSAGE);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findOrderIdsWithUnreadMessages(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptySet();
        }
        ensureTrackerInitialized();
        refreshTrackerForOrders(orderIds);
        return unreadMessageTracker.getUnreadOrderIds(orderIds);
    }

    /**
     * Maps entities to UI models, setting per-user read state from the tracker.
     * Newly discovered unread messages (not yet in the tracker) are added.
     * Uses the user ID stored in the tracker (safe for Push threads).
     */
    private List<OrderActivity> mapWithReadState(List<OrderActivityEntity> entities, Long orderId) {
        var currentUserId = unreadMessageTracker.getUserId();

        var models = new ArrayList<OrderActivity>(entities.size());
        for (var entity : entities) {
            var model = orderActivityMapper.toModel(entity);

            if (entity.getType() == OrderActivityTypeCode.STAFF_MESSAGE) {
                // Add newly discovered globally-unread messages to the tracker
                if (!entity.isRead()
                        && currentUserId != null
                        && entity.getAuthor() != null
                        && !currentUserId.equals(entity.getAuthor().getId())
                        && !unreadMessageTracker.isUnread(entity.getId())) {
                    unreadMessageTracker.addUnread(entity.getId(), orderId);
                }
                // Read state from tracker (author's own messages are never in tracker)
                model.setRead(!unreadMessageTracker.isUnread(entity.getId()));
            } else {
                // System events are always read
                model.setRead(true);
            }

            models.add(model);
        }
        return models;
    }

    /**
     * Initializes the session tracker with all globally-unread staff messages
     * not authored by the current user. Called lazily on first access.
     * Stores the user ID in the tracker for use in Push threads where
     * Spring Security context is not available.
     */
    private void ensureTrackerInitialized() {
        if (unreadMessageTracker.isInitialized()) {
            return;
        }
        var currentUserId = currentUserService.getCurrentUser()
                .map(u -> u.getId())
                .orElse(null);
        if (currentUserId == null) {
            unreadMessageTracker.initialize(null, Collections.emptyMap());
            return;
        }
        var rows = orderActivityRepository.findGloballyUnreadStaffMessages(currentUserId);
        var map = new HashMap<Long, Long>(rows.size());
        for (var row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        unreadMessageTracker.initialize(currentUserId, map);
    }

    /**
     * Refreshes the tracker with any new globally-unread messages for the given orders.
     * Uses the user ID stored in the tracker (safe for Push threads).
     */
    private void refreshTrackerForOrders(Collection<Long> orderIds) {
        var currentUserId = unreadMessageTracker.getUserId();
        if (currentUserId == null) {
            return;
        }
        var rows = orderActivityRepository.findGloballyUnreadStaffMessagesForOrders(currentUserId, orderIds);
        for (var row : rows) {
            var activityId = (Long) row[0];
            var orderId = (Long) row[1];
            if (!unreadMessageTracker.isUnread(activityId)) {
                unreadMessageTracker.addUnread(activityId, orderId);
            }
        }
    }
}
