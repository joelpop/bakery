package org.vaadin.bakery.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
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
import org.vaadin.bakery.uimodel.data.UserDetail;

import com.vaadin.flow.component.AttachEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Main application layout with horizontal navigation.
 * Features:
 * - App branding (Café Sunshine)
 * - Desktop: Top horizontal navigation tabs
 * - Mobile: Bottom tab bar with touch optimization
 * - User menu with avatar
 * - Role-based navigation item visibility
 */
@Layout
@PermitAll
public class MainLayout extends AppLayout implements RouterLayout, AfterNavigationObserver {

    private final CurrentUserService currentUserService;
    private final AccessAnnotationChecker accessChecker;

    private Tabs navigationTabs;
    private Tabs bottomNavigationTabs;
    private final Map<String, Tab> routeToTab = new HashMap<>();
    private final Map<String, Tab> routeToBottomTab = new HashMap<>();

    public MainLayout(CurrentUserService currentUserService, AccessAnnotationChecker accessChecker) {
        this.currentUserService = currentUserService;
        this.accessChecker = accessChecker;

        addClassName("main-layout");
        setPrimarySection(Section.NAVBAR);

        addHeaderContent();
        addBottomNavigation();
    }

    private void addHeaderContent() {
        var header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);

        // App branding
        var appName = createAppBranding();

        // Navigation tabs
        navigationTabs = createNavigationTabs();

        // User menu (will be pushed to the right by expand on tabs)
        var userMenu = createUserMenu();

        header.add(appName, navigationTabs, userMenu);
        header.expand(navigationTabs);

        addToNavbar(header);
    }

    private Component createAppBranding() {
        var sunIcon = new Icon(VaadinIcon.SUN_O);
        sunIcon.addClassNames(LumoUtility.TextColor.PRIMARY);

        var appName = new H1("Café Sunshine");
        appName.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.NONE,
                LumoUtility.Whitespace.NOWRAP
        );

        var brandingContainer = new HorizontalLayout(sunIcon, appName);
        brandingContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        brandingContainer.setSpacing(false);
        brandingContainer.addClassNames(LumoUtility.Gap.SMALL);

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

    private String normalizePathForLookup(String path) {
        // Normalize path for consistent map lookups (empty string for root)
        if (path == null || path.equals("/")) {
            return "";
        }
        return path;
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
        bottomNavigationTabs = new Tabs();
        bottomNavigationTabs.addThemeVariants(TabsVariant.LUMO_EQUAL_WIDTH_TABS);
        bottomNavigationTabs.addClassNames("bottom-navigation");

        MenuConfiguration.getMenuEntries().stream()
                .filter(this::isAccessible)
                .forEach(entry -> {
                    var tab = createBottomTab(entry);
                    bottomNavigationTabs.add(tab);
                    routeToBottomTab.put(normalizePathForLookup(entry.path()), tab);
                });

        // Add to touch-optimized navbar slot for mobile
        addToNavbar(true, bottomNavigationTabs);
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

        var tab = new Tab(link);
        return tab;
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Inject responsive CSS inline to avoid cross-module frontend resource issues
        attachEvent.getUI().getPage().executeJs(
                "if (!document.getElementById('main-layout-styles')) {" +
                "  const style = document.createElement('style');" +
                "  style.id = 'main-layout-styles';" +
                "  style.textContent = `" +
                "    .desktop-navigation { display: flex; }" +
                "    .bottom-navigation { display: none; }" +
                "    @media (max-width: 768px) {" +
                "      .desktop-navigation { display: none; }" +
                "      .bottom-navigation {" +
                "        display: flex;" +
                "        position: fixed;" +
                "        bottom: 0;" +
                "        left: 0;" +
                "        right: 0;" +
                "        background: var(--lumo-base-color);" +
                "        border-top: 1px solid var(--lumo-contrast-10pct);" +
                "        z-index: 100;" +
                "        padding: var(--lumo-space-xs) 0;" +
                "      }" +
                "      .main-layout [slot=''] { padding-bottom: 60px; }" +
                "    }" +
                "    .main-layout vaadin-tab a { text-decoration: none; color: inherit; display: flex; align-items: center; }" +
                "  `;" +
                "  document.head.appendChild(style);" +
                "}"
        );
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
