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

    List<OrderEntity> findByStatus(OrderStatusCode status);

    List<OrderEntity> findByDueDateOrderByDueTimeAsc(LocalDate dueDate);

    List<OrderEntity> findByDueDateBetweenOrderByDueDateAscDueTimeAsc(LocalDate startDate, LocalDate endDate);

    List<OrderEntity> findByCustomerIdOrderByDueDateDescDueTimeDesc(Long customerId);

    long countByStatus(OrderStatusCode status);

    long countByDueDate(LocalDate dueDate);

    long countByDueDateAndStatusNot(LocalDate dueDate, OrderStatusCode status);

    long countByDueDateAndStatusNotIn(LocalDate dueDate, List<OrderStatusCode> statuses);

    @Query("SELECT o.dueDate AS dueDate, o.dueTime AS dueTime FROM OrderEntity o " +
           "WHERE o.dueDate = :dueDate AND o.status NOT IN :excludedStatuses " +
           "ORDER BY o.dueTime ASC")
    List<OrderTimeProjection> findNextPickupTimeByDate(
            @Param("dueDate") LocalDate dueDate,
            @Param("excludedStatuses") List<OrderStatusCode> excludedStatuses);

    @Query("SELECT o FROM OrderEntity o " +
           "LEFT JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.location " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product " +
           "WHERE o.dueDate >= :startDate " +
           "ORDER BY o.dueDate ASC, o.dueTime ASC")
    List<OrderEntity> findUpcomingOrdersWithDetails(@Param("startDate") LocalDate startDate);

    @Query("SELECT o FROM OrderEntity o " +
           "LEFT JOIN FETCH o.customer " +
           "LEFT JOIN FETCH o.location " +
           "WHERE o.dueDate = :dueDate AND o.status NOT IN :excludedStatuses " +
           "ORDER BY o.dueTime ASC")
    List<OrderEntity> findDashboardOrdersByDate(
            @Param("dueDate") LocalDate dueDate,
            @Param("excludedStatuses") List<OrderStatusCode> excludedStatuses);

    @Query("SELECT COUNT(o) FROM OrderEntity o " +
           "WHERE o.dueDate BETWEEN :startDate AND :endDate " +
           "AND o.status = :status")
    long countByDueDateBetweenAndStatus(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") OrderStatusCode status);

    @Query("SELECT COUNT(o) FROM OrderEntity o " +
           "WHERE YEAR(o.dueDate) = :year AND o.status = :status")
    long countByYearAndStatus(@Param("year") int year, @Param("status") OrderStatusCode status);

    @Query("SELECT COUNT(o) FROM OrderEntity o " +
           "WHERE YEAR(o.dueDate) = :year AND MONTH(o.dueDate) = :month AND o.status = :status")
    long countByYearAndMonthAndStatus(
            @Param("year") int year,
            @Param("month") int month,
            @Param("status") OrderStatusCode status);

    boolean existsByCustomerIdAndStatusIn(Long customerId, List<OrderStatusCode> statuses);

    List<OrderEntity> findByCustomerIdAndStatusIn(Long customerId, List<OrderStatusCode> statuses);
}
