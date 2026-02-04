package org.vaadin.bakery.ui;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

import java.time.ZoneId;

import org.vaadin.bakery.service.CurrentUserService;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.OrderService;
import org.vaadin.bakery.service.ProductService;
import org.vaadin.bakery.service.UserTimezoneService;
import org.vaadin.bakery.ui.view.storefront.EditOrderDialog;
import org.vaadin.bakery.ui.view.storefront.StorefrontView;
import org.vaadin.bakery.uimodel.data.UserDetail;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Main application layout with responsive navigation.
 * Features:
 * - App branding (Café Sunshine)
 * - Desktop: Top navigation bar with icons + text
 * - Mobile: Bottom navigation bar with icons only
 * - Global "+ New order" action button
 * - User menu with avatar
 * - Role-based navigation item visibility
 */
@Layout
@PermitAll
public class MainLayout extends AppLayout implements RouterLayout, AfterNavigationObserver {

    private final transient CurrentUserService currentUserService;
    private final transient AccessAnnotationChecker accessChecker;
    private final transient OrderService orderService;
    private final transient LocationService locationService;
    private final transient ProductService productService;
    private final transient UserTimezoneService userTimezoneService;

    private Tabs navigationTabs;
    private final Map<String, Tab> routeToTab = new HashMap<>();

    public MainLayout(CurrentUserService currentUserService, AccessAnnotationChecker accessChecker,
                      OrderService orderService, LocationService locationService,
                      ProductService productService, UserTimezoneService userTimezoneService) {
        this.currentUserService = currentUserService;
        this.accessChecker = accessChecker;
        this.orderService = orderService;
        this.locationService = locationService;
        this.productService = productService;
        this.userTimezoneService = userTimezoneService;

        addClassName("main-layout");
        setPrimarySection(Section.NAVBAR);

        addNavbarContent();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Retrieve browser timezone on first attach if not already set
        if (!userTimezoneService.isBrowserTimezoneSet()) {
            attachEvent.getUI().getPage().retrieveExtendedClientDetails(details -> {
                var timezoneId = details.getTimeZoneId();
                if (timezoneId != null && !timezoneId.isEmpty()) {
                    userTimezoneService.setBrowserTimezone(ZoneId.of(timezoneId));
                }
            });
        }
    }

    private void addNavbarContent() {
        var navbar = new HorizontalLayout();
        navbar.setWidthFull();
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        navbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        navbar.addClassNames(
                LumoUtility.Padding.Horizontal.MEDIUM,
                "main-navbar"
        );

        // App branding (hidden on mobile)
        var branding = createAppBranding();

        // Navigation group: tabs + new order button + mobile menu
        navigationTabs = createNavigationTabs();
        var newOrderButton = createNewOrderButton();
        var mobileMenu = createMobileMenu();

        var navGroup = new HorizontalLayout(navigationTabs, newOrderButton, mobileMenu);
        navGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        navGroup.addClassNames(LumoUtility.Gap.MEDIUM, "nav-group");
        navGroup.setSpacing(false);

        // User menu (desktop/tablet)
        var userMenu = createUserMenu();

        navbar.add(branding, navGroup, userMenu);

        addToNavbar(navbar);
    }

    private Component createAppBranding() {
        var sunIcon = new Icon(VaadinIcon.SUN_O);
        sunIcon.getStyle().set("color", "#F5A623");

        var appName = new H1("Café Sunshine");
        appName.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.NONE,
                LumoUtility.Whitespace.NOWRAP
        );

        var container = new HorizontalLayout(sunIcon, appName);
        container.setAlignItems(FlexComponent.Alignment.CENTER);
        container.setSpacing(false);
        container.addClassNames(LumoUtility.Gap.SMALL, "app-branding");

