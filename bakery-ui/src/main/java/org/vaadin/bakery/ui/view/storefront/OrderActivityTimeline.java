package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.ComponentEffect;
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
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.service.OrderActivityService;
import org.vaadin.bakery.ui.event.DataChangeSignals;
import org.vaadin.bakery.uimodel.data.OrderActivity;
import org.vaadin.bakery.uimodel.type.OrderActivityType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Activity timeline panel for an order, showing system events and staff messages
 * with a message input area at the bottom.
 */
public class OrderActivityTimeline extends VerticalLayout {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, h:mm a");

    private final OrderActivityService orderActivityService;
    private final Long orderId;
    private final Div entriesContainer;
    private final Scroller scroller;
    private final Button newMessagesButton;

    private LocalDateTime lastLoadedTimestamp;
    private boolean initialLoadDone;
    private int previousEntryCount;

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
        setPadding(false);
        setSpacing(false);
        setWidth("350px");
        setSizeFull();

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

        var inputArea = createInputArea();

        // Signal bindings
        initialLoadDone = false;
        previousEntryCount = 0;

        ComponentEffect.effect(this, () -> {
            DataChangeSignals.messageVersion().value();
            loadActivities();
        });

        // Layout assembly
        add(titleHeader, scroller, newMessagesButton, inputArea);
        setFlexGrow(1, scroller);
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
            }
        }
    }

    private void renderActivities(List<OrderActivity> activities) {
        entriesContainer.removeAll();
        var firstUnreadFound = false;

        for (var activity : activities) {
            // Insert "New" divider before first unread staff message
            if (!firstUnreadFound && activity.getType() == OrderActivityType.STAFF_MESSAGE && !activity.isRead()) {
                var divider = new Div();
                divider.addClassName("unread-divider");
                divider.add(new Span("New"));
                entriesContainer.add(divider);
                firstUnreadFound = true;
            }

            entriesContainer.add(createEntry(activity));
        }

        previousEntryCount = activities.size();
        if (!activities.isEmpty()) {
            lastLoadedTimestamp = activities.getLast().getPostedAt();
        }
    }

    private void appendActivities(List<OrderActivity> newActivities) {
        for (var activity : newActivities) {
            entriesContainer.add(createEntry(activity));
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
