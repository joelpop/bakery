package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.BakeryTile;
import org.vaadin.bakery.uimodel.data.BakeryTileDetail;
import org.vaadin.bakery.uimodel.type.OrderItemStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for the bakery board (Kanban-style production view).
 *
 * <p>Provides tile listing, tile detail drill-down, position persistence,
 * and undo support for status transitions.
 */
public interface BakeryService {

    /**
     * Returns all bakery tiles for the given date range, including overdue non-terminal items.
     *
     * @param startDate the start of the date range (inclusive)
     * @param endDate   the end of the date range (inclusive)
     * @return tiles grouped and sorted by swimlane, date, and persisted position
     */
    List<BakeryTile> listTiles(LocalDate startDate, LocalDate endDate);

    /**
     * Returns the contributing order items for a specific bakery tile.
     *
     * @param groupingKey the tile's grouping key
     * @return detail records for each contributing order item
     */
    List<BakeryTileDetail> getTileDetails(String groupingKey);

    /**
     * Persists the position of a tile within its swimlane.
     *
     * @param groupingKey the tile's grouping key
     * @param swimlane    the swimlane (item status) the tile is in
     * @param dueDate     the date group the tile belongs to
     * @param position    the zero-based position within the date group
     */
    void saveTilePosition(String groupingKey, OrderItemStatus swimlane, LocalDate dueDate, int position);

    /**
     * Returns the undo stack for a tile (list of previous statuses, newest first).
     *
     * @param groupingKey the tile's grouping key
     * @return list of previous statuses that can be undone
     */
    List<OrderItemStatus> getUndoStack(String groupingKey);

    /**
     * Undoes the most recent status transition for a tile.
     *
     * @param groupingKey the tile's grouping key
     * @return the status the tile was reverted to
     * @throws IllegalStateException if no undo entries exist
     */
    OrderItemStatus undoTileTransition(String groupingKey);
}
