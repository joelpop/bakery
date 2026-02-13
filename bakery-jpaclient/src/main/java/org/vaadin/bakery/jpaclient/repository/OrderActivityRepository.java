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

    /**
     * Returns activity ID and order ID pairs for globally-unread staff messages
     * not authored by the given user.
     */
    @Query("SELECT a.id, a.order.id FROM OrderActivityEntity a " +
           "WHERE a.type = org.vaadin.bakery.jpamodel.code.OrderActivityTypeCode.STAFF_MESSAGE " +
           "AND a.read = false AND a.author.id != :userId")
    List<Object[]> findGloballyUnreadStaffMessages(@Param("userId") Long userId);

    /**
     * Returns activity ID and order ID pairs for globally-unread staff messages
     * within the given orders, not authored by the given user.
     */
    @Query("SELECT a.id, a.order.id FROM OrderActivityEntity a " +
           "WHERE a.order.id IN :orderIds " +
           "AND a.type = org.vaadin.bakery.jpamodel.code.OrderActivityTypeCode.STAFF_MESSAGE " +
           "AND a.read = false AND a.author.id != :userId")
    List<Object[]> findGloballyUnreadStaffMessagesForOrders(@Param("userId") Long userId,
                                                           @Param("orderIds") Collection<Long> orderIds);

    /** Marks specific activities as globally read. */
    @Modifying
    @Query("UPDATE OrderActivityEntity a SET a.read = true WHERE a.id IN :ids AND a.read = false")
    int markAsReadByIds(@Param("ids") Collection<Long> ids);
}
