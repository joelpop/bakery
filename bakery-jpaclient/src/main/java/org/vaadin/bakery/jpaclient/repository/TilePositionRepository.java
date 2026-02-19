package org.vaadin.bakery.jpaclient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vaadin.bakery.jpamodel.code.OrderItemStatusCode;
import org.vaadin.bakery.jpamodel.entity.TilePositionEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for bakery board tile position persistence.
 */
@Repository
public interface TilePositionRepository extends JpaRepository<TilePositionEntity, Long> {

    /** Finds the position entry for a specific tile. */
    Optional<TilePositionEntity> findBySwimlaneAndDueDateAndGroupingKey(
            OrderItemStatusCode swimlane, LocalDate dueDate, String groupingKey);

    /** Finds all position entries for a given swimlane and date, ordered by position. */
    List<TilePositionEntity> findBySwimlaneAndDueDateOrderByPositionAsc(
            OrderItemStatusCode swimlane, LocalDate dueDate);

    /** Deletes all position entries for a specific grouping key. */
    void deleteByGroupingKey(String groupingKey);
}
