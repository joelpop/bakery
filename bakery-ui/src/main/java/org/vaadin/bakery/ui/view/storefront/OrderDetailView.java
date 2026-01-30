package org.vaadin.bakery.ui.view.storefront;

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
    private OrderDetail order;

    private final Span orderIdLabel = new Span();
    private final Span statusBadge = new Span();
    private final Span customerNameLabel = new Span();
    private final Span customerPhoneLabel = new Span();
    private final Span locationLabel = new Span();
    private final Span dueDateTimeLabel = new Span();
    private final Span additionalDetailsLabel = new Span();
    private final Span totalLabel = new Span();
    private final Span paidBadge = new Span();
    private final Span createdByLabel = new Span();
    private final Span updatedByLabel = new Span();

    private final Grid<OrderItemDetail> itemsGrid = new Grid<>();
    private final HorizontalLayout actionButtons = new HorizontalLayout();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d 'at' h:mm a");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    public OrderDetailView(OrderService orderService) {
        this.orderService = orderService;

        addClassName("order-detail-view");
        setSizeFull();
        setPadding(true);

        // Header
        add(createHeader());

        // Main content
        var content = new HorizontalLayout();
        content.setSizeFull();
        content.setSpacing(true);

        // Left: Order info
        content.add(createOrderInfoSection());

        // Right: Items
        content.add(createItemsSection());

        add(content);

        // Actions
        add(createActionsSection());
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
            order = optOrder.get();
            populateView();
        } catch (NumberFormatException e) {
            navigateBack();
        }
    }

    private HorizontalLayout createHeader() {
        var header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        var backButton = new Button(new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(e -> navigateBack());

        var title = new H3("Order Details");
        title.addClassNames(LumoUtility.Margin.NONE);

        orderIdLabel.addClassNames(LumoUtility.TextColor.SECONDARY);

        var spacer = new Span();
        spacer.addClassNames(LumoUtility.Flex.GROW);

        header.add(backButton, title, orderIdLabel, spacer, statusBadge);
        return header;
    }

    private VerticalLayout createOrderInfoSection() {
        var section = new VerticalLayout();
        section.setWidth("350px");
        section.setPadding(false);
        section.setSpacing(false);

        section.add(createInfoCard("Customer", customerNameLabel, customerPhoneLabel));
        section.add(createInfoCard("Pickup", locationLabel, dueDateTimeLabel));
        section.add(createInfoCard("Notes", additionalDetailsLabel));
        section.add(createInfoCard("Payment", totalLabel, paidBadge));
        section.add(createInfoCard("History", createdByLabel, updatedByLabel));

        return section;
    }

    private Div createInfoCard(String title, Span... content) {
        var card = new Div();
        card.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Margin.Bottom.SMALL
        );

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

    private VerticalLayout createItemsSection() {
        var section = new VerticalLayout();
        section.setSizeFull();
        section.setPadding(false);

        var header = new H3("Order Items");
        header.addClassNames(LumoUtility.Margin.NONE, LumoUtility.Margin.Bottom.MEDIUM);
        section.add(header);

        configureItemsGrid();
        section.add(itemsGrid);
        section.setFlexGrow(1, itemsGrid);

        return section;
    }

    private void configureItemsGrid() {
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
    }

    private HorizontalLayout createActionsSection() {
        actionButtons.setWidthFull();
        actionButtons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        actionButtons.setSpacing(true);
        actionButtons.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        return actionButtons;
    }

    private void populateView() {
        orderIdLabel.setText("#" + order.getId());

        // Status badge
        statusBadge.removeAll();
        statusBadge.setText(order.getStatus().getDisplayName());
        statusBadge.getElement().getThemeList().clear();
        statusBadge.getElement().getThemeList().add("badge " + mapStatusToTheme(order.getStatus()));

        // Customer info
        customerNameLabel.setText(order.getCustomerName());
        customerPhoneLabel.setText(order.getCustomerPhone() != null ? order.getCustomerPhone() : "");
        customerPhoneLabel.addClassNames(LumoUtility.TextColor.SECONDARY);

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
        dueDateTimeLabel.addClassNames(LumoUtility.TextColor.SECONDARY);

        // Additional details
        additionalDetailsLabel.setText(order.getAdditionalDetails() != null ?
                order.getAdditionalDetails() : "None");
        if (order.getAdditionalDetails() == null || order.getAdditionalDetails().isBlank()) {
            additionalDetailsLabel.addClassNames(LumoUtility.TextColor.SECONDARY);
        }

        // Payment
        totalLabel.setText(CURRENCY_FORMAT.format(order.getTotal()));
        totalLabel.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);
        paidBadge.setText(order.isPaid() ? "Paid" : "Unpaid");
        paidBadge.getElement().getThemeList().clear();
        paidBadge.getElement().getThemeList().add("badge " + (order.isPaid() ? "success" : "error"));

        // History
        createdByLabel.setText("Created by: " + (order.getCreatedByName() != null ?
                order.getCreatedByName() : "Unknown"));
        createdByLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        updatedByLabel.setText("Updated by: " + (order.getUpdatedByName() != null ?
                order.getUpdatedByName() : "-"));
        updatedByLabel.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        // Items
        itemsGrid.setItems(order.getItems());

        // Action buttons
        updateActionButtons();
    }

    private void updateActionButtons() {
        actionButtons.removeAll();

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
    }

    private void openStatusChangeDialog() {
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
        try {
            orderService.updateStatus(order.getId(), newStatus);
            order.setStatus(newStatus);
            populateView();
            Notification.show("Status updated", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Failed to update status: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void markAsPaid() {
        try {
            orderService.markAsPaid(order.getId());
            order.setPaid(true);
            populateView();
            Notification.show("Order marked as paid", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Failed to mark as paid: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void confirmCancel() {
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
        try {
            orderService.updateStatus(order.getId(), OrderStatus.CANCELLED);
            order.setStatus(OrderStatus.CANCELLED);
            populateView();
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
