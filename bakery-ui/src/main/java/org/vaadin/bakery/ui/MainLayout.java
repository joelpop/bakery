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

        // Inject comprehensive CSS for styling
        attachEvent.getUI().getPage().executeJs(
                "if (!document.getElementById('main-layout-styles')) {" +
                "  const style = document.createElement('style');" +
                "  style.id = 'main-layout-styles';" +
                "  style.textContent = `" +
                "    /* Header styling with gradient */" +
                "    .main-layout vaadin-app-layout::part(navbar) {" +
                "      background: linear-gradient(135deg, #1565C0 0%, #1976D2 50%, #42A5F5 100%);" +
                "      box-shadow: 0 2px 4px rgba(0,0,0,0.1);" +
                "    }" +
                "    .main-layout .app-branding h1 { color: white; }" +
                "    .main-layout .app-branding vaadin-icon { filter: drop-shadow(0 1px 2px rgba(0,0,0,0.2)); }" +
                "    " +
                "    /* Navigation tabs styling */" +
                "    .desktop-navigation { display: flex; }" +
                "    .desktop-navigation vaadin-tab { color: rgba(255,255,255,0.85); }" +
                "    .desktop-navigation vaadin-tab[selected] { color: white; }" +
                "    .desktop-navigation vaadin-tab a { color: inherit !important; }" +
                "    .desktop-navigation vaadin-tab::before { background: white !important; }" +
                "    " +
                "    /* User menu in header */" +
                "    .main-layout vaadin-avatar { border: 2px solid rgba(255,255,255,0.5); }" +
                "    " +
                "    /* Content area styling */" +
                "    .main-layout [slot=''] { background: #f5f7fa; min-height: calc(100vh - 64px); }" +
                "    " +
                "    /* Card styling utility class */" +
                "    .card {" +
                "      background: white;" +
                "      border-radius: var(--lumo-border-radius-l);" +
                "      box-shadow: 0 1px 3px rgba(0,0,0,0.08), 0 1px 2px rgba(0,0,0,0.06);" +
                "      padding: var(--lumo-space-m);" +
                "    }" +
                "    .card-elevated {" +
                "      background: white;" +
                "      border-radius: var(--lumo-border-radius-l);" +
                "      box-shadow: 0 4px 6px rgba(0,0,0,0.07), 0 2px 4px rgba(0,0,0,0.06);" +
                "      padding: var(--lumo-space-m);" +
                "    }" +
                "    " +
                "    /* Status badge styling */" +
                "    .status-badge {" +
                "      display: inline-flex;" +
                "      align-items: center;" +
                "      padding: 2px 8px;" +
                "      border-radius: 12px;" +
                "      font-size: var(--lumo-font-size-xs);" +
                "      font-weight: 500;" +
                "      text-transform: uppercase;" +
                "      letter-spacing: 0.5px;" +
                "    }" +
                "    .status-new { background: #E3F2FD; color: #1565C0; }" +
                "    .status-verified { background: #E8F5E9; color: #2E7D32; }" +
                "    .status-ready { background: #E8F5E9; color: #2E7D32; }" +
                "    .status-problem { background: #FFEBEE; color: #C62828; }" +
                "    .status-not-ok { background: #FFEBEE; color: #C62828; }" +
                "    .status-cancelled { background: #ECEFF1; color: #546E7A; }" +
                "    .status-in-progress { background: #FFF3E0; color: #E65100; }" +
                "    .status-baked { background: #FFF8E1; color: #F57C00; }" +
                "    .status-packaged { background: #E0F7FA; color: #00838F; }" +
                "    .status-picked-up { background: #ECEFF1; color: #546E7A; }" +
                "    " +
                "    /* View header styling */" +
                "    .view-header {" +
                "      display: flex;" +
                "      align-items: center;" +
                "      justify-content: space-between;" +
                "      padding: var(--lumo-space-m) var(--lumo-space-l);" +
                "      background: white;" +
                "      border-bottom: 1px solid var(--lumo-contrast-10pct);" +
                "    }" +
                "    .view-header h1, .view-header h2 {" +
                "      margin: 0;" +
                "      font-size: var(--lumo-font-size-xl);" +
                "      font-weight: 600;" +
                "      color: var(--lumo-header-text-color);" +
                "    }" +
                "    " +
                "    /* Section headers */" +
                "    .section-header {" +
                "      font-size: var(--lumo-font-size-s);" +
                "      font-weight: 600;" +
                "      color: var(--lumo-secondary-text-color);" +
                "      text-transform: uppercase;" +
                "      letter-spacing: 0.5px;" +
                "      margin: var(--lumo-space-l) 0 var(--lumo-space-s) 0;" +
                "    }" +
                "    " +
                "    /* Grid/list item hover */" +
                "    .list-item:hover { background: var(--lumo-contrast-5pct); }" +
                "    " +
                "    /* Bottom navigation */" +
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
                "      vaadin-dialog-overlay[theme~='responsive-dialog'] {" +
                "        --_vaadin-dialog-content-width: 100vw !important;" +
                "        --_vaadin-dialog-content-height: 100vh !important;" +
                "      }" +
                "      vaadin-dialog-overlay[theme~='responsive-dialog'] [part='overlay'] {" +
                "        width: 100vw !important;" +
                "        height: 100vh !important;" +
                "        max-width: 100vw !important;" +
                "        max-height: 100vh !important;" +
                "        border-radius: 0;" +
                "      }" +
                "      vaadin-button { min-height: 44px; min-width: 44px; }" +
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
