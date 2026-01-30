package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.OrderDashboard;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for dashboard data and KPI operations.
 */
public interface DashboardService {

    /**
     * Count of remaining orders for today (excluding completed/cancelled).
     */
    long getRemainingTodayCount();

    /**
     * Next pickup time for today.
     */
    Optional<LocalTime> getNextPickupTime();

    /**
     * Count of new orders (NEW status).
     */
    long getNewOrdersCount();

    /**
     * Timestamp of the most recent new order.
     */
    Optional<java.time.LocalDateTime> getLastNewOrderTime();

    /**
     * Count of orders due tomorrow.
     */
    long getTomorrowCount();

    /**
     * First pickup time tomorrow.
     */
    Optional<LocalTime> getFirstPickupTimeTomorrow();

    /**
     * Count of unavailable products.
     */
    long getUnavailableProductsCount();

    /**
     * Upcoming orders for display widget.
     */
    List<OrderDashboard> getUpcomingOrders(int limit);

    /**
     * Monthly pickup counts for current month (day -> count).
     */
    Map<Integer, Long> getMonthlyPickupData();

    /**
     * Yearly pickup counts for current year (month -> count).
     */
    Map<Integer, Long> getYearlyPickupData();

    /**
     * Product breakdown for current month (product name -> count).
     */
    Map<String, Long> getProductBreakdown();

    /**
     * Year-over-year sales comparison (year -> total).
     */
    Map<Integer, Long> getYearOverYearSales();

    /**
     * Month total with delta comparisons.
     */
    KpiWithDelta getMonthTotal();

    /**
     * Year total with delta comparisons.
     */
    KpiWithDelta getYearTotal();

    /**
     * KPI value with comparison deltas.
     */
    record KpiWithDelta(
            long value,
            long previousPeriodValue,
            long samePeriodLastYearValue,
            double previousPeriodDelta,
            double samePeriodLastYearDelta
    ) {}
}
