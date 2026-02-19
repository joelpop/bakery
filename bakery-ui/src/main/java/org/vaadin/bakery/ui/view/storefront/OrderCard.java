package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.uimodel.data.OrderList;
import org.vaadin.bakery.uimodel.type.OrderStatus;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Card component for displaying an order in the storefront view.
 */
public class OrderCard extends Div {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    private final OrderList order;

    /**
     * Creates an order card displaying a summary of the given order.
     *
     * @param order the order to display
     * @param hasUnreadMessage whether this order has unread staff messages
     */
    public OrderCard(OrderList order, boolean hasUnreadMessage) {
        this.order = order;

        addClassName("order-card");
        addClassName("card");
        getStyle()
                .set("cursor", "pointer")
                .set("transition", "box-shadow 0.2s, transform 0.2s");

        // Add hover effect
        getElement().addEventListener("mouseenter", e ->
                getStyle()
                        .set("box-shadow", "0 4px 12px rgba(0,0,0,0.15)")
                        .set("transform", "translateY(-2px)"));
        getElement().addEventListener("mouseleave", e ->
                getStyle()
                        .set("box-shadow", "")
                        .set("transform", ""));

        addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL
        );

        // Header row: status badge + unread dot + time
        var header = createHeader(hasUnreadMessage);
        add(header);

        // Customer name
        var customerName = new Span(order.getCustomerName());
        customerName.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.FontWeight.SEMIBOLD
        );
        add(customerName);

        // Location
        if (order.getLocationName() != null) {
            var location = new Span(order.getLocationName());
            location.addClassNames(
                    LumoUtility.FontSize.SMALL,
                    LumoUtility.TextColor.SECONDARY
            );
            add(location);
        }

        // Items summary
        var items = new Span(order.getItemsSummary());
        items.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.Whitespace.NOWRAP,
                LumoUtility.Overflow.HIDDEN
        );
        items.getStyle().set("text-overflow", "ellipsis");
        add(items);

        // Footer: total + paid indicator
        var footer = createFooter();
        add(footer);

        // Click handler
        addClickListener(e -> fireEvent(new OrderClickEvent(this, order)));
    }

    private Div createHeader(boolean hasUnreadMessage) {
        var header = new Div();
        header.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.AlignItems.CENTER
        );

        var leftGroup = new Div();
        leftGroup.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.SMALL
        );

        var statusBadge = createStatusBadge(order.getStatus());
        leftGroup.add(statusBadge);

        if (hasUnreadMessage) {
            var unreadDot = new Span();
            unreadDot.addClassName("unread-dot");
            leftGroup.add(unreadDot);
        }

        var time = new Span(order.getDueTime() != null ?
                TIME_FORMATTER.format(order.getDueTime()) : "");
        time.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY
        );

        header.add(leftGroup, time);
        return header;
    }

    private Span createStatusBadge(OrderStatus status) {
        var badge = new Span(status.getDisplayName());
        badge.getElement().getThemeList().add("badge " + mapStatusToTheme(status));
        return badge;
    }

    private String mapStatusToTheme(OrderStatus status) {
        return switch (status) {
            case IN_REVIEW -> "primary";
            case VERIFIED -> "success";
            case IN_PROGRESS -> "warning";
            case PRODUCED, PACKAGED, IN_TRANSIT -> "";
            case READY_FOR_PICK_UP -> "success";
            case PICKED_UP -> "contrast";
            case CANCELED -> "error";
        };
    }

    private Div createFooter() {
        var footer = new Div();
        footer.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Margin.Top.SMALL
        );

        var total = new Span(CURRENCY_FORMAT.format(order.getTotal()));
        total.addClassNames(
                LumoUtility.FontWeight.BOLD
        );

        if (order.isPaid()) {
            var paidBadge = new Span("Paid");
            paidBadge.getElement().getThemeList().add("badge success small");
            footer.add(total, paidBadge);
        } else {
            footer.add(total);
        }

        return footer;
    }

    public OrderList getOrder() {
        return order;
    }

    /** Event fired when the order card is clicked. */
    public static class OrderClickEvent extends ComponentEvent<OrderCard> {
        private final OrderList order;

        public OrderClickEvent(OrderCard source, OrderList order) {
            super(source, false);
            this.order = order;
        }

        public OrderList getOrder() {
            return order;
        }
    }

    /** Registers a listener for order click events. */
    public Registration addOrderClickListener(ComponentEventListener<OrderClickEvent> listener) {
        return addListener(OrderClickEvent.class, listener);
    }
}
