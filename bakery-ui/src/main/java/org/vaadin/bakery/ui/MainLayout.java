package org.vaadin.bakery.ui;

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
import org.vaadin.bakery.service.CurrentUserService;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.OrderService;
import org.vaadin.bakery.service.ProductService;
import org.vaadin.bakery.ui.view.storefront.EditOrderDialog;
import org.vaadin.bakery.ui.view.storefront.StorefrontView;
import org.vaadin.bakery.uimodel.data.UserDetail;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Main application layout with horizontal navigation.
 * Features:
 * - App branding (Café Sunshine)
 * - Desktop: Top horizontal navigation tabs
 * - Mobile: Bottom tab bar with touch optimization
 * - Global "+ New order" action button
 * - User menu with avatar
 * - Role-based navigation item visibility
 */
@Layout
@PermitAll
public class MainLayout extends AppLayout implements RouterLayout, AfterNavigationObserver {

    private final CurrentUserService currentUserService;
    private final AccessAnnotationChecker accessChecker;
    private final OrderService orderService;
    private final LocationService locationService;
    private final ProductService productService;

    private Tabs navigationTabs;
    private Tabs bottomNavigationTabs;
    private final Map<String, Tab> routeToTab = new HashMap<>();
    private final Map<String, Tab> routeToBottomTab = new HashMap<>();

    public MainLayout(CurrentUserService currentUserService, AccessAnnotationChecker accessChecker,
                      OrderService orderService, LocationService locationService,
                      ProductService productService) {
        this.currentUserService = currentUserService;
        this.accessChecker = accessChecker;
        this.orderService = orderService;
        this.locationService = locationService;
        this.productService = productService;

        addClassName("main-layout");
        setPrimarySection(Section.NAVBAR);

        addHeaderContent();
        addBottomNavigation();
    }

    private void addHeaderContent() {
        var header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);
        header.addClassName("main-header");

        // App branding
        var appName = createAppBranding();

        // Navigation group: tabs + new order button (grouped together)
        navigationTabs = createNavigationTabs();
        var newOrderButton = createNewOrderButton();

        var navGroup = new HorizontalLayout(navigationTabs, newOrderButton);
        navGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        navGroup.setSpacing(false);
        navGroup.addClassNames(LumoUtility.Gap.SMALL, "nav-group");

        // User menu
        var userMenu = createUserMenu();

        header.add(appName, navGroup, userMenu);

