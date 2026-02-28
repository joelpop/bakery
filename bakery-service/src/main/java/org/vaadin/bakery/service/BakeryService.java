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
     * Persists positions for all tiles in a swimlane/date group at once.
     *
     * <p>Each grouping key in the list is assigned its list index as position (0, 1, 2, ...),
     * ensuring consistent ordering across all tiles in the group.
     *
     * @param swimlane           the swimlane (item status)
     * @param dueDate            the date group
     * @param orderedGroupingKeys the grouping keys in their desired display order
     * @param movedGroupingKey   the grouping key of the tile that was directly moved
     */
    void saveTileOrder(OrderItemStatus swimlane, LocalDate dueDate,
                       List<String> orderedGroupingKeys, String movedGroupingKey);

    /**
     * Manages tile positions for a status transition.
     *
     * <p>Removes the tile from the source (fromStatus, dueDate) group, inserts it into
     * the target (toStatus, dueDate) group at the specified position, and resequences
     * both groups to maintain sequential positions (0, 1, 2, ...).
     *
     * @param groupingKey the tile's grouping key (batch keys are resolved internally)
     * @param fromStatus  the tile's current status (source swimlane)
     * @param toStatus    the target status (target swimlane)
     * @param dueDate     the date group the tile belongs to
     * @param position    desired zero-based position in the target group;
     *                    {@code Integer.MAX_VALUE} means append to end
     */
    void transitionTilePosition(String groupingKey, OrderItemStatus fromStatus,
                                OrderItemStatus toStatus, LocalDate dueDate, int position);

    /**
     * Atomically transitions a tile: position management, item status updates,
     * system events, order status roll-up, and undo recording in one transaction.
     * Fires a single change notification per affected order on success.
     *
     * @param tile              the tile being transitioned (carries grouping key, versions, etc.)
     * @param newStatus         the target item status
     * @param position          zero-based position in the target swimlane
     * @param rejectionMessage  rejection message text, or {@code null} for non-rejection transitions
     * @throws StaleDataException if any item version has been concurrently modified
     */
    void transitionTile(BakeryTile tile, OrderItemStatus newStatus,
                        int position, String rejectionMessage);

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
