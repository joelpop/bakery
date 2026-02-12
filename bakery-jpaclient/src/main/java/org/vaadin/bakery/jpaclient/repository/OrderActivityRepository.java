package org.vaadin.bakery.jpaclient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.vaadin.bakery.jpamodel.code.OrderActivityTypeCode;
import org.vaadin.bakery.jpamodel.entity.OrderActivityEntity;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Repository for order activity timeline entries.
 */
@Repository
public interface OrderActivityRepository extends JpaRepository<OrderActivityEntity, Long> {

    /** Finds all activities for the given order, ordered chronologically. */
    List<OrderActivityEntity> findByOrderIdOrderByPostedAtAsc(Long orderId);

    /** Finds activities for the given order posted after the specified time, ordered chronologically. */
    List<OrderActivityEntity> findByOrderIdAndPostedAtAfterOrderByPostedAtAsc(Long orderId, Instant after);

    /** Returns the IDs of orders (from the given set) that have unread staff messages. */
    @Query("SELECT DISTINCT a.order.id FROM OrderActivityEntity a " +
           "WHERE a.order.id IN :orderIds " +
           "AND a.type = org.vaadin.bakery.jpamodel.code.OrderActivityTypeCode.STAFF_MESSAGE " +
           "AND a.read = false")
    Set<Long> findOrderIdsWithUnreadMessages(@Param("orderIds") Collection<Long> orderIds);

    /** Marks all unread staff messages for the given order as read. */
    @Modifying
    @Query("UPDATE OrderActivityEntity a SET a.read = true " +
           "WHERE a.order.id = :orderId " +
           "AND a.type = org.vaadin.bakery.jpamodel.code.OrderActivityTypeCode.STAFF_MESSAGE " +
           "AND a.read = false")
    int markAllReadByOrderId(@Param("orderId") Long orderId);
}
