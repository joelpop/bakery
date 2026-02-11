package org.vaadin.bakery.jpaclient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vaadin.bakery.jpamodel.code.OrderStatusCode;
import org.vaadin.bakery.jpamodel.entity.OrderEntity;
import org.vaadin.bakery.jpamodel.projection.OrderDashboardProjection;
import org.vaadin.bakery.jpamodel.projection.OrderTimeProjection;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for order entity operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    /** Finds all orders with the given status. */
    List<OrderEntity> findByStatus(OrderStatusCode status);

    /** Finds all orders for the given due date, ordered by due time ascending. */
    List<OrderEntity> findByDueDateOrderByDueTimeAsc(LocalDate dueDate);

    /** Finds all orders with due dates in the given range, ordered by due date and time ascending. */
    List<OrderEntity> findByDueDateBetweenOrderByDueDateAscDueTimeAsc(LocalDate startDate, LocalDate endDate);

    /** Finds all orders for the given customer, ordered by due date and time descending. */
    List<OrderEntity> findByCustomerIdOrderByDueDateDescDueTimeDesc(Long customerId);

    /** Returns the count of orders with the given status. */
    long countByStatus(OrderStatusCode status);

    /** Returns the count of orders for the given due date. */
    long countByDueDate(LocalDate dueDate);

    /** Returns the count of orders for the given due date, excluding a specific status. */
    long countByDueDateAndStatusNot(LocalDate dueDate, OrderStatusCode status);

    /** Returns the count of orders for the given due date, excluding the specified statuses. */
    long countByDueDateAndStatusNotIn(LocalDate dueDate, List<OrderStatusCode> statuses);

    /** Returns due date and time projections for orders on the given date, excluding specified statuses. */
    @Query("SELECT o.dueDate AS dueDate, o.dueTime AS dueTime FROM OrderEntity o " +
           "WHERE o.dueDate = :dueDate AND o.status NOT IN :excludedStatuses " +
           "ORDER BY o.dueTime ASC")
    List<OrderTimeProjection> findNextPickupTimeByDate(
            @Param("dueDate") LocalDate dueDate,
            @Param("excludedStatuses") List<OrderStatusCode> excludedStatuses);

    /** Finds upcoming orders from the given start date with customer, location, items, and product details eagerly fetched. */
    @Query("SELECT o FROM OrderEntity o " +
           "LEFT JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.location " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product " +
           "WHERE o.dueDate >= :startDate " +
           "ORDER BY o.dueDate ASC, o.dueTime ASC")
    List<OrderEntity> findUpcomingOrdersWithDetails(@Param("startDate") LocalDate startDate);

    /** Finds orders for the dashboard on the given date, excluding specified statuses, with customer and location eagerly fetched. */
    @Query("SELECT o FROM OrderEntity o " +
           "LEFT JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.location " +
           "WHERE o.dueDate = :dueDate AND o.status NOT IN :excludedStatuses " +
           "ORDER BY o.dueTime ASC")
    List<OrderEntity> findDashboardOrdersByDate(
            @Param("dueDate") LocalDate dueDate,
            @Param("excludedStatuses") List<OrderStatusCode> excludedStatuses);

    /** Returns the count of orders within a date range that have the given status. */
    @Query("SELECT COUNT(o) FROM OrderEntity o " +
           "WHERE o.dueDate BETWEEN :startDate AND :endDate " +
           "AND o.status = :status")
    long countByDueDateBetweenAndStatus(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") OrderStatusCode status);

    /** Returns the count of orders in the given year with the specified status. */
    @Query("SELECT COUNT(o) FROM OrderEntity o " +
           "WHERE YEAR(o.dueDate) = :year AND o.status = :status")
    long countByYearAndStatus(@Param("year") int year, @Param("status") OrderStatusCode status);

    /** Returns the count of orders in the given year and month with the specified status. */
    @Query("SELECT COUNT(o) FROM OrderEntity o " +
           "WHERE YEAR(o.dueDate) = :year AND MONTH(o.dueDate) = :month AND o.status = :status")
    long countByYearAndMonthAndStatus(
            @Param("year") int year,
            @Param("month") int month,
            @Param("status") OrderStatusCode status);

    /** Checks whether orders exist for the given customer with any of the specified statuses. */
    boolean existsByCustomerIdAndStatusIn(Long customerId, List<OrderStatusCode> statuses);

    /** Finds all orders for the given customer with any of the specified statuses. */
    List<OrderEntity> findByCustomerIdAndStatusIn(Long customerId, List<OrderStatusCode> statuses);
}
