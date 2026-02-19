package org.vaadin.bakery.jpamodel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.vaadin.bakery.jpamodel.code.OrderItemStatusCode;

/**
 * Undo stack entry for bakery board tile transitions.
 * Stores the previous status and associated activity IDs so a transition can be reverted.
 */
@Entity
@Table(name = "tile_undo_entry")
public class TileUndoEntryEntity extends AbstractEntity {

    @NotNull
    @Column(name = "grouping_key", nullable = false)
    private String groupingKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false)
    private OrderItemStatusCode previousStatus;

    @NotNull
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Column(name = "activity_ids")
    private String activityIds;

    public String getGroupingKey() {
        return groupingKey;
    }

    public void setGroupingKey(String groupingKey) {
        this.groupingKey = groupingKey;
    }

    public OrderItemStatusCode getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(OrderItemStatusCode previousStatus) {
        this.previousStatus = previousStatus;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getActivityIds() {
        return activityIds;
    }

    public void setActivityIds(String activityIds) {
        this.activityIds = activityIds;
    }
}
