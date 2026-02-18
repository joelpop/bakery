package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.signals.impl.Effect;
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
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.bakery.service.CustomerService;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.OrderActivityService;
import org.vaadin.bakery.service.OrderService;
import org.vaadin.bakery.service.ProductService;
import org.vaadin.bakery.service.StaleDataException;
import org.vaadin.bakery.service.UserLocationService;
import org.vaadin.bakery.ui.event.DataChangeSignals;
import org.vaadin.bakery.uimodel.data.OrderDetail;
import org.vaadin.bakery.uimodel.data.OrderItemDetail;
import org.vaadin.bakery.uimodel.type.OrderStatus;
import org.vaadin.bakery.uimodel.type.UserRole;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;

/**
 * View for displaying and managing a single order.
 */
@Route(StorefrontView.ROUTE + "/:orderId")
@PageTitle("Order Details")
@RolesAllowed({UserRole.ROLE_ADMIN, UserRole.ROLE_BARISTA, UserRole.ROLE_BAKER})
public class OrderDetailView extends VerticalLayout implements BeforeEnterObserver {

    private final transient OrderService orderService;
    private final transient OrderActivityService orderActivityService;
    private final transient ProductService productService;
    private final transient LocationService locationService;
    private final transient CustomerService customerService;
    private final transient UserLocationService userLocationService;

    // Signal holding the currently displayed order; all display fields react to changes in this signal
    private final transient ValueSignal<OrderDetail> orderSignal;

