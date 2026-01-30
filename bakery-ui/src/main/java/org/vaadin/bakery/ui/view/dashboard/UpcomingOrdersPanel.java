package org.vaadin.bakery.ui.view.dashboard;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.uimodel.data.OrderDashboard;
import org.vaadin.bakery.uimodel.type.OrderStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Panel showing upcoming orders for the dashboard.
 */
public class UpcomingOrdersPanel extends Div {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d");

    private final Div ordersContainer;

    public UpcomingOrdersPanel() {
        addClassName("upcoming-orders-panel");
        getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("padding", "var(--lumo-space-m)");

        addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN
        );

        // Header
        var header = new H3("Upcoming Orders");
        header.addClassNames(
                LumoUtility.Margin.NONE,
                LumoUtility.Margin.Bottom.MEDIUM,
                LumoUtility.FontSize.MEDIUM
        );
        add(header);

        // Orders container
        ordersContainer = new Div();
        ordersContainer.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL
        );
        add(ordersContainer);
    }

    public void setOrders(List<OrderDashboard> orders) {
        ordersContainer.removeAll();

        if (orders == null || orders.isEmpty()) {
            var emptyMessage = new Span("No upcoming orders");
            emptyMessage.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            ordersContainer.add(emptyMessage);
            return;
        }

        for (var order : orders) {
            ordersContainer.add(createOrderRow(order));
        }
    }

    private Div createOrderRow(OrderDashboard order) {
        var row = new Div();
        row.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.Gap.SMALL,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Padding.Vertical.XSMALL
        );
        row.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        // Status badge
        var statusBadge = createStatusBadge(order.getStatus());

        // Paid indicator
        if (order.isPaid()) {
            var paidBadge = new Span("$");
            paidBadge.getElement().getThemeList().add("badge success small");
            row.add(statusBadge, paidBadge);
        } else {
            row.add(statusBadge);
        }

        // Day and time
        var timeInfo = new Div();
        timeInfo.addClassNames(LumoUtility.FontSize.SMALL);

        var dayText = formatDay(order.getDueDate());
        var timeText = order.getDueTime() != null ? TIME_FORMATTER.format(order.getDueTime()) : "";
        timeInfo.setText(dayText + " " + timeText);
        row.add(timeInfo);

        // Location
        var location = new Span(order.getLocationName());
        location.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        row.add(location);

        // Spacer
        var spacer = new Span();
        spacer.addClassNames(LumoUtility.Flex.GROW);
        row.add(spacer);

        // Customer and items (right side)
        var customerInfo = new Div();
        customerInfo.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.AlignItems.END
        );

        var customerName = new Span(order.getCustomerName());
        customerName.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.SEMIBOLD);

        var items = new Span(order.getItemsSummary());
        items.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);
        items.getStyle()
                .set("max-width", "150px")
                .set("white-space", "nowrap")
                .set("overflow", "hidden")
                .set("text-overflow", "ellipsis");

        customerInfo.add(customerName, items);
        row.add(customerInfo);

        return row;
    }

    private Span createStatusBadge(OrderStatus status) {
        var badge = new Span(status.getDisplayName().substring(0, Math.min(3, status.getDisplayName().length())));
        badge.getElement().getThemeList().add("badge small " + mapStatusToTheme(status));
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

    private String formatDay(LocalDate date) {
        var today = LocalDate.now();
        if (date.equals(today)) {
            return "Today";
        } else if (date.equals(today.plusDays(1))) {
            return "Tomorrow";
        } else {
            return DATE_FORMATTER.format(date);
        }
    }
}
