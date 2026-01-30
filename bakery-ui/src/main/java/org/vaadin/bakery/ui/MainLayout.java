package org.vaadin.bakery.ui;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

/**
 * Main application layout with navigation drawer.
 */
@Layout
@PermitAll
public class MainLayout extends AppLayout implements RouterLayout, AfterNavigationObserver {

    private H1 viewTitle;

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();
    }

    private void addHeaderContent() {
        var toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H1();
        viewTitle.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.NONE);

        var header = new Header(toggle, viewTitle);
        header.addClassNames(
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Display.FLEX,
                LumoUtility.Padding.End.MEDIUM,
                LumoUtility.Width.FULL
        );

        addToNavbar(true, header);
    }

    private void addDrawerContent() {
        var appName = new Span("Bakery");
        appName.addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.Display.FLEX,
                LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.Height.XLARGE, LumoUtility.Padding.Horizontal.MEDIUM);

        addToDrawer(appName, new Scroller(createNavigation()));
    }

    private SideNav createNavigation() {
        var nav = new SideNav();

        MenuConfiguration.getMenuEntries().forEach(entry -> nav.addItem(createSideNavItem(entry)));

        return nav;
    }

    private SideNavItem createSideNavItem(MenuEntry menuEntry) {
        var item = new SideNavItem(menuEntry.title(), menuEntry.path());
        item.setMatchNested(true);
        if (menuEntry.icon() != null) {
            item.setPrefixComponent(new Icon(menuEntry.icon()));
        }
        return item;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        return MenuConfiguration.getPageHeader(getContent()).orElse("");
    }
}
