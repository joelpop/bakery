package org.vaadin.bakery.jpaclient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.vaadin.bakery.jpamodel.entity.TileUndoEntryEntity;

import java.util.List;
import java.util.Optional;

/**
 * Repository for bakery board tile undo stack entries.
 */
@Repository
public interface TileUndoEntryRepository extends JpaRepository<TileUndoEntryEntity, Long> {

    /** Finds all undo entries for a grouping key, ordered by sequence number descending (newest first). */
    List<TileUndoEntryEntity> findByGroupingKeyOrderBySequenceNumberDesc(String groupingKey);

    /** Finds the top (most recent) undo entry for a grouping key. */
    Optional<TileUndoEntryEntity> findFirstByGroupingKeyOrderBySequenceNumberDesc(String groupingKey);

    /** Deletes all undo entries for a specific grouping key. */
    void deleteByGroupingKey(String groupingKey);
}
