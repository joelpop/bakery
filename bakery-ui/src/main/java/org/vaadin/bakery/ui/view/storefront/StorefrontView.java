package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.ElementEffect;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.ObjectProvider;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.OrderActivityService;
import org.vaadin.bakery.service.OrderService;
import org.vaadin.bakery.service.UserLocationService;
import org.vaadin.bakery.ui.MainLayout;
import org.vaadin.bakery.ui.component.ChangeTracker;
import org.vaadin.bakery.ui.component.ViewHeader;
import org.vaadin.bakery.ui.event.DataChangeSignals;
import org.vaadin.bakery.uimodel.data.OrderList;
import org.vaadin.bakery.uimodel.type.UserRole;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Storefront view showing orders as cards grouped by date.
 */
@Route(StorefrontView.ROUTE)
@RouteAlias("")
@PageTitle("Storefront")
@Menu(order = 1, icon = LineAwesomeIconUrl.STORE_ALT_SOLID)
@RolesAllowed({UserRole.ROLE_ADMIN, UserRole.ROLE_BAKER, UserRole.ROLE_BARISTA})
public class StorefrontView extends Composite<VerticalLayout> implements HasSize, HasStyle {

    /** Route path for this view. */
    public static final String ROUTE = "orders";

    private final transient OrderService orderService;
    private final transient OrderActivityService orderActivityService;
    private final transient LocationService locationService;
    private final transient UserLocationService userLocationService;
    private final transient ObjectProvider<EditOrderDialog> editOrderDialogProvider;
    private final Div ordersContainer;
    private final FilterBar filterBar;
    private final TextField searchField;

    // Signal incremented to trigger a same-session data refresh (e.g., after local filter change or save)
    private final ValueSignal<Integer> refreshTriggerSignal;

    // Tracks version changes between refreshes to identify new and modified orders for highlight animation
    private final ChangeTracker<OrderList> changeTracker;

    private Registration locationChangeRegistration;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d");

    /** Creates the storefront view with filter bar and order cards grouped by date. */
    public StorefrontView(OrderService orderService, OrderActivityService orderActivityService,
                          LocationService locationService, UserLocationService userLocationService,
                          ObjectProvider<EditOrderDialog> editOrderDialogProvider) {
        this.orderService = orderService;
        this.orderActivityService = orderActivityService;
        this.locationService = locationService;
        this.userLocationService = userLocationService;
        this.editOrderDialogProvider = editOrderDialogProvider;

        // Component initializations
        searchField = new TextField();
        searchField.setPlaceholder("Filter orders");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setWidth("300px");

        var header = new ViewHeader("Storefront")
                .withFilters(searchField)
                .withAction("New order", this::openNewOrderDialog);

        filterBar = new FilterBar(locationService.listActive(), userLocationService);

        ordersContainer = new Div();
        ordersContainer.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.LARGE,
                LumoUtility.Padding.MEDIUM
        );

