package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
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
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.OrderService;
import org.vaadin.bakery.service.ProductService;
import org.vaadin.bakery.uimodel.data.OrderList;
import org.vaadin.bakery.uimodel.type.OrderStatus;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import com.vaadin.flow.router.RouteParameters;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Storefront view showing orders as cards grouped by date.
 */
@Route("storefront")
@PageTitle("Storefront")
@Menu(order = 1, icon = LineAwesomeIconUrl.STORE_ALT_SOLID)
@RolesAllowed({"ADMIN", "BARISTA"})
public class StorefrontView extends VerticalLayout {

    private final OrderService orderService;
    private final LocationService locationService;
    private final ProductService productService;
    private final Div ordersContainer;
    private FilterBar filterBar;
    private TextField searchField;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMMM d");

    public StorefrontView(OrderService orderService, LocationService locationService,
                          ProductService productService) {
        this.orderService = orderService;
        this.locationService = locationService;
        this.productService = productService;

        addClassName("storefront-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Toolbar with search and new order button
        var toolbar = createToolbar();
        add(toolbar);

        // Filter bar (expandable filters)
        filterBar = new FilterBar(locationService.listActive());
        filterBar.addFilterChangedListener(e -> refreshOrders());
        add(filterBar);

        // Orders container (scrollable)
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
        add(scroller);
        setFlexGrow(1, scroller);

        refreshOrders();
    }

    private Div createToolbar() {
        var toolbar = new Div();
        toolbar.addClassName("view-toolbar");

        // Left side: search field
        var leftSection = new Div();
        leftSection.addClassName("view-toolbar-left");

        searchField = new TextField();
        searchField.setPlaceholder("Filter");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshOrders());
        searchField.setWidth("300px");

        leftSection.add(searchField);

        // Right side: new order button
        var rightSection = new Div();
        rightSection.addClassName("view-toolbar-right");

        var newOrderButton = new Button("New order", new Icon(VaadinIcon.PLUS));
        newOrderButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newOrderButton.addClickListener(e -> openNewOrderDialog());

        rightSection.add(newOrderButton);

        toolbar.add(leftSection, rightSection);
        return toolbar;
    }

    private void refreshOrders() {
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

        // Apply search filter (customer name)
        var searchTerm = searchField.getValue();
        if (searchTerm != null && !searchTerm.isBlank()) {
            var lowerSearch = searchTerm.toLowerCase();
            orders = orders.stream()
                    .filter(o -> o.getCustomerName().toLowerCase().contains(lowerSearch))
                    .toList();
        }

        // Apply status filter
        var selectedStatuses = filterBar.getSelectedStatuses();
        if (selectedStatuses != null && !selectedStatuses.isEmpty()) {
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

        // Group by date
        Map<LocalDate, List<OrderList>> ordersByDate = orders.stream()
                .collect(Collectors.groupingBy(
                        OrderList::getDueDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        if (ordersByDate.isEmpty()) {
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

        // Create sections for each date
        ordersByDate.forEach((date, dateOrders) -> {
            var section = createDateSection(date, dateOrders);
            ordersContainer.add(section);
        });
    }

    private Div createDateSection(LocalDate date, List<OrderList> orders) {
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
            var card = new OrderCard(order);
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

    private void openNewOrderDialog() {
        var dialog = new NewOrderDialog(
                orderService,
                locationService,
                () -> refreshOrders()
        );
        dialog.setAvailableProducts(productService.listAvailable());
        dialog.open();
    }

    private void openOrderDetail(Long orderId) {
        getUI().ifPresent(ui -> ui.navigate(OrderDetailView.class,
                new RouteParameters("orderId", orderId.toString())));
    }
}
