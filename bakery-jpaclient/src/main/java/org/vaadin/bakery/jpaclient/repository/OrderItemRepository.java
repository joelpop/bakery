package org.vaadin.bakery.jpaclient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vaadin.bakery.jpamodel.code.OrderItemStatusCode;
import org.vaadin.bakery.jpamodel.entity.OrderItemEntity;
import org.vaadin.bakery.jpamodel.projection.OrderItemSummaryProjection;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for order item entity operations.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    /** Finds all order items for the given order ID, ordered by item ID ascending. */
    List<OrderItemEntity> findByOrderIdOrderByIdAsc(Long orderId);

    /** Deletes all order items belonging to the specified order. */
    @Modifying
    @Query("DELETE FROM OrderItemEntity oi WHERE oi.order.id = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);

    /** Returns summary projections for all order items of the given order, including product name and size. */
    @Query("SELECT oi.id AS id, oi.status AS status, oi.quantity AS quantity, oi.details AS details, " +
           "oi.unitPrice AS unitPrice, oi.lineTotal AS lineTotal, p.name AS productName, p.size AS productSize " +
           "FROM OrderItemEntity oi JOIN oi.product p " +
           "WHERE oi.order.id = :orderId ORDER BY oi.id ASC")
    List<OrderItemSummaryProjection> findProjectedByOrderId(@Param("orderId") Long orderId);

    /**
     * Finds all order items for the bakery board: items within the date range plus
     * overdue non-terminal items. Eagerly fetches product, order, and customer.
     */
    @Query("SELECT oi FROM OrderItemEntity oi " +
           "JOIN FETCH oi.product p " +
           "JOIN FETCH oi.order o " +
           "LEFT JOIN FETCH o.customer c " +
           "WHERE (o.dueDate BETWEEN :startDate AND :endDate) " +
           "OR (o.dueDate < :startDate AND oi.status NOT IN :terminalStatuses)")
    List<OrderItemEntity> findItemsForBakeryBoard(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("terminalStatuses") List<OrderItemStatusCode> terminalStatuses);
}