        var scroller = new Scroller(ordersContainer);
        scroller.setSizeFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);

        // Signal definitions
        refreshTriggerSignal = new ValueSignal<>(0);
        changeTracker = new ChangeTracker<>();

        // Signal bindings - trigger a local refresh when user changes search text or filter criteria
        searchField.addValueChangeListener(_ -> triggerRefresh());
        filterBar.addFilterChangedListener(_ -> triggerRefresh());

        // Reactive effect: re-fetches and rebuilds the orders display whenever order data changes
        // in any session (via shared orderVersion/messageVersion signals) or locally (via refreshTriggerSignal)
        ElementEffect.effect(this.getElement(), () -> {
            DataChangeSignals.orderVersion().get();
            DataChangeSignals.messageVersion().get();
            refreshTriggerSignal.get();
            rebuildOrdersDisplay();
        });

        // Content layout
        var content = getContent();
        content.addClassName("storefront-view");
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.add(header, filterBar, scroller);
        content.setFlexGrow(1, scroller);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Listen for location changes from MainLayout
        getUI().ifPresent(ui -> {
            ui.getChildren()
                    .filter(MainLayout.class::isInstance)
                    .map(MainLayout.class::cast)
                    .findFirst()
                    .ifPresent(mainLayout -> {
                        locationChangeRegistration = mainLayout.addCurrentLocationChangedListener(_ -> {
                            // Refresh if "Current Location" is selected in the filter
                            if (filterBar.isCurrentLocationSelected()) {
                                triggerRefresh();
                            }
                        });
                    });
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (locationChangeRegistration != null) {
            locationChangeRegistration.remove();
            locationChangeRegistration = null;
        }
    }

    private void openNewOrderDialog() {
        var dialog = editOrderDialogProvider.getObject();
        dialog.addSaveListener(_ -> triggerRefresh());
        dialog.open();
    }

    private void triggerRefresh() {
        refreshTriggerSignal.update(v -> v + 1);
    }

    /**
     * Refresh the orders display. Called by MainLayout after order creation.
     */
    public void refresh() {
        triggerRefresh();
    }

    private void rebuildOrdersDisplay() {
        ordersContainer.removeAll();

        var fromDate = filterBar.getFromDate();
        var toDate = filterBar.getToDate();

        if (fromDate == null) {
            fromDate = LocalDate.now();
        }
        if (toDate == null) {
            toDate = fromDate.plusDays(7);
        }

        var orders = orderService.listByDateRange(fromDate, toDate);

        // Fetch unread message status for all orders in the list
        var orderIds = orders.stream().map(OrderList::getId).toList();
        var unreadOrderIds = orderIds.isEmpty()
                ? Collections.<Long>emptySet()
                : orderActivityService.findOrderIdsWithUnreadMessages(orderIds);

        // Compare current data versions against previous snapshot to identify new/changed orders
        changeTracker.detectChanges(orders);

        // Apply search filter (customer name)
        var searchTerm = searchField.getValue();
        if (!searchTerm.isBlank()) {
            var lowerSearch = searchTerm.toLowerCase();
            orders = orders.stream()
                    .filter(o -> o.getCustomerName().toLowerCase().contains(lowerSearch))
                    .toList();
        }

        // Apply status filter
        var selectedStatuses = filterBar.getSelectedStatuses();
        if (!selectedStatuses.isEmpty()) {
            orders = orders.stream()
                    .filter(o -> selectedStatuses.contains(o.getStatus()))
                    .toList();
        }

        // Apply location filter
        var selectedLocation = filterBar.getSelectedLocation();
        if (selectedLocation != null) {
            orders = orders.stream()
                    .filter(o -> selectedLocation.getName().equals(o.getLocationName()))
                    .toList();
        }

        // Partition orders: "Needs Attention" (rejected items) vs normal
        var needsAttention = orders.stream()
                .filter(OrderList::isHasRejectedItems)
                .toList();
        var normalOrders = orders.stream()
                .filter(o -> !o.isHasRejectedItems())
                .toList();

        // Group normal orders by date
        Map<LocalDate, List<OrderList>> ordersByDate = normalOrders.stream()
                .collect(Collectors.groupingBy(
                        OrderList::getDueDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        if (needsAttention.isEmpty() && ordersByDate.isEmpty()) {
            var emptyMessage = new Div();
            emptyMessage.addClassNames(
                    LumoUtility.Display.FLEX,
                    LumoUtility.AlignItems.CENTER,
                    LumoUtility.JustifyContent.CENTER,
                    LumoUtility.TextColor.SECONDARY
            );
            emptyMessage.getStyle().set("min-height", "200px");
            emptyMessage.add(new Span("No orders found for the selected criteria"));
            ordersContainer.add(emptyMessage);
            return;
        }

        var finalUnreadOrderIds = unreadOrderIds;

        // "Needs Attention" section at top for orders with rejected items
        if (!needsAttention.isEmpty()) {
            var section = createNeedsAttentionSection(needsAttention, finalUnreadOrderIds);
            ordersContainer.add(section);
        }

        // Create sections for each date
        ordersByDate.forEach((date, dateOrders) -> {
            var section = createDateSection(date, dateOrders, finalUnreadOrderIds);
            ordersContainer.add(section);
        });
    }

    private Div createNeedsAttentionSection(List<OrderList> orders, Set<Long> unreadOrderIds) {
        var section = new Div();
        section.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.BorderRadius.MEDIUM
        );
        section.getStyle().set("background-color", "var(--lumo-error-color-10pct)");

        var header = new H3("Needs Attention");
        header.addClassNames(
                LumoUtility.Margin.NONE,
                LumoUtility.FontSize.MEDIUM,
                LumoUtility.TextColor.ERROR
        );
        section.add(header);

        var cardsContainer = new Div();
        cardsContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-m)");

        for (var order : orders) {
            var card = new OrderCard(order, unreadOrderIds.contains(order.getId()));
            if (changeTracker.isNew(order.getId())) {
                card.addClassName("card-new");
            } else if (changeTracker.isHighlighted(order.getId())) {
                card.addClassName("card-highlight");
            }
            card.addOrderClickListener(e -> openOrderDetail(e.getOrder().getId()));
            cardsContainer.add(card);
        }

        section.add(cardsContainer);
        return section;
    }

    private Div createDateSection(LocalDate date, List<OrderList> orders, Set<Long> unreadOrderIds) {
        var section = new Div();
        section.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.MEDIUM
        );

        // Date header
        var dateLabel = formatDateLabel(date);
        var header = new H3(dateLabel);
        header.addClassNames(
                LumoUtility.Margin.NONE,
                LumoUtility.FontSize.MEDIUM,
                LumoUtility.TextColor.SECONDARY
        );
        section.add(header);

        // Cards grid
        var cardsContainer = new Div();
        cardsContainer.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-m)");

        for (var order : orders) {
            var card = new OrderCard(order, unreadOrderIds.contains(order.getId()));
            // Apply CSS highlight classes to visually indicate new or modified orders (animated fade)
            if (changeTracker.isNew(order.getId())) {
                card.addClassName("card-new");
            } else if (changeTracker.isHighlighted(order.getId())) {
                card.addClassName("card-highlight");
            }
            card.addOrderClickListener(e -> openOrderDetail(e.getOrder().getId()));
            cardsContainer.add(card);
        }

        section.add(cardsContainer);
        return section;
    }

    private String formatDateLabel(LocalDate date) {
        var today = LocalDate.now();
        if (date.equals(today)) {
            return "Today - " + DATE_FORMATTER.format(date);
        } else if (date.equals(today.plusDays(1))) {
            return "Tomorrow - " + DATE_FORMATTER.format(date);
        } else {
            return DATE_FORMATTER.format(date);
        }
    }

    private void openOrderDetail(Long orderId) {
        getUI().ifPresent(ui -> ui.navigate(OrderDetailView.class,
                new RouteParameters("orderId", orderId.toString())));
    }
}
