package org.vaadin.bakery.jpaservice;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.vaadin.bakery.jpamodel.code.OrderStatusCode;
import org.vaadin.bakery.jpaclient.repository.OrderRepository;
import org.vaadin.bakery.jpaclient.repository.ProductRepository;
import org.vaadin.bakery.jpaservice.mapper.OrderMapper;
import org.vaadin.bakery.service.DashboardService;
import org.vaadin.bakery.uimodel.data.OrderDashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * JPA implementation of the dashboard service.
 */
@Service
@Transactional(readOnly = true)
public class JpaDashboardService implements DashboardService {

    private static final List<OrderStatusCode> TERMINAL_STATUSES = List.of(
            OrderStatusCode.PICKED_UP,
            OrderStatusCode.CANCELLED
    );

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    public JpaDashboardService(OrderRepository orderRepository, ProductRepository productRepository,
                               OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
    }

    @Override
    public long getRemainingTodayCount() {
        return orderRepository.countByDueDateAndStatusNotIn(LocalDate.now(), TERMINAL_STATUSES);
    }

    @Override
    public Optional<LocalTime> getNextPickupTime() {
        var projections = orderRepository.findNextPickupTimeByDate(LocalDate.now(), TERMINAL_STATUSES);
        return projections.stream()
                .filter(p -> p.getDueTime().isAfter(LocalTime.now()))
                .map(p -> p.getDueTime())
                .findFirst();
    }

    @Override
    public long getNewOrdersCount() {
        return orderRepository.countByStatus(OrderStatusCode.NEW);
    }

    @Override
    public Optional<LocalDateTime> getLastNewOrderTime() {
        var newOrders = orderRepository.findByStatus(OrderStatusCode.NEW);
        return newOrders.stream()
                .map(o -> o.getCreatedAt())
                .max(LocalDateTime::compareTo);
    }

    @Override
    public long getTomorrowCount() {
        return orderRepository.countByDueDateAndStatusNotIn(LocalDate.now().plusDays(1), TERMINAL_STATUSES);
    }

    @Override
    public Optional<LocalTime> getFirstPickupTimeTomorrow() {
        var projections = orderRepository.findNextPickupTimeByDate(LocalDate.now().plusDays(1), TERMINAL_STATUSES);
        return projections.stream()
                .map(p -> p.getDueTime())
                .findFirst();
    }

    @Override
    public long getUnavailableProductsCount() {
        return productRepository.countByAvailableFalse();
    }

    @Override
    public List<OrderDashboard> getUpcomingOrders(int limit) {
        var orders = orderRepository.findDashboardOrdersByDate(LocalDate.now(), TERMINAL_STATUSES);
        return orderMapper.toDashboardList(orders).stream()
                .limit(limit)
                .toList();
    }

    @Override
    public Map<Integer, Long> getMonthlyPickupData() {
        var now = LocalDate.now();
        var result = new HashMap<Integer, Long>();

        for (int day = 1; day <= now.lengthOfMonth(); day++) {
            var date = now.withDayOfMonth(day);
            var count = orderRepository.countByDueDateBetweenAndStatus(date, date, OrderStatusCode.PICKED_UP);
            result.put(day, count);
        }

        return result;
    }

    @Override
    public Map<Integer, Long> getYearlyPickupData() {
        var now = LocalDate.now();
        var result = new HashMap<Integer, Long>();

        for (int month = 1; month <= 12; month++) {
            var count = orderRepository.countByYearAndMonthAndStatus(now.getYear(), month, OrderStatusCode.PICKED_UP);
            result.put(month, count);
        }

        return result;
    }

    @Override
    public Map<String, Long> getProductBreakdown() {
        // Simplified implementation - would need a custom query for production
        return new HashMap<>();
    }

    @Override
    public Map<Integer, Long> getYearOverYearSales() {
        var now = LocalDate.now();
        var result = new HashMap<Integer, Long>();

        for (int year = now.getYear() - 2; year <= now.getYear(); year++) {
            var count = orderRepository.countByYearAndStatus(year, OrderStatusCode.PICKED_UP);
            result.put(year, count);
        }

        return result;
    }

    @Override
    public KpiWithDelta getMonthTotal() {
        var now = LocalDate.now();
        var currentMonth = orderRepository.countByYearAndMonthAndStatus(now.getYear(), now.getMonthValue(), OrderStatusCode.PICKED_UP);
        var previousMonth = orderRepository.countByYearAndMonthAndStatus(
                now.minusMonths(1).getYear(), now.minusMonths(1).getMonthValue(), OrderStatusCode.PICKED_UP);
        var sameMonthLastYear = orderRepository.countByYearAndMonthAndStatus(now.getYear() - 1, now.getMonthValue(), OrderStatusCode.PICKED_UP);

        var previousDelta = previousMonth > 0 ? ((double) (currentMonth - previousMonth) / previousMonth) * 100 : 0;
        var lastYearDelta = sameMonthLastYear > 0 ? ((double) (currentMonth - sameMonthLastYear) / sameMonthLastYear) * 100 : 0;

        return new KpiWithDelta(currentMonth, previousMonth, sameMonthLastYear, previousDelta, lastYearDelta);
    }

    @Override
    public KpiWithDelta getYearTotal() {
        var now = LocalDate.now();
        var currentYear = orderRepository.countByYearAndStatus(now.getYear(), OrderStatusCode.PICKED_UP);
        var previousYear = orderRepository.countByYearAndStatus(now.getYear() - 1, OrderStatusCode.PICKED_UP);
        var twoYearsAgo = orderRepository.countByYearAndStatus(now.getYear() - 2, OrderStatusCode.PICKED_UP);

        var previousDelta = previousYear > 0 ? ((double) (currentYear - previousYear) / previousYear) * 100 : 0;
        var twoYearsAgoDelta = twoYearsAgo > 0 ? ((double) (currentYear - twoYearsAgo) / twoYearsAgo) * 100 : 0;

        return new KpiWithDelta(currentYear, previousYear, twoYearsAgo, previousDelta, twoYearsAgoDelta);
    }
}
