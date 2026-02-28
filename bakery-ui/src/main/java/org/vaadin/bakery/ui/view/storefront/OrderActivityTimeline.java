package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.service.OrderActivityService;
import org.vaadin.bakery.ui.event.DataChangeSignals;
import org.vaadin.bakery.uimodel.data.OrderActivity;
import org.vaadin.bakery.uimodel.type.OrderActivityType;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Activity timeline panel for an order, showing system events and staff messages
 * with a message input area at the bottom. Unread messages are marked as read
 * when they become visible in the scroller viewport.
 */
public class OrderActivityTimeline extends Composite<VerticalLayout> implements HasSize, HasStyle {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private final transient OrderActivityService orderActivityService;
    private final Long orderId;
    private final Div entriesContainer;
    private final Scroller scroller;
    private final Button newMessagesButton;
    private final Map<Long, Div> unreadEntryComponents;
    private final List<Long> entryActivityIds;

    private LocalDateTime lastLoadedTimestamp;
    private boolean initialLoadDone;
    private int previousEntryCount;
    private Div unreadDivider;

    /**
     * Creates the activity timeline for the given order.
     *
     * @param orderActivityService the service for loading and posting activities
     * @param orderId the order ID
     */
    public OrderActivityTimeline(OrderActivityService orderActivityService, Long orderId) {
        this.orderActivityService = orderActivityService;
        this.orderId = orderId;

        // Component initializations
        var titleHeader = new H3("Activity");
        titleHeader.addClassNames(LumoUtility.Margin.NONE, LumoUtility.Margin.Bottom.SMALL);

        entriesContainer = new Div();
        entriesContainer.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.XSMALL,
                LumoUtility.Padding.SMALL
        );

        scroller = new Scroller(entriesContainer);
        scroller.setSizeFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        scroller.addClassName("timeline-scroller");

        newMessagesButton = new Button("New messages \u2193");
        newMessagesButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        newMessagesButton.addClassName("new-messages-button");
        newMessagesButton.setVisible(false);
        newMessagesButton.addClickListener(_ -> scrollToBottom());

        unreadEntryComponents = new HashMap<>();
        entryActivityIds = new ArrayList<>();

        var inputArea = createInputArea();

        // Signal bindings
        initialLoadDone = false;
        previousEntryCount = 0;

        Signal.effect(this, () -> {
            DataChangeSignals.messageVersion().get();
            loadActivities();
        });

        // Content layout
        var content = getContent();
        content.setPadding(false);
        content.setSpacing(false);
        content.setWidth("350px");
        content.setSizeFull();
        content.add(titleHeader, scroller, newMessagesButton, inputArea);
        content.setFlexGrow(1, scroller);

