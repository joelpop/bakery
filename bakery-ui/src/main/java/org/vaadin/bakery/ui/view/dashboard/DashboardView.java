package org.vaadin.bakery.ui.view.dashboard;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import org.vaadin.bakery.service.DashboardService;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Dashboard view showing business analytics and KPIs.
 */
@Route("dashboard")
@PageTitle("Dashboard")
@Menu(order = 0, icon = LineAwesomeIconUrl.CHART_AREA_SOLID)
@PermitAll
public class DashboardView extends VerticalLayout {

    private final DashboardService dashboardService;

    // KPI Cards
    private final KpiCard remainingTodayCard;
    private final KpiCard unavailableCard;
    private final KpiCard newOrdersCard;
    private final KpiCard tomorrowCard;
    private final KpiCard monthTotalCard;
    private final KpiCard yearTotalCard;

    // Panels
    private final UpcomingOrdersPanel upcomingOrdersPanel;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    public DashboardView(DashboardService dashboardService) {
        this.dashboardService = dashboardService;

        addClassName("dashboard-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Header
        var header = createHeader();
        add(header);

        // Main content (scrollable)
        var content = new Div();
        content.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.LARGE,
                LumoUtility.Padding.MEDIUM
        );

        // KPI Cards row
        remainingTodayCard = new KpiCard("Remaining Today", VaadinIcon.CLOCK);
        unavailableCard = new KpiCard("Not Available", VaadinIcon.WARNING);
        newOrdersCard = new KpiCard("New Orders", VaadinIcon.INBOX);
        tomorrowCard = new KpiCard("Tomorrow", VaadinIcon.CALENDAR);
        monthTotalCard = new KpiCard("Month Total", VaadinIcon.CHART);
        yearTotalCard = new KpiCard("Year Total", VaadinIcon.TRENDING_UP);

        var kpiRow = createKpiRow();
        content.add(kpiRow);

        // Charts row (placeholder for now)
        var chartsRow = createChartsPlaceholder();
        content.add(chartsRow);

        // Bottom section: Upcoming orders + product breakdown
        upcomingOrdersPanel = new UpcomingOrdersPanel();
        var bottomRow = createBottomRow();
        content.add(bottomRow);

        var scroller = new Scroller(content);
        scroller.setSizeFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        add(scroller);
        setFlexGrow(1, scroller);

        // Load data
        refreshData();
    }

    private Div createHeader() {
        var header = new Div();
        header.addClassName("view-header");

        var title = new Span("Dashboard");
        title.addClassNames(
                LumoUtility.FontSize.XLARGE,
                LumoUtility.FontWeight.SEMIBOLD
        );

        header.add(title);
        return header;
    }

    private Div createKpiRow() {
        var row = new Div();
        row.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(180px, 1fr))")
                .set("gap", "var(--lumo-space-m)");

        row.add(remainingTodayCard, unavailableCard, newOrdersCard,
                tomorrowCard, monthTotalCard, yearTotalCard);
        return row;
    }

    private Div createChartsPlaceholder() {
        var row = new Div();
        row.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(300px, 1fr))")
                .set("gap", "var(--lumo-space-m)");

        // Pickups chart placeholder
        var pickupsChart = createChartPlaceholder("Pickups This Month", "Daily pickup counts");
        var yearlyChart = createChartPlaceholder("Pickups This Year", "Monthly pickup counts");

        row.add(pickupsChart, yearlyChart);
        return row;
    }

    private Div createChartPlaceholder(String title, String description) {
        var card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("padding", "var(--lumo-space-m)")
                .set("min-height", "200px");

        card.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN
        );

        var header = new H3(title);
        header.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.MEDIUM);

        var desc = new Span(description);
        desc.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);

        var placeholder = new Div();
        placeholder.addClassNames(
                LumoUtility.Flex.GROW,
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.JustifyContent.CENTER,
                LumoUtility.TextColor.TERTIARY
        );
        placeholder.setText("Chart will be added in future update");

        card.add(header, desc, placeholder);
        return card;
    }

    private Div createBottomRow() {
        var row = new Div();
        row.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(300px, 1fr))")
                .set("gap", "var(--lumo-space-m)");

        // Upcoming orders panel
        row.add(upcomingOrdersPanel);

        // Product breakdown placeholder
        var productBreakdown = createChartPlaceholder("Products This Month", "Product distribution");
        row.add(productBreakdown);

        return row;
    }

    private void refreshData() {
        // Remaining Today
        var remainingCount = dashboardService.getRemainingTodayCount();
        remainingTodayCard.setValue(remainingCount);
        dashboardService.getNextPickupTime().ifPresentOrElse(
                time -> remainingTodayCard.setSubtitle("Next: " + TIME_FORMATTER.format(time)),
                () -> remainingTodayCard.setSubtitle("No more pickups")
        );

        // Unavailable Products
        var unavailableCount = dashboardService.getUnavailableProductsCount();
        unavailableCard.setValue(unavailableCount);
        if (unavailableCount > 0) {
            unavailableCard.setSubtitle("Products unavailable");
        } else {
            unavailableCard.setSubtitle("All products available");
        }

        // New Orders
        var newCount = dashboardService.getNewOrdersCount();
        newOrdersCard.setValue(newCount);
        dashboardService.getLastNewOrderTime().ifPresentOrElse(
                time -> newOrdersCard.setSubtitle(formatTimeAgo(time)),
                () -> newOrdersCard.setSubtitle("No new orders")
        );

        // Tomorrow
        var tomorrowCount = dashboardService.getTomorrowCount();
        tomorrowCard.setValue(tomorrowCount);
        dashboardService.getFirstPickupTimeTomorrow().ifPresentOrElse(
                time -> tomorrowCard.setSubtitle("First: " + TIME_FORMATTER.format(time)),
                () -> tomorrowCard.setSubtitle("No orders")
        );

        // Month Total with deltas
        var monthTotal = dashboardService.getMonthTotal();
        monthTotalCard.setValue(monthTotal.value());
        monthTotalCard.clearDeltas();
        monthTotalCard.addDelta("vs prev month", monthTotal.previousPeriodDelta());
        monthTotalCard.addDelta("vs last year", monthTotal.samePeriodLastYearDelta());

        // Year Total with deltas
        var yearTotal = dashboardService.getYearTotal();
        yearTotalCard.setValue(yearTotal.value());
        yearTotalCard.clearDeltas();
        yearTotalCard.addDelta("vs prev year", yearTotal.previousPeriodDelta());
        yearTotalCard.addDelta("vs same period", yearTotal.samePeriodLastYearDelta());

        // Upcoming orders
        var upcomingOrders = dashboardService.getUpcomingOrders(10);
        upcomingOrdersPanel.setOrders(upcomingOrders);
    }

    private String formatTimeAgo(LocalDateTime time) {
        var now = LocalDateTime.now();
        var duration = Duration.between(time, now);

        if (duration.toMinutes() < 1) {
            return "Just now";
        } else if (duration.toMinutes() < 60) {
            return duration.toMinutes() + "m ago";
        } else if (duration.toHours() < 24) {
            return duration.toHours() + "h ago";
        } else {
            return duration.toDays() + "d ago";
        }
    }
}
