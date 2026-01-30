package org.vaadin.bakery.ui.view.about;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Version;
import com.vaadin.flow.server.WebBrowser;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.component.dependency.Uses;
import jakarta.annotation.security.PermitAll;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

/**
 * About view showing application and environment information.
 */
@Route("")
@PageTitle("About")
@Menu(order = 0, icon = LineAwesomeIconUrl.INFO_CIRCLE_SOLID)
@PermitAll
@Uses(LumoUtility.class)
public class AboutView extends Composite<VerticalLayout> implements HasSize, HasStyle {

    public AboutView() {
        var content = getContent();
        content.addClassNames(LumoUtility.Padding.LARGE);

        var title = new H2("Café Sunshine");
        title.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
        content.add(title);

        content.add(createInfoSection("Application", createApplicationInfo()));
        content.add(createInfoSection("Dependencies", createDependencyInfo()));
        content.add(createInfoSection("Runtime", createRuntimeInfo()));
        content.add(createInfoSection("Database", createDatabaseInfo()));
        content.add(createInfoSection("Browser", createBrowserInfo()));
    }

    private Div createInfoSection(String title, Div info) {
        var section = new Div();
        section.addClassNames(LumoUtility.Margin.Bottom.LARGE);

        var header = new H2(title);
        header.addClassNames(
                LumoUtility.FontSize.MEDIUM,
                LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.Margin.Bottom.SMALL
        );

        section.add(header, info);
        return section;
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
        info.add(createInfoRow("Available Processors", String.valueOf(runtime.availableProcessors())));
        info.add(createInfoRow("Max Memory", formatBytes(runtime.maxMemory())));
        return info;
    }

    private Div createDatabaseInfo() {
        var info = new Div();
        info.add(createInfoRow("Type", "H2"));
        info.add(createInfoRow("Mode", "In-Memory (Development)"));
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

    private Paragraph createInfoRow(String label, String value) {
        var row = new Paragraph();
        row.addClassNames(LumoUtility.Margin.Vertical.XSMALL);

        var labelSpan = new Span(label + ": ");
        labelSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD);

        var valueSpan = new Span(value);

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
