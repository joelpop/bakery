package org.vaadin.bakery.ui.view.dashboard;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import org.vaadin.bakery.service.DashboardService;
import org.vaadin.bakery.ui.component.ViewHeader;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

        // Component initializations
        addClassName("dashboard-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        var header = new ViewHeader("Dashboard");

        remainingTodayCard = new KpiCard("Remaining Today", VaadinIcon.CLOCK);
        unavailableCard = new KpiCard("Not Available", VaadinIcon.WARNING);
        newOrdersCard = new KpiCard("New Orders", VaadinIcon.INBOX);
        tomorrowCard = new KpiCard("Tomorrow", VaadinIcon.CALENDAR);
        monthTotalCard = new KpiCard("Month Total", VaadinIcon.CHART);
        yearTotalCard = new KpiCard("Year Total", VaadinIcon.TRENDING_UP);

        var kpiRow = new Div();
        kpiRow.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(180px, 1fr))")
                .set("gap", "var(--lumo-space-m)");
        kpiRow.add(remainingTodayCard, unavailableCard, newOrdersCard,
                tomorrowCard, monthTotalCard, yearTotalCard);

        var chartsRow = new Div();
        chartsRow.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(300px, 1fr))")
                .set("gap", "var(--lumo-space-m)");
        chartsRow.add(
                createChartPlaceholder("Pickups This Month", "Daily pickup counts"),
                createChartPlaceholder("Pickups This Year", "Monthly pickup counts")
        );

        upcomingOrdersPanel = new UpcomingOrdersPanel();

        var bottomRow = new Div();
        bottomRow.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(300px, 1fr))")
                .set("gap", "var(--lumo-space-m)");
        bottomRow.add(
                upcomingOrdersPanel,
                createChartPlaceholder("Products This Month", "Product distribution")
        );

        var content = new Div();
        content.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.LARGE,
                LumoUtility.Padding.MEDIUM
        );
        content.add(kpiRow, chartsRow, bottomRow);

        var scroller = new Scroller(content);
        scroller.setSizeFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);

        // Layout assembly
        add(header, scroller);
        setFlexGrow(1, scroller);

        // Data loading
        refreshData();
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