        return container;
    }

    private Tabs createNavigationTabs() {
        var tabs = new Tabs();
        tabs.addThemeVariants(TabsVariant.LUMO_MINIMAL);
        tabs.addClassName("main-nav-tabs");

        MenuConfiguration.getMenuEntries().stream()
                .filter(this::isAccessible)
                .forEach(entry -> {
                    var tab = createNavTab(entry);
                    tabs.add(tab);
                    routeToTab.put(normalizePathForLookup(entry.path()), tab);
                });

        return tabs;
    }

    private Tab createNavTab(MenuEntry entry) {
        var link = new Anchor(buildHref(entry.path()));
        link.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.SMALL
        );
        link.getStyle().set("text-decoration", "none");

        // Icon (always visible)
        var icon = new Icon(getIconForRoute(entry.path()));
        icon.addClassName("nav-icon");
        link.add(icon);

        // Text label (hidden on mobile via CSS)
        var label = new Span(entry.title());
        label.addClassName("nav-label");
        link.add(label);

        var tab = new Tab(link);

        // Mark admin nav tabs to hide on mobile (they move to hamburger menu)
        if (isAdminNavRoute(entry.path())) {
            tab.addClassName("admin-nav-tab");
        }

        return tab;
    }

    private boolean isAdminNavRoute(String path) {
        var normalized = normalizePathForLookup(path);
        return normalized.equals("products") ||
               normalized.equals("locations") ||
               normalized.equals("users");
    }

    private VaadinIcon getIconForRoute(String path) {
        var normalizedPath = normalizePathForLookup(path);
        return switch (normalizedPath) {
            case "dashboard" -> VaadinIcon.DASHBOARD;
            case "", "orders" -> VaadinIcon.CART;
            case "products" -> VaadinIcon.PACKAGE;
            case "locations" -> VaadinIcon.MAP_MARKER;
            case "users" -> VaadinIcon.USERS;
            default -> VaadinIcon.CIRCLE;
        };
    }

    private Component createNewOrderButton() {
        // Create button with icon, text added via suffix component
        var icon = new Icon(VaadinIcon.PLUS);
        var text = new Span("New order");
        text.addClassName("button-text");

        var button = new Button(icon);
        button.setSuffixComponent(text);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        button.addClassName("new-order-button");
        button.addClickListener(_ -> openNewOrderDialog());

        return button;
    }

    private Component createUserMenu() {
        var menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        menuBar.addClassName("user-menu");

        Optional<UserDetail> currentUser = currentUserService.getCurrentUser();

        var avatar = new Avatar();
        currentUser.ifPresent(user ->
            avatar.setName(user.getFirstName() + " " + user.getLastName())
        );

        var menuItem = menuBar.addItem(avatar);
        var subMenu = menuItem.getSubMenu();

        // User info section
        currentUser.ifPresent(user -> {
            var userInfo = new Div();
            userInfo.addClassNames(
                    LumoUtility.Padding.MEDIUM,
                    LumoUtility.Border.BOTTOM
            );

            var userName = new Div(user.getFirstName() + " " + user.getLastName());
            userName.addClassNames(LumoUtility.FontWeight.SEMIBOLD);

            var userEmail = new Div(user.getEmail());
            userEmail.addClassNames(
                    LumoUtility.FontSize.SMALL,
                    LumoUtility.TextColor.SECONDARY
            );

            var userRole = new Div(user.getRole().getDisplayName());
            userRole.addClassNames(
                    LumoUtility.FontSize.XSMALL,
                    LumoUtility.TextColor.TERTIARY
            );

            userInfo.add(userName, userEmail, userRole);
            subMenu.addItem(userInfo);
        });

        // Preferences link
        subMenu.addItem("Preferences", _ ->
                UI.getCurrent().navigate("preferences"));

        // About link (Admin only)
        if (currentUserService.isAdmin()) {
            subMenu.addItem("About", _ ->
                    UI.getCurrent().navigate("about"));
        }

        // Logout
        var logoutIcon = new Icon(VaadinIcon.SIGN_OUT);
        logoutIcon.addClassNames(LumoUtility.Margin.End.SMALL);

        var logoutLink = new Anchor("/logout", "Log out");
        logoutLink.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER
        );
        logoutLink.getElement().insertChild(0, logoutIcon.getElement());

        subMenu.addItem(logoutLink);

        return menuBar;
    }

    private Component createMobileMenu() {
        var menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        menuBar.addClassName("mobile-menu");

        var menuIcon = new Icon(VaadinIcon.MENU);
        var menuItem = menuBar.addItem(menuIcon);
        var subMenu = menuItem.getSubMenu();

        // Navigation section - admin routes that are hidden in mobile nav
        if (accessChecker.hasAccess(getViewClass("products"))) {
            subMenu.addItem(createMenuItemContent(VaadinIcon.PACKAGE, "Products"),
                    _ -> UI.getCurrent().navigate("products"));
        }

        if (accessChecker.hasAccess(getViewClass("locations"))) {
            subMenu.addItem(createMenuItemContent(VaadinIcon.MAP_MARKER, "Locations"),
                    _ -> UI.getCurrent().navigate("locations"));
        }

        if (accessChecker.hasAccess(getViewClass("users"))) {
            subMenu.addItem(createMenuItemContent(VaadinIcon.USERS, "Users"),
                    _ -> UI.getCurrent().navigate("users"));
        }

        // User info section (same as desktop user menu)
        Optional<UserDetail> currentUser = currentUserService.getCurrentUser();
        currentUser.ifPresent(user -> {
            var userInfo = new Div();
            userInfo.addClassNames(
                    LumoUtility.Padding.MEDIUM,
                    LumoUtility.Border.TOP,
                    LumoUtility.Border.BOTTOM
            );

            var userName = new Div(user.getFirstName() + " " + user.getLastName());
            userName.addClassNames(LumoUtility.FontWeight.SEMIBOLD);

            var userEmail = new Div(user.getEmail());
            userEmail.addClassNames(
                    LumoUtility.FontSize.SMALL,
                    LumoUtility.TextColor.SECONDARY
            );

            var userRole = new Div(user.getRole().getDisplayName());
            userRole.addClassNames(
                    LumoUtility.FontSize.XSMALL,
                    LumoUtility.TextColor.TERTIARY
            );

            userInfo.add(userName, userEmail, userRole);
            subMenu.addItem(userInfo);
        });

        // Preferences link
        subMenu.addItem("Preferences", _ -> UI.getCurrent().navigate("preferences"));

        // About link (Admin only)
        if (currentUserService.isAdmin()) {
            subMenu.addItem("About", _ -> UI.getCurrent().navigate("about"));
        }

        // Logout
        var logoutIcon = new Icon(VaadinIcon.SIGN_OUT);
        logoutIcon.addClassNames(LumoUtility.Margin.End.SMALL);

        var logoutLink = new Anchor("/logout", "Log out");
        logoutLink.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER
        );
        logoutLink.getElement().insertChild(0, logoutIcon.getElement());

        subMenu.addItem(logoutLink);

        return menuBar;
    }

    private Class<?> getViewClass(String route) {
        return switch (route) {
            case "products" -> org.vaadin.bakery.ui.view.products.ProductsView.class;
            case "locations" -> org.vaadin.bakery.ui.view.locations.LocationsView.class;
            case "users" -> org.vaadin.bakery.ui.view.users.UsersView.class;
            default -> null;
        };
    }

    private Component createMenuItemContent(VaadinIcon iconType, String text) {
        var icon = new Icon(iconType);
        icon.addClassNames(LumoUtility.Margin.End.SMALL);
        var label = new Span(text);
        var container = new HorizontalLayout(icon, label);
        container.setAlignItems(FlexComponent.Alignment.CENTER);
        container.setSpacing(false);
        return container;
    }

    private String normalizePathForLookup(String path) {
        if (path.isEmpty() || path.equals("/")) {
            return "";
        }
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private String buildHref(String path) {
        if (path.isEmpty() || path.equals("/")) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private boolean isAccessible(MenuEntry entry) {
        try {
            var viewClass = Class.forName(entry.menuClass().getName());
            return accessChecker.hasAccess(viewClass);
        } catch (ClassNotFoundException _) {
            return false;
        }
    }

    private void openNewOrderDialog() {
        var dialog = new EditOrderDialog(orderService, locationService);
        dialog.setAvailableProducts(productService.listAvailable());
        dialog.addSaveClickListener(_ -> refreshCurrentViewIfNeeded());
        dialog.open();
    }

    private void refreshCurrentViewIfNeeded() {
        if (getContent() instanceof StorefrontView view) {
            view.refresh();
        }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        var path = normalizePathForLookup(event.getLocation().getPath());
        navigationTabs.setSelectedTab(routeToTab.get(path));

        // Toggle class for storefront-specific styling (hides duplicate new order button on desktop)
        if (path.isEmpty() || path.equals("orders")) {
            addClassName("on-storefront");
        } else {
            removeClassName("on-storefront");
        }
    }
}