        // Scroll listener for visibility-based read marking
        scroller.getElement().addEventListener("scroll", this::onTimelineScroll)
                .debounce(300, DebouncePhase.TRAILING)
                .addEventData("element.scrollTop")
                .addEventData("element.scrollHeight")
                .addEventData("element.clientHeight");
    }

    private HorizontalLayout createInputArea() {
        var messageInput = new TextArea();
        messageInput.setPlaceholder("Type a message...");
        messageInput.setWidthFull();
        messageInput.setMaxHeight("80px");
        messageInput.addClassName("message-input");

        var sendButton = new Button(new Icon(VaadinIcon.PAPERPLANE));
        sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        sendButton.addClickListener(_ -> {
            var text = messageInput.getValue();
            if (!text.isBlank()) {
                orderActivityService.postMessage(orderId, text.strip(), null);
                messageInput.clear();
            }
        });

        // Enter sends the message; Shift+Enter inserts a newline
        sendButton.addClickShortcut(Key.ENTER)
                .listenOn(messageInput)
                .resetFocusOnActiveElement();

        var inputLayout = new HorizontalLayout(messageInput, sendButton);
        inputLayout.setWidthFull();
        inputLayout.setAlignItems(FlexComponent.Alignment.END);
        inputLayout.addClassNames(LumoUtility.Padding.SMALL);

        return inputLayout;
    }

    private void loadActivities() {
        if (!initialLoadDone) {
            // Full load on first render
            var activities = orderActivityService.listByOrder(orderId);
            renderActivities(activities);
            scrollToFirstUnread(activities);
            triggerVisibilityCheck();
            initialLoadDone = true;
        } else if (lastLoadedTimestamp != null) {
            // Incremental load for live updates
            var newActivities = orderActivityService.listByOrderSince(orderId, lastLoadedTimestamp);
            if (!newActivities.isEmpty()) {
                appendActivities(newActivities);
                // Show "new messages" button instead of auto-scrolling
                if (!isScrolledToBottom()) {
                    newMessagesButton.setVisible(true);
                } else {
                    scrollToBottom();
                }
                triggerVisibilityCheck();
            }
        }
    }

    private void renderActivities(List<OrderActivity> activities) {
        entriesContainer.removeAll();
        entryActivityIds.clear();
        unreadEntryComponents.clear();
        unreadDivider = null;

        var firstUnreadFound = false;

        for (var activity : activities) {
            // Insert "New" divider before first unread staff message
            if (!firstUnreadFound && activity.getType() == OrderActivityType.STAFF_MESSAGE && !activity.isRead()) {
                var divider = new Div();
                divider.addClassName("unread-divider");
                divider.add(new Span("New"));
                entriesContainer.add(divider);
                entryActivityIds.add(null);
                unreadDivider = divider;
                firstUnreadFound = true;
            }

            var entry = createEntry(activity);
            entriesContainer.add(entry);

            if (activity.getType() == OrderActivityType.STAFF_MESSAGE && !activity.isRead()) {
                entryActivityIds.add(activity.getId());
                unreadEntryComponents.put(activity.getId(), entry);
            } else {
                entryActivityIds.add(null);
            }
        }

        previousEntryCount = activities.size();
        if (!activities.isEmpty()) {
            lastLoadedTimestamp = activities.getLast().getPostedAt();
        }
    }

    private void appendActivities(List<OrderActivity> newActivities) {
        for (var activity : newActivities) {
            var entry = createEntry(activity);
            entriesContainer.add(entry);

            if (activity.getType() == OrderActivityType.STAFF_MESSAGE && !activity.isRead()) {
                entryActivityIds.add(activity.getId());
                unreadEntryComponents.put(activity.getId(), entry);
            } else {
                entryActivityIds.add(null);
            }
        }
        previousEntryCount += newActivities.size();
        lastLoadedTimestamp = newActivities.getLast().getPostedAt();
    }

    private Div createEntry(OrderActivity activity) {
        var entry = new Div();

        if (activity.getType() == OrderActivityType.SYSTEM_EVENT) {
            entry.addClassName("timeline-system-event");
            entry.addClassNames(
                    LumoUtility.FontSize.SMALL,
                    LumoUtility.TextColor.SECONDARY,
                    LumoUtility.Padding.Vertical.XSMALL
            );

            var icon = new Icon(VaadinIcon.COG_O);
            icon.setSize("14px");
            icon.addClassNames(LumoUtility.TextColor.TERTIARY);

            var text = new Span(activity.getText());
            var time = createTimeSpan(activity.getPostedAt());

            var row = new HorizontalLayout(icon, text, time);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setWidthFull();
            row.setSpacing(false);
            row.addClassNames(LumoUtility.Gap.SMALL);

            entry.add(row);
        } else {
            entry.addClassName("timeline-staff-message");
            entry.addClassName("card");
            entry.addClassNames(LumoUtility.Padding.SMALL);

            if (!activity.isRead()) {
                entry.addClassName("timeline-unread");
            }

            var authorName = new Span(activity.getAuthorName() != null ? activity.getAuthorName() : "Unknown");
            authorName.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.SMALL);

            var time = createTimeSpan(activity.getPostedAt());

            var headerRow = new HorizontalLayout(authorName, time);
            headerRow.setWidthFull();
            headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            headerRow.setAlignItems(FlexComponent.Alignment.CENTER);

            entry.add(headerRow);

            if (activity.getReferencedItemName() != null) {
                var refLabel = new Span("Re: " + activity.getReferencedItemName());
                refLabel.addClassNames(
                        LumoUtility.FontSize.XSMALL,
                        LumoUtility.TextColor.SECONDARY,
                        LumoUtility.Display.BLOCK
                );
                refLabel.getStyle().set("font-style", "italic");
                entry.add(refLabel);
            }

            var text = new Span(activity.getText());
            text.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.Display.BLOCK);
            entry.add(text);
        }

        return entry;
    }

    private Span createTimeSpan(LocalDateTime time) {
        var timeSpan = new Span(time != null ? TIME_FORMATTER.format(time) : "");
        timeSpan.addClassNames(
                LumoUtility.FontSize.XSMALL,
                LumoUtility.TextColor.TERTIARY,
                LumoUtility.Whitespace.NOWRAP
        );
        return timeSpan;
    }

    /**
     * Handles debounced scroll events on the timeline scroller.
     * Extracts scroll dimensions from event data and marks visible entries as read.
     */
    private void onTimelineScroll(DomEvent event) {
        var data = event.getEventData();
        var scrollTop = data.get("element.scrollTop").doubleValue();
        var scrollHeight = data.get("element.scrollHeight").doubleValue();
        var clientHeight = data.get("element.clientHeight").doubleValue();
        markVisibleEntries(scrollTop, scrollHeight, clientHeight);
    }

    /**
     * Estimates which entries are visible based on scroll position and marks
     * unread ones as read. Uses average entry height for visibility estimation.
     */
    private void markVisibleEntries(double scrollTop, double scrollHeight, double clientHeight) {
        if (entryActivityIds.isEmpty() || unreadEntryComponents.isEmpty()) {
            return;
        }

        var entryCount = entryActivityIds.size();
        var avgHeight = scrollHeight / entryCount;
        if (avgHeight <= 0) {
            return;
        }

        // Estimate visible range with ±1 buffer for height variation
        var firstVisible = Math.max(0, (int) (scrollTop / avgHeight) - 1);
        var lastVisible = Math.min(entryCount - 1, (int) ((scrollTop + clientHeight) / avgHeight) + 1);

        var readIds = new HashSet<Long>();
        for (int i = firstVisible; i <= lastVisible; i++) {
            var activityId = entryActivityIds.get(i);
            if (activityId != null && unreadEntryComponents.containsKey(activityId)) {
                readIds.add(activityId);
                var div = unreadEntryComponents.remove(activityId);
                div.removeClassName("timeline-unread");
                entryActivityIds.set(i, null);
            }
        }

        // Remove divider if all unread entries are now read
        if (unreadEntryComponents.isEmpty() && unreadDivider != null) {
            entriesContainer.remove(unreadDivider);
            unreadDivider = null;
        }

        if (!readIds.isEmpty()) {
            orderActivityService.markActivitiesAsRead(readIds);
        }
    }

    /**
     * Triggers a one-time visibility check by querying the scroller's current
     * dimensions. Catches entries that are immediately visible without scrolling
     * (e.g., short timelines where all entries fit in the viewport).
     */
    private void triggerVisibilityCheck() {
        if (unreadEntryComponents.isEmpty()) {
            return;
        }
        scroller.getElement().executeJs(
                "return [this.scrollTop, this.scrollHeight, this.clientHeight]"
        ).then((JsonNode dims) ->
                markVisibleEntries(dims.get(0).doubleValue(), dims.get(1).doubleValue(), dims.get(2).doubleValue())
        );
    }

    private void scrollToFirstUnread(List<OrderActivity> activities) {
        // Find the index of the first unread staff message
        for (int i = 0; i < activities.size(); i++) {
            var activity = activities.get(i);
            if (activity.getType() == OrderActivityType.STAFF_MESSAGE && !activity.isRead()) {
                // Scroll to the unread divider (positioned before the first unread message)
                // Account for the divider itself: element index = i (divider) not i+1 (message)
                scrollToIndex(i);
                return;
            }
        }
        // All read: scroll to bottom
        scrollToBottom();
    }

    private void scrollToBottom() {
        newMessagesButton.setVisible(false);
        scroller.getElement().executeJs("this.scrollTop = this.scrollHeight");
    }

    private void scrollToIndex(int index) {
        scroller.getElement().executeJs(
                "var items = this.querySelectorAll(':scope > * > *'); " +
                "if (items[$0]) items[$0].scrollIntoView({behavior: 'smooth', block: 'start'});",
                index);
    }

    private boolean isScrolledToBottom() {
        // Optimistic: assume not scrolled to bottom to show the indicator.
        // A proper check would require a JS callback, but for simplicity we compare entry counts.
        return previousEntryCount == 0;
    }
}
