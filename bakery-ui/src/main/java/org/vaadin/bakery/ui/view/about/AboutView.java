package org.vaadin.bakery.ui.view.about;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Version;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import org.vaadin.bakery.ui.component.ViewHeader;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

/**
 * About view showing application and environment information.
 */
@Route("")
@PageTitle("About")
@Menu(order = 0, icon = LineAwesomeIconUrl.INFO_CIRCLE_SOLID)
@PermitAll
public class AboutView extends Composite<VerticalLayout> implements HasSize, HasStyle {

    public AboutView() {
        var content = getContent();
        content.setPadding(false);
        content.setSpacing(false);

        // Header
        var header = new ViewHeader("About");
        content.add(header);

        // Main content area
        var main = new Div();
        main.addClassNames(LumoUtility.Padding.LARGE);
        main.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(280px, 1fr))")
                .set("gap", "var(--lumo-space-l)")
                .set("max-width", "1200px");

        main.add(createInfoCard("Application", VaadinIcon.INFO_CIRCLE, createApplicationInfo()));
        main.add(createInfoCard("Dependencies", VaadinIcon.PACKAGE, createDependencyInfo()));
        main.add(createInfoCard("Runtime", VaadinIcon.COG, createRuntimeInfo()));
        main.add(createInfoCard("Database", VaadinIcon.DATABASE, createDatabaseInfo()));
        main.add(createInfoCard("Browser", VaadinIcon.GLOBE, createBrowserInfo()));

        content.add(main);
    }

    private Div createInfoCard(String title, VaadinIcon iconType, Div info) {
        var card = new Div();
        card.addClassName("card");

        // Card header with icon
        var icon = new Icon(iconType);
        icon.getStyle()
                .set("color", "var(--lumo-primary-color)")
                .set("width", "20px")
                .set("height", "20px");

        var header = new Span(title);
        header.addClassNames(
                LumoUtility.FontSize.MEDIUM,
                LumoUtility.FontWeight.SEMIBOLD
        );

        var headerRow = new HorizontalLayout(icon, header);
        headerRow.setAlignItems(FlexComponent.Alignment.CENTER);
        headerRow.addClassNames(LumoUtility.Gap.SMALL, LumoUtility.Margin.Bottom.MEDIUM);

        card.add(headerRow, info);
        return card;
    }

    private Div createApplicationInfo() {
        var info = new Div();
        info.add(createInfoRow("Name", "Café Sunshine"));
        info.add(createInfoRow("Version", "1.0.0-SNAPSHOT"));
        return info;
    }

    private Div createDependencyInfo() {
        var info = new Div();
        info.add(createInfoRow("Vaadin", Version.getFullVersion()));
        info.add(createInfoRow("Spring Boot", getSpringBootVersion()));
        info.add(createInfoRow("Java", Runtime.version().toString()));
        return info;
    }

    private Div createRuntimeInfo() {
        var info = new Div();
        var runtime = Runtime.getRuntime();
        info.add(createInfoRow("JVM", System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version")));
        info.add(createInfoRow("OS", System.getProperty("os.name") + " " + System.getProperty("os.version")));
        info.add(createInfoRow("Processors", String.valueOf(runtime.availableProcessors())));
        info.add(createInfoRow("Max Memory", formatBytes(runtime.maxMemory())));
        return info;
    }

    private Div createDatabaseInfo() {
        var info = new Div();
        info.add(createInfoRow("Type", "H2"));
        info.add(createInfoRow("Mode", "In-Memory"));
        info.add(createInfoRow("DDL Auto", "create-drop"));
        return info;
    }

    private Div createBrowserInfo() {
        var info = new Div();
        var browser = UI.getCurrent().getSession().getBrowser();

        info.add(createInfoRow("Browser", getBrowserName(browser)));
        info.add(createInfoRow("Platform", getPlatformName(browser)));

        return info;
    }

    private String getPlatformName(WebBrowser browser) {
        if (browser.isWindows()) return "Windows";
        if (browser.isMacOSX()) return "macOS";
        if (browser.isLinux()) return "Linux";
        if (browser.isAndroid()) return "Android";
        if (browser.isIPhone()) return "iOS";
        return "Unknown";
    }

    private Div createInfoRow(String label, String value) {
        var row = new Div();
        row.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.Padding.Vertical.XSMALL
        );
        row.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        var labelSpan = new Span(label);
        labelSpan.addClassNames(LumoUtility.TextColor.SECONDARY);

        var valueSpan = new Span(value);
        valueSpan.addClassNames(LumoUtility.FontWeight.MEDIUM);

        row.add(labelSpan, valueSpan);
        return row;
    }

    private String getSpringBootVersion() {
        var springBootVersion = org.springframework.boot.SpringBootVersion.getVersion();
        return springBootVersion != null ? springBootVersion : "Unknown";
    }

    private String getBrowserName(WebBrowser browser) {
        if (browser.isChrome()) return "Chrome";
        if (browser.isFirefox()) return "Firefox";
        if (browser.isSafari()) return "Safari";
        if (browser.isEdge()) return "Edge";
        if (browser.isOpera()) return "Opera";
        return "Unknown";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        var exp = (int) (Math.log(bytes) / Math.log(1024));
        var pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