        addToNavbar(header);
    }

    private Component createAppBranding() {
        var sunIcon = new Icon(VaadinIcon.SUN_O);
        sunIcon.getStyle().set("color", "#F5A623"); // Warm golden yellow

        var appName = new H1("Café Sunshine");
        appName.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.NONE,
                LumoUtility.Whitespace.NOWRAP
        );

        var brandingContainer = new HorizontalLayout(sunIcon, appName);
        brandingContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        brandingContainer.setSpacing(false);
        brandingContainer.addClassNames(LumoUtility.Gap.SMALL, "app-branding");

        return brandingContainer;
    }

    private Tabs createNavigationTabs() {
        var tabs = new Tabs();
        tabs.addThemeVariants(TabsVariant.LUMO_MINIMAL);
        tabs.addClassNames("desktop-navigation");
        tabs.getStyle().set("--lumo-primary-text-color", "var(--lumo-primary-color)");

        MenuConfiguration.getMenuEntries().stream()
                .filter(this::isAccessible)
                .forEach(entry -> {
                    var tab = createTab(entry);
                    tabs.add(tab);
                    routeToTab.put(normalizePathForLookup(entry.path()), tab);
                });

        return tabs;
    }

    private Component createNewOrderButton() {
        // Desktop button with text
        var button = new Button("New order", new Icon(VaadinIcon.PLUS));
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        button.addClassName("new-order-button");
        button.addClickListener(e -> openNewOrderDialog());
        return button;
    }

    private String normalizePathForLookup(String path) {
        // Normalize path for consistent map lookups (empty string for root)
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }
        // Strip leading slash if present for consistent matching
        return path.startsWith("/") ? path.substring(1) : path;
    }

    private String buildHref(String path) {
        // Build proper href, avoiding double slashes
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private Tab createTab(MenuEntry entry) {
        var link = new Anchor(buildHref(entry.path()));
        link.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.TextColor.BODY
        );
        link.getStyle().set("text-decoration", "none");

        if (entry.icon() != null) {
            var icon = new Icon(entry.icon());
            icon.addClassNames(LumoUtility.Margin.End.SMALL);
            link.add(icon);
        }
        link.add(new Span(entry.title()));

        var tab = new Tab(link);
        tab.addClassNames(LumoUtility.Padding.Horizontal.SMALL);
        return tab;
    }

    private void addBottomNavigation() {
        var bottomNav = new HorizontalLayout();
        bottomNav.setWidthFull();
        bottomNav.setAlignItems(FlexComponent.Alignment.CENTER);
        bottomNav.addClassName("bottom-navigation");

        bottomNavigationTabs = new Tabs();
        bottomNavigationTabs.addThemeVariants(TabsVariant.LUMO_EQUAL_WIDTH_TABS);

        MenuConfiguration.getMenuEntries().stream()
                .filter(this::isAccessible)
                .forEach(entry -> {
                    var tab = createBottomTab(entry);
                    bottomNavigationTabs.add(tab);
                    routeToBottomTab.put(normalizePathForLookup(entry.path()), tab);
                });

        // Mobile new order button (icon only)
        var mobileNewOrderButton = new Button(new Icon(VaadinIcon.PLUS));
        mobileNewOrderButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        mobileNewOrderButton.addClassName("new-order-button-mobile");
        mobileNewOrderButton.addClickListener(e -> openNewOrderDialog());

        bottomNav.add(bottomNavigationTabs, mobileNewOrderButton);
        bottomNav.setFlexGrow(1, bottomNavigationTabs);

        // Add to touch-optimized navbar slot for mobile
        addToNavbar(true, bottomNav);
    }

    private Tab createBottomTab(MenuEntry entry) {
        var link = new Anchor(buildHref(entry.path()));
        link.addClassNames(
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.FontSize.XSMALL,
                LumoUtility.TextColor.BODY
        );
        link.getStyle().set("text-decoration", "none");

        if (entry.icon() != null) {
            var icon = new Icon(entry.icon());
            link.add(icon);
        }
        link.add(new Span(entry.title()));

        return new Tab(link);
    }

    private Component createUserMenu() {
        var menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);

        Optional<UserDetail> currentUser = currentUserService.getCurrentUser();

        var avatar = new Avatar();
        currentUser.ifPresent(user -> {
            avatar.setName(user.getFirstName() + " " + user.getLastName());
            if (user.getPhoto() != null && user.getPhoto().length > 0) {
                // Photo would be set as stream resource in real implementation
            }
        });

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
        subMenu.addItem("Preferences", e ->
                UI.getCurrent().navigate("preferences"));

        // Logout
        subMenu.addItem(createLogoutLink());

        return menuBar;
    }

    private Component createLogoutLink() {
        var logoutIcon = new Icon(VaadinIcon.SIGN_OUT);
        logoutIcon.addClassNames(LumoUtility.Margin.End.SMALL);

        var logoutLink = new Anchor("/logout", "Log out");
        logoutLink.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER
        );
        logoutLink.getElement().insertChild(0, logoutIcon.getElement());

        return logoutLink;
    }

    private boolean isAccessible(MenuEntry entry) {
        // Use the access checker to determine if the current user can access the view
        try {
            var viewClass = Class.forName(entry.menuClass().getName());
            return accessChecker.hasAccess(viewClass);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // ========== New Order Dialog ==========

    private void openNewOrderDialog() {
        var dialog = new EditOrderDialog(orderService, locationService);
        dialog.setAvailableProducts(productService.listAvailable());

        dialog.addSaveClickListener(event -> {
            refreshCurrentViewIfNeeded(event.isNewCustomerCreated());
        });

        dialog.open();
    }

    private void refreshCurrentViewIfNeeded(boolean newCustomerCreated) {
        var content = getContent();

        if (content instanceof StorefrontView view) {
            view.refresh();
        }
        // TODO: Add DashboardView refresh when implemented
        // else if (content instanceof DashboardView view) {
        //     view.refresh();
        // }
        // TODO: Add CustomerView refresh when implemented
        // else if (newCustomerCreated && content instanceof CustomerView view) {
        //     view.refresh();
        // }
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        String path = normalizePathForLookup(event.getLocation().getPath());

        // Update desktop tabs selection
        Tab selectedTab = routeToTab.get(path);
        if (selectedTab != null) {
            navigationTabs.setSelectedTab(selectedTab);
        } else {
            navigationTabs.setSelectedTab(null);
        }

        // Update mobile bottom tabs selection
        Tab selectedBottomTab = routeToBottomTab.get(path);
        if (selectedBottomTab != null) {
            bottomNavigationTabs.setSelectedTab(selectedBottomTab);
        } else {
            bottomNavigationTabs.setSelectedTab(null);
        }
    }
}
