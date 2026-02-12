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
import org.vaadin.bakery.uimodel.data.OrderActivity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * JPA implementation of the order activity service.
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

    public JpaOrderActivityService(OrderActivityRepository orderActivityRepository,
                                   OrderRepository orderRepository,
                                   UserRepository userRepository,
                                   OrderItemRepository orderItemRepository,
                                   OrderActivityMapper orderActivityMapper,
                                   InstantMapper instantMapper,
                                   CurrentUserService currentUserService,
                                   DataChangeNotifier dataChangeNotifier) {
        this.orderActivityRepository = orderActivityRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderActivityMapper = orderActivityMapper;
        this.instantMapper = instantMapper;
        this.currentUserService = currentUserService;
        this.dataChangeNotifier = dataChangeNotifier;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderActivity> listByOrder(Long orderId) {
        var entities = orderActivityRepository.findByOrderIdOrderByPostedAtAsc(orderId);
        return orderActivityMapper.toModelList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderActivity> listByOrderSince(Long orderId, LocalDateTime since) {
        var sinceInstant = instantMapper.toServerTime(since);
        var entities = orderActivityRepository.findByOrderIdAndPostedAtAfterOrderByPostedAtAsc(orderId, sinceInstant);
        return orderActivityMapper.toModelList(entities);
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

        return orderActivityMapper.toModel(saved);
    }

    @Override
    public void markOrderAsRead(Long orderId) {
        orderActivityRepository.markAllReadByOrderId(orderId);
        dataChangeNotifier.notifyChange(DataChangeNotifier.EntityType.MESSAGE);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> findOrderIdsWithUnreadMessages(Collection<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Collections.emptySet();
        }
        return orderActivityRepository.findOrderIdsWithUnreadMessages(orderIds);
    }
}
