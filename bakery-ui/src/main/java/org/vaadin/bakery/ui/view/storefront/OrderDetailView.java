package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.ComponentEffect;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.signals.local.ValueSignal;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.bakery.service.OrderService;
import org.vaadin.bakery.uimodel.data.OrderDetail;
import org.vaadin.bakery.uimodel.data.OrderItemDetail;
import org.vaadin.bakery.uimodel.type.OrderStatus;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

/**
 * View for displaying and managing a single order.
 */
@Route("orders/:orderId")
@PageTitle("Order Details")
@RolesAllowed({"ADMIN", "BARISTA", "BAKER"})
public class OrderDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final OrderService orderService;

    // Signal - primary state
    private final transient ValueSignal<OrderDetail> orderSignal;

    private final Span orderIdLabel;
    private final Span statusBadge;
    private final Span customerNameLabel;
    private final Span customerPhoneLabel;
    private final Span locationLabel;
    private final Span dueDateTimeLabel;
    private final Span additionalDetailsLabel;
    private final Span totalLabel;
    private final Span paidBadge;
    private final Span createdByLabel;
    private final Span updatedByLabel;

    private final Grid<OrderItemDetail> itemsGrid;
    private final HorizontalLayout actionButtons;

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    public OrderDetailView(OrderService orderService) {
        this.orderService = orderService;

        // Component initializations
        addClassName("order-detail-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        orderIdLabel = new Span();
        orderIdLabel.addClassNames(LumoUtility.TextColor.SECONDARY);

        // Signal definitions
        orderSignal = new ValueSignal<>(null);

        statusBadge = new Span();
        customerNameLabel = new Span();
        customerPhoneLabel = new Span();
        customerPhoneLabel.addClassNames(LumoUtility.TextColor.SECONDARY);
        locationLabel = new Span();
        dueDateTimeLabel = new Span();
        dueDateTimeLabel.addClassNames(LumoUtility.TextColor.SECONDARY);
        additionalDetailsLabel = new Span();
        totalLabel = new Span();
        totalLabel.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);
        paidBadge = new Span();
        createdByLabel = new Span();
        createdByLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        updatedByLabel = new Span();
        updatedByLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        itemsGrid = new Grid<>();
        itemsGrid.setSizeFull();
        itemsGrid.addColumn(OrderItemDetail::getProductName)
                .setHeader("Product")
                .setFlexGrow(2);
        itemsGrid.addColumn(OrderItemDetail::getProductSize)
                .setHeader("Size")
                .setFlexGrow(1);
        itemsGrid.addColumn(OrderItemDetail::getQuantity)
                .setHeader("Qty")
                .setFlexGrow(0)
                .setWidth("60px");
        itemsGrid.addColumn(item -> CURRENCY_FORMAT.format(item.getUnitPrice()))
                .setHeader("Unit Price")
                .setFlexGrow(0)
                .setWidth("100px");
        itemsGrid.addColumn(item -> CURRENCY_FORMAT.format(item.getLineTotal()))
                .setHeader("Total")
                .setFlexGrow(0)
                .setWidth("100px");
        itemsGrid.addColumn(OrderItemDetail::getDetails)
                .setHeader("Notes")
                .setFlexGrow(1);

        actionButtons = new HorizontalLayout();
        actionButtons.setWidthFull();
        actionButtons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        actionButtons.setSpacing(true);
        actionButtons.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        // Signal bindings
        ComponentEffect.effect(this, () -> {
            var order = orderSignal.value();
            if (order == null) return;

            orderIdLabel.setText("#" + order.getId());

            // Status badge
            statusBadge.setText(order.getStatus().getDisplayName());
            statusBadge.getElement().getThemeList().clear();
            statusBadge.getElement().getThemeList().add("badge " + mapStatusToTheme(order.getStatus()));

            // Customer info
            customerNameLabel.setText(order.getCustomerName());
            customerPhoneLabel.setText(order.getCustomerPhone() != null ? order.getCustomerPhone() : "");

            // Location and time
            locationLabel.setText(order.getLocationName());
            var dateTime = "";
            if (order.getDueDate() != null) {
                dateTime = order.getDueDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"));
                if (order.getDueTime() != null) {
                    dateTime += " at " + order.getDueTime().format(DateTimeFormatter.ofPattern("h:mm a"));
                }
            }
            dueDateTimeLabel.setText(dateTime);

            // Additional details
            additionalDetailsLabel.setText(order.getAdditionalDetails() != null ?
                    order.getAdditionalDetails() : "None");
            additionalDetailsLabel.getClassNames().remove(LumoUtility.TextColor.SECONDARY);
            if (order.getAdditionalDetails() == null || order.getAdditionalDetails().isBlank()) {
                additionalDetailsLabel.addClassName(LumoUtility.TextColor.SECONDARY);
            }

            // Payment
            totalLabel.setText(CURRENCY_FORMAT.format(order.getTotal()));
            paidBadge.setText(order.isPaid() ? "Paid" : "Unpaid");
            paidBadge.getElement().getThemeList().clear();
            paidBadge.getElement().getThemeList().add("badge " + (order.isPaid() ? "success" : "error"));

            // History
            createdByLabel.setText("Created by: " + (order.getCreatedByName() != null ?
                    order.getCreatedByName() : "Unknown"));
            updatedByLabel.setText("Updated by: " + (order.getUpdatedByName() != null ?
                    order.getUpdatedByName() : "-"));

            // Items
            itemsGrid.setItems(order.getItems());
        });

        // Action buttons effect
        ComponentEffect.effect(this, () -> {
            var order = orderSignal.value();
            actionButtons.removeAll();
            if (order == null) return;

            // Status change button
            if (!order.getStatus().isTerminal()) {
                var changeStatusButton = new Button("Change Status", new Icon(VaadinIcon.EDIT));
                changeStatusButton.addClickListener(e -> openStatusChangeDialog());
                actionButtons.add(changeStatusButton);
            }

            // Mark as paid button
            if (!order.isPaid() && !order.getStatus().isTerminal()) {
                var markPaidButton = new Button("Mark as Paid", new Icon(VaadinIcon.MONEY));
                markPaidButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                markPaidButton.addClickListener(e -> markAsPaid());
                actionButtons.add(markPaidButton);
            }

            // Cancel button (only for pre-production orders)
            if (order.getStatus().isPreProduction()) {
                var cancelButton = new Button("Cancel Order", new Icon(VaadinIcon.CLOSE));
                cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                cancelButton.addClickListener(e -> confirmCancel());
                actionButtons.add(cancelButton);
            }
        });

        // Header
        var backButton = new Button(new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(e -> navigateBack());

        var title = new Span("Order Details");
        title.addClassNames(
                LumoUtility.FontSize.XLARGE,
                LumoUtility.FontWeight.SEMIBOLD
        );

        var leftSection = new Div();
        leftSection.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.SMALL
        );
        leftSection.add(backButton, title, orderIdLabel);

        var header = new Div();
        header.addClassName("view-header");
        header.add(leftSection, statusBadge);

        // Order info section
        var orderInfoSection = new VerticalLayout();
        orderInfoSection.setWidth("350px");
        orderInfoSection.setPadding(false);
        orderInfoSection.setSpacing(false);
        orderInfoSection.add(createInfoCard("Customer", customerNameLabel, customerPhoneLabel));
        orderInfoSection.add(createInfoCard("Pickup", locationLabel, dueDateTimeLabel));
        orderInfoSection.add(createInfoCard("Notes", additionalDetailsLabel));
        orderInfoSection.add(createInfoCard("Payment", totalLabel, paidBadge));
        orderInfoSection.add(createInfoCard("History", createdByLabel, updatedByLabel));

        // Items section
        var itemsHeader = new H3("Order Items");
        itemsHeader.addClassNames(LumoUtility.Margin.NONE, LumoUtility.Margin.Bottom.MEDIUM);

        var itemsSection = new VerticalLayout();
        itemsSection.setSizeFull();
        itemsSection.setPadding(false);
        itemsSection.add(itemsHeader, itemsGrid);
        itemsSection.setFlexGrow(1, itemsGrid);

        // Layout assembly
        var content = new HorizontalLayout();
        content.setSizeFull();
        content.setSpacing(true);
        content.add(orderInfoSection, itemsSection);

        var contentWrapper = new Div();
        contentWrapper.addClassNames(LumoUtility.Padding.MEDIUM);
        contentWrapper.setSizeFull();
        contentWrapper.add(content);

        add(header, contentWrapper, actionButtons);
        setFlexGrow(1, contentWrapper);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var orderIdParam = event.getRouteParameters().get("orderId");
        if (orderIdParam.isEmpty()) {
            navigateBack();
            return;
        }

        try {
            var orderId = Long.parseLong(orderIdParam.get());
            var optOrder = orderService.get(orderId);
            if (optOrder.isEmpty()) {
                Notification.show("Order not found", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                navigateBack();
                return;
            }
            orderSignal.value(optOrder.get());
        } catch (NumberFormatException e) {
            navigateBack();
        }
    }

    private Div createInfoCard(String title, Span... content) {
        var card = new Div();
        card.addClassName("card");
        card.addClassNames(LumoUtility.Margin.Bottom.SMALL);

        var titleSpan = new Span(title);
        titleSpan.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.Display.BLOCK,
                LumoUtility.Margin.Bottom.XSMALL
        );
        card.add(titleSpan);

        for (var span : content) {
            span.addClassNames(LumoUtility.Display.BLOCK);
            card.add(span);
        }

        return card;
    }

    private void openStatusChangeDialog() {
        var order = orderSignal.value();
        if (order == null) return;

        var dialog = new Dialog();
        dialog.setHeaderTitle("Change Order Status");
        dialog.setWidth("400px");

        var statusCombo = new ComboBox<OrderStatus>("New Status");
        statusCombo.setWidthFull();

        // Filter available statuses based on current status
        var availableStatuses = Arrays.stream(OrderStatus.values())
                .filter(s -> s != order.getStatus())
                .filter(s -> !s.isTerminal() || s == OrderStatus.CANCELLED)
                .toList();
        statusCombo.setItems(availableStatuses);
        statusCombo.setItemLabelGenerator(OrderStatus::getDisplayName);

        dialog.add(statusCombo);

        var cancelButton = new Button("Cancel", e -> dialog.close());
        var confirmButton = new Button("Update", e -> {
            if (statusCombo.getValue() != null) {
                updateStatus(statusCombo.getValue());
                dialog.close();
            }
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelButton, confirmButton);
        dialog.open();
    }

    private void updateStatus(OrderStatus newStatus) {
        var order = orderSignal.value();
        if (order == null) return;

        try {
            orderService.updateStatus(order.getId(), newStatus);
            order.setStatus(newStatus);
            orderSignal.value(order);
            Notification.show("Status updated", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Failed to update status: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void markAsPaid() {
        var order = orderSignal.value();
        if (order == null) return;

        try {
            orderService.markAsPaid(order.getId());
            order.setPaid(true);
            orderSignal.value(order);
            Notification.show("Order marked as paid", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Failed to mark as paid: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void confirmCancel() {
        var order = orderSignal.value();
        if (order == null) return;

        var dialog = new Dialog();
        dialog.setHeaderTitle("Cancel Order");
        dialog.add(new Span("Are you sure you want to cancel order #" + order.getId() + "?"));

        var cancelButton = new Button("No, keep it", e -> dialog.close());
        var confirmButton = new Button("Yes, cancel order", e -> {
            dialog.close();
            cancelOrder();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelButton, confirmButton);
        dialog.open();
    }

    private void cancelOrder() {
        var order = orderSignal.value();
        if (order == null) return;

        try {
            orderService.updateStatus(order.getId(), OrderStatus.CANCELLED);
            order.setStatus(OrderStatus.CANCELLED);
            orderSignal.value(order);
            Notification.show("Order cancelled", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Failed to cancel order: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
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

    private void navigateBack() {
        getUI().ifPresent(ui -> ui.navigate(StorefrontView.class));
    }
}
