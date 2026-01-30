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

    public OrderCard(OrderList order) {
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

        // Header row: status badge + time
        var header = createHeader();
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
                LumoUtility.TextColor.SECONDARY
        );
        items.getStyle().set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");
        add(items);

        // Footer: total + paid indicator
        var footer = createFooter();
        add(footer);

        // Click handler
        addClickListener(e -> fireEvent(new OrderClickEvent(this, order)));
    }

    private Div createHeader() {
        var header = new Div();
        header.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.AlignItems.CENTER
        );

        var statusBadge = createStatusBadge(order.getStatus());

        var time = new Span(order.getDueTime() != null ?
                TIME_FORMATTER.format(order.getDueTime()) : "");
        time.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY
        );

        header.add(statusBadge, time);
        return header;
    }

    private Span createStatusBadge(OrderStatus status) {
        var badge = new Span(status.getDisplayName());
        badge.getElement().getThemeList().add("badge " + mapStatusToTheme(status));
        return badge;
    }

    private String mapStatusToTheme(OrderStatus status) {
        return switch (status) {
            case NEW -> "primary";
            case VERIFIED -> "success";
            case NOT_OK -> "error";
            case CANCELLED -> "contrast";
            case IN_PROGRESS -> "warning";
            case BAKED, PACKAGED -> "";
            case READY_FOR_PICK_UP -> "success";
            case PICKED_UP -> "contrast";
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

    // Event for order click
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

    public Registration addOrderClickListener(ComponentEventListener<OrderClickEvent> listener) {
        return addListener(OrderClickEvent.class, listener);
    }
}
