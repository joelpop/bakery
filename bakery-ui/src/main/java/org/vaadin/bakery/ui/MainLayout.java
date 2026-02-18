package org.vaadin.bakery.ui;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.SvgIcon;
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
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

import java.time.ZoneId;

import org.springframework.beans.factory.ObjectProvider;
import org.vaadin.bakery.service.CurrentUserService;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.UserLocationService;
import org.vaadin.bakery.service.UserTimezoneService;
import org.vaadin.bakery.ui.event.MessageBroadcaster;
import org.vaadin.bakery.ui.view.about.AboutView;
import org.vaadin.bakery.ui.view.preferences.PreferencesView;
import org.vaadin.bakery.ui.view.storefront.EditOrderDialog;
import org.vaadin.bakery.ui.view.storefront.StorefrontView;
import org.vaadin.bakery.uimodel.data.LocationSummary;

import java.util.HashMap;
import java.util.Map;

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
    private final transient LocationService locationService;
    private final transient UserTimezoneService userTimezoneService;
    private final transient UserLocationService userLocationService;
    private final transient AuthenticationContext authenticationContext;
    private final transient ObjectProvider<EditOrderDialog> editOrderDialogProvider;

    private final Tabs navigationTabs;
    private final Map<String, Tab> routeToTab;
    private final ComboBox<LocationSummary> locationSelector;

    /** Creates the main application layout with navigation, location selector, and user menu. */
    public MainLayout(CurrentUserService currentUserService, AccessAnnotationChecker accessChecker,
                      LocationService locationService, UserTimezoneService userTimezoneService,
                      UserLocationService userLocationService, AuthenticationContext authenticationContext,
                      ObjectProvider<EditOrderDialog> editOrderDialogProvider) {
        this.currentUserService = currentUserService;
        this.accessChecker = accessChecker;
        this.locationService = locationService;
        this.userTimezoneService = userTimezoneService;
        this.userLocationService = userLocationService;
        this.authenticationContext = authenticationContext;
        this.editOrderDialogProvider = editOrderDialogProvider;

        addClassName("main-layout");
        setPrimarySection(Section.NAVBAR);

        // Build navbar content
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
        routeToTab = new HashMap<>();
        navigationTabs = createNavigationTabs();
        var newOrderButton = createNewOrderButton();
        var mobileMenu = createMobileMenu();

        var navGroup = new HorizontalLayout(navigationTabs, newOrderButton, mobileMenu);
        navGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        navGroup.addClassNames(LumoUtility.Gap.MEDIUM, "nav-group");
        navGroup.setSpacing(false);

        // Location selector + User menu (desktop/tablet)
        locationSelector = createLocationSelector();
        var userMenu = createUserMenu();

        var rightGroup = new HorizontalLayout(locationSelector, userMenu);
        rightGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        rightGroup.addClassNames(LumoUtility.Gap.SMALL);
        rightGroup.setSpacing(false);

        navbar.add(branding, navGroup, rightGroup);

        addToNavbar(navbar);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Retrieve browser timezone on first attach if not already set
        if (!userTimezoneService.isBrowserTimezoneSet()) {
            var details = attachEvent.getUI().getPage().getExtendedClientDetails();
            var timezoneId = details.getTimeZoneId();
            if (timezoneId != null && !timezoneId.isEmpty()) {
                userTimezoneService.setBrowserTimezone(ZoneId.of(timezoneId));
            }
        }

        // Initialize current location from user's primary location
        if (!userLocationService.isCurrentLocationSet()) {
            userLocationService.initializeFromUserPrimaryLocation();
        }

        // Update location selector to show current location
        updateLocationSelectorValue();

        // Register for message broadcast notifications
        currentUserService.getCurrentUser().ifPresent(user -> {
            var currentLocation = userLocationService.getCurrentLocation();
            var sessionInfo = new MessageBroadcaster.SessionInfo(
                    user.getId(), user.getRole(), currentLocation.getId());
            MessageBroadcaster.register(attachEvent.getUI(), sessionInfo);
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        MessageBroadcaster.unregister(detachEvent.getUI());
    }

    private void updateLocationSelectorValue() {
        var currentLocation = userLocationService.getCurrentLocation();
        locationSelector.getListDataView().getItems()
                .filter(loc -> loc.getId().equals(currentLocation.getId()))
                .findFirst()
                .ifPresent(locationSelector::setValue);
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

        // Icon (always visible) — sourced from the view's @Menu annotation
        var icon = new SvgIcon(entry.icon());
        icon.addClassName("nav-icon");
        link.add(icon);

        // Text label (hidden on mobile via CSS)
        var label = new Span(entry.title());
        label.addClassName("nav-label");
        link.add(label);

        var tab = new Tab(link);

        // Mark role-restricted nav tabs to hide on mobile (they move to hamburger menu)
        if (isRoleRestrictedRoute(entry)) {
            tab.addClassName("admin-nav-tab");
        }

        return tab;
    }

    private boolean isRoleRestrictedRoute(MenuEntry entry) {
        return !entry.menuClass().isAnnotationPresent(PermitAll.class);
    }

    private ComboBox<LocationSummary> createLocationSelector() {
        var comboBox = new ComboBox<LocationSummary>();
        comboBox.setItems(locationService.listActive());
        comboBox.setItemLabelGenerator(LocationSummary::getName);
        comboBox.setPlaceholder("Select location");
        comboBox.setWidth("160px");
        comboBox.addClassName("location-selector");
        comboBox.getElement().setAttribute("theme", "small");

        // Set initial value from service
        var currentLocation = userLocationService.getCurrentLocation();
        comboBox.getListDataView().getItems()
                .filter(loc -> loc.getId().equals(currentLocation.getId()))
                .findFirst()
                .ifPresent(comboBox::setValue);

        comboBox.addValueChangeListener(this::onLocationSelectorValueChanged);

        return comboBox;
    }

    private void onLocationSelectorValueChanged(
            ComponentValueChangeEvent<ComboBox<LocationSummary>, LocationSummary> event) {
        if (event.isFromClient()) {
            userLocationService.setCurrentLocation(event.getValue());
            getUI().ifPresent(ui ->
                    MessageBroadcaster.updateLocation(ui, event.getValue().getId()));
            fireEvent(new CurrentLocationChangedEvent(this, event.getValue()));
        }
    }

    /**
     * Register a listener for current location changes.
     */
    public Registration addCurrentLocationChangedListener(
            ComponentEventListener<CurrentLocationChangedEvent> listener) {
        return addListener(CurrentLocationChangedEvent.class, listener);
    }

    /**
     * Event fired when the user changes their current working location.
     */
    public static class CurrentLocationChangedEvent extends ComponentEvent<MainLayout> {
        private final LocationSummary location;

        public CurrentLocationChangedEvent(MainLayout source, LocationSummary location) {
            super(source, false);
            this.location = location;
        }

        public LocationSummary getLocation() {
            return location;
        }
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

        var avatar = new Avatar();
        currentUserService.getCurrentUser().ifPresent(user ->
            avatar.setName(user.getFirstName() + " " + user.getLastName())
        );

        var menuItem = menuBar.addItem(avatar);
        var subMenu = menuItem.getSubMenu();

        addUserInfoToMenu(subMenu, LumoUtility.Border.BOTTOM);
        addCommonMenuActions(subMenu);

        return menuBar;
    }

    private Component createMobileMenu() {
        var menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        menuBar.addClassName("mobile-menu");

        var menuIcon = new Icon(VaadinIcon.MENU);
        var menuItem = menuBar.addItem(menuIcon);
        var subMenu = menuItem.getSubMenu();

        // Navigation section - role-restricted routes that are hidden in mobile nav
        MenuConfiguration.getMenuEntries().stream()
                .filter(this::isAccessible)
                .filter(this::isRoleRestrictedRoute)
                .forEach(entry ->
                    subMenu.addItem(createMenuItemContent(entry.icon(), entry.title()),
                            _ -> UI.getCurrent().navigate(entry.menuClass()))
                );

        // User info + common actions (shared with desktop user menu)
        addUserInfoToMenu(subMenu, LumoUtility.Border.TOP, LumoUtility.Border.BOTTOM);
        addCommonMenuActions(subMenu);

        return menuBar;
    }

    private void addUserInfoToMenu(SubMenu subMenu, String... borderClassNames) {
        currentUserService.getCurrentUser().ifPresent(user -> {
            var userInfo = new Div();
            userInfo.addClassNames(LumoUtility.Padding.MEDIUM);
            userInfo.addClassNames(borderClassNames);

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
    }

    private void addCommonMenuActions(SubMenu subMenu) {
        subMenu.addItem("Preferences", _ ->
                UI.getCurrent().navigate(PreferencesView.class));

        if (currentUserService.isAdmin()) {
            subMenu.addItem("About", _ ->
                    UI.getCurrent().navigate(AboutView.class));
        }

        subMenu.addItem("Log out", _ -> authenticationContext.logout());
    }

    private Component createMenuItemContent(String iconUrl, String text) {
        var icon = new SvgIcon(iconUrl);
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
        return accessChecker.hasAccess(entry.menuClass());
    }

    private void openNewOrderDialog() {
        var dialog = editOrderDialogProvider.getObject();
        dialog.addSaveListener(_ -> refreshCurrentViewIfNeeded());
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
        if (path.isEmpty() || path.equals(StorefrontView.ROUTE)) {
            addClassName("on-storefront");
        } else {
            removeClassName("on-storefront");
        }
    }
}
