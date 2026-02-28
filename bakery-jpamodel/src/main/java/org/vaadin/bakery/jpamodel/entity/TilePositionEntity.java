package org.vaadin.bakery.jpamodel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import org.vaadin.bakery.jpamodel.code.OrderItemStatusCode;

import java.time.LocalDate;

/**
 * Persists the position of a bakery board tile within its swimlane.
 * Allows bakers to reorder tiles and have their arrangement preserved across sessions.
 */
@Entity
@Table(name = "tile_position", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"swimlane", "due_date", "grouping_key"})
})
public class TilePositionEntity extends AbstractEntity {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "swimlane", nullable = false)
    private OrderItemStatusCode swimlane;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull
    @Column(name = "grouping_key", nullable = false)
    private String groupingKey;

    @NotNull
    @Column(name = "position", nullable = false)
    private Integer position;

    public OrderItemStatusCode getSwimlane() {
        return swimlane;
    }

    public void setSwimlane(OrderItemStatusCode swimlane) {
        this.swimlane = swimlane;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getGroupingKey() {
        return groupingKey;
    }

    public void setGroupingKey(String groupingKey) {
        this.groupingKey = groupingKey;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}