    // Cross-session refresh state: used to avoid redundant UI updates when the version hasn't changed
    private Long currentOrderId;
    private Integer currentOrderVersion;

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
    private final Div timelineContainer;

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    /** Creates the order detail view with order information, items grid, action buttons, and activity timeline. */
    public OrderDetailView(OrderService orderService, OrderActivityService orderActivityService,
                           ProductService productService,
                           LocationService locationService, CustomerService customerService,
                           UserLocationService userLocationService) {
        this.orderService = orderService;
        this.orderActivityService = orderActivityService;
        this.productService = productService;
        this.locationService = locationService;
        this.customerService = customerService;
        this.userLocationService = userLocationService;

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

        // Reactive effect: updates all display fields (status, customer, location, items, payment, history)
        // whenever the orderSignal value changes
        Effect.effect(this, () -> {
            var order = orderSignal.get();
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

        // Reactive effect: rebuilds the action buttons (edit, change status, mark paid, cancel)
        // based on the current order status and payment state
        Effect.effect(this, () -> {
            var order = orderSignal.get();
            actionButtons.removeAll();
            if (order == null) return;

            // Edit button (enabled for non-terminal orders)
            var editButton = createActionButton("Edit Order", VaadinIcon.EDIT);
            editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            editButton.setEnabled(!order.getStatus().isTerminal());
            editButton.addClickListener(_ -> openEditDialog());
            actionButtons.add(editButton);

            // Status change button
            if (!order.getStatus().isTerminal()) {
                var changeStatusButton = createActionButton("Change Status", VaadinIcon.ARROWS_LONG_H);
                changeStatusButton.addClickListener(_ -> openStatusChangeDialog());
                actionButtons.add(changeStatusButton);
            }

            // Mark as paid button
            if (!order.isPaid() && !order.getStatus().isTerminal()) {
                var markPaidButton = createActionButton("Mark as Paid", VaadinIcon.MONEY);
                markPaidButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
                markPaidButton.addClickListener(_ -> markAsPaid());
                actionButtons.add(markPaidButton);
            }

            // Cancel button (only for pre-production orders)
            if (order.getStatus().isPreProduction()) {
                var cancelButton = createActionButton("Cancel Order", VaadinIcon.CLOSE);
                cancelButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
                cancelButton.addClickListener(_ -> confirmCancel());
                actionButtons.add(cancelButton);
            }
        });

        // Reactive effect: re-fetches the order from the database whenever any session modifies
        // order data (via shared orderVersion signal), keeping this view in sync across sessions
        Effect.effect(this, () -> {
            DataChangeSignals.orderVersion().get();
            refreshOrder();
        });

        // Header
        var backButton = new Button(new Icon(VaadinIcon.ARROW_LEFT));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        backButton.addClickListener(_ -> navigateBack());

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

        // Activity timeline — populated with order-specific content in beforeEnter()
        timelineContainer = new Div();
        timelineContainer.setWidth("350px");
        timelineContainer.setMinHeight("300px");

        // Layout assembly
        var content = new HorizontalLayout();
        content.setSizeFull();
        content.setSpacing(true);
        content.add(orderInfoSection, itemsSection, timelineContainer);

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
            var order = optOrder.get();
            currentOrderId = order.getId();
            currentOrderVersion = order.getVersion();
            orderSignal.set(order);

            // Set up the activity timeline
            timelineContainer.removeAll();
            var timeline = new OrderActivityTimeline(orderActivityService, orderId);
            timeline.setSizeFull();
            timelineContainer.add(timeline);
        } catch (NumberFormatException _) {
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

    private void openEditDialog() {
        var order = orderSignal.get();
        if (order == null) return;

        var editDialog = new EditOrderDialog(orderService, locationService, customerService, userLocationService);
        editDialog.setAvailableProducts(productService.listAvailable());
        editDialog.editOrder(order);
        editDialog.addSaveListener(_ -> refreshOrder());
        editDialog.open();
    }

    private void openStatusChangeDialog() {
        var order = orderSignal.get();
        if (order == null) return;

        var dialog = new Dialog();
        dialog.setHeaderTitle("Change Order Status");
        dialog.setWidth("400px");

        var statusCombo = new ComboBox<OrderStatus>("New Status");
        statusCombo.setWidthFull();

        // Filter available statuses: current + non-terminal + cancelled
        var availableStatuses = Arrays.stream(OrderStatus.values())
                .filter(s -> s == order.getStatus() || !s.isTerminal() || s == OrderStatus.CANCELLED)
                .toList();
        statusCombo.setItems(availableStatuses);
        statusCombo.setItemLabelGenerator(OrderStatus::getDisplayName);
        statusCombo.setValue(order.getStatus());

        dialog.add(statusCombo);

        var cancelButton = new Button("Cancel", _ -> dialog.close());
        var confirmButton = new Button("Update", _ -> {
            if (statusCombo.getValue() != null) {
                updateStatus(statusCombo.getValue());
                dialog.close();
            }
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelButton, confirmButton);
        dialog.open();
    }

    /** Re-fetches the order from the database and updates the signal if the version has changed. */
    private void refreshOrder() {
        if (currentOrderId == null) return;
        orderService.get(currentOrderId).ifPresentOrElse(
                order -> {
                    if (!order.getVersion().equals(currentOrderVersion)) {
                        currentOrderVersion = order.getVersion();
                        orderSignal.set(order);
                    }
                },
                () -> {
                    Notification.show("Order has been deleted", 5000, Notification.Position.BOTTOM_START)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    navigateBack();
                }
        );
    }

    private void updateStatus(OrderStatus newStatus) {
        var order = orderSignal.get();
        if (order == null) return;

        try {
            orderService.updateStatus(order.getId(), newStatus, order.getVersion());
            Notification.show("Status updated", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (StaleDataException _) {
            // Optimistic lock failed: the order was modified by another session since this view loaded
            Notification.show("Order was modified by another user. View refreshed.",
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
        } catch (Exception e) {
            Notification.show("Failed to update status: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        refreshOrder();
    }

    private void markAsPaid() {
        var order = orderSignal.get();
        if (order == null) return;

        try {
            orderService.markAsPaid(order.getId(), order.getVersion());
            Notification.show("Order marked as paid", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (StaleDataException _) {
            // Optimistic lock failed: the order was modified by another session since this view loaded
            Notification.show("Order was modified by another user. View refreshed.",
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
        } catch (Exception e) {
            Notification.show("Failed to mark as paid: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        refreshOrder();
    }

    private void confirmCancel() {
        var order = orderSignal.get();
        if (order == null) return;

        var dialog = new Dialog();
        dialog.setHeaderTitle("Cancel Order");
        dialog.add(new Span("Are you sure you want to cancel order #" + order.getId() + "?"));

        var cancelButton = new Button("No, keep it", _ -> dialog.close());
        var confirmButton = new Button("Yes, cancel order", _ -> {
            dialog.close();
            cancelOrder();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelButton, confirmButton);
        dialog.open();
    }

    private void cancelOrder() {
        var order = orderSignal.get();
        if (order == null) return;

        try {
            orderService.updateStatus(order.getId(), OrderStatus.CANCELLED, order.getVersion());
            Notification.show("Order cancelled", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (StaleDataException _) {
            // Optimistic lock failed: the order was modified by another session since this view loaded
            Notification.show("Order was modified by another user. View refreshed.",
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
        } catch (Exception e) {
            Notification.show("Failed to cancel order: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
        refreshOrder();
    }

    private static Button createActionButton(String text, VaadinIcon icon) {
        var textSpan = new Span(text);
        textSpan.addClassName("button-text");
        var button = new Button(new Icon(icon));
        button.setSuffixComponent(textSpan);
        button.addClassName("order-detail-button");
        return button;
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
