package org.vaadin.bakery.jpamodel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.vaadin.bakery.jpamodel.code.OrderItemStatusCode;

import java.time.Instant;

/**
 * Tracks order item status changes for audit history.
 */
@Entity
@Table(name = "order_item_status_history")
public class OrderItemStatusHistoryEntity extends AbstractEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItemEntity orderItem;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderItemStatusCode status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private UserEntity changedBy;

    @NotNull
    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    public OrderItemEntity getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(OrderItemEntity orderItem) {
        this.orderItem = orderItem;
    }

    public OrderItemStatusCode getStatus() {
        return status;
    }

    public void setStatus(OrderItemStatusCode status) {
        this.status = status;
    }

    public UserEntity getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(UserEntity changedBy) {
        this.changedBy = changedBy;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }
}
