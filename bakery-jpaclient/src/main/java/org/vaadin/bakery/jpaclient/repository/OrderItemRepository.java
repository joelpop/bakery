package org.vaadin.bakery.jpaclient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vaadin.bakery.jpamodel.entity.OrderItemEntity;
import org.vaadin.bakery.jpamodel.projection.OrderItemSummaryProjection;

import java.util.List;

/**
 * Repository for order item entity operations.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findByOrderIdOrderByIdAsc(Long orderId);

    @Modifying
    @Query("DELETE FROM OrderItemEntity oi WHERE oi.order.id = :orderId")
    void deleteByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT oi.id AS id, oi.quantity AS quantity, oi.details AS details, oi.unitPrice AS unitPrice, " +
           "oi.lineTotal AS lineTotal, p.name AS productName, p.size AS productSize " +
           "FROM OrderItemEntity oi JOIN oi.product p " +
           "WHERE oi.order.id = :orderId ORDER BY oi.id ASC")
    List<OrderItemSummaryProjection> findProjectedByOrderId(@Param("orderId") Long orderId);
}
