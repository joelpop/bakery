package org.vaadin.bakery.ui.view.bakery;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.bakery.service.BakeryService;
import org.vaadin.bakery.service.OrderService;
import org.vaadin.bakery.service.StaleDataException;
import org.vaadin.bakery.ui.event.DataChangeSignals;
import org.vaadin.bakery.uimodel.data.BakeryTile;
import org.vaadin.bakery.uimodel.type.OrderItemStatus;
import org.vaadin.bakery.uimodel.type.UserRole;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kanban-style bakery production board showing order items as tiles in status swimlanes.
 */
@Route("bakery")
@PageTitle("Bakery Board")
@Menu(order = 1.5, icon = LineAwesomeIconUrl.BREAD_SLICE_SOLID)
@RolesAllowed({UserRole.ROLE_ADMIN, UserRole.ROLE_BAKER})
public class BakeryView extends Composite<VerticalLayout> implements HasSize, HasStyle {

    private final transient BakeryService bakeryService;
    private final transient OrderService orderService;
    private final ValueSignal<Integer> refreshTriggerSignal;

    private final BakerySwimlane reviewSwimlane;
    private final BakerySwimlane acceptedSwimlane;
    private final BakerySwimlane inProgressSwimlane;
    private final BakerySwimlane completedSwimlane;

    private LocalDate startDate;
    private LocalDate endDate;
    private List<BakeryTile> cachedTiles;

    /** Creates the bakery board view with toolbar and four swimlane columns. */
    public BakeryView(BakeryService bakeryService, OrderService orderService) {
        this.bakeryService = bakeryService;
        this.orderService = orderService;

        startDate = LocalDate.now();
        endDate = LocalDate.now();
        cachedTiles = List.of();

        // Signal definitions
        refreshTriggerSignal = new ValueSignal<>(0);

        // Component initializations
        var todayButton = new Button("Today", _ -> setDateRange(LocalDate.now(), LocalDate.now()));
        todayButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        var tomorrowButton = new Button("Today + Tomorrow", _ ->
                setDateRange(LocalDate.now(), LocalDate.now().plusDays(1)));
        tomorrowButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        var weekButton = new Button("This Week", _ ->
                setDateRange(LocalDate.now(), LocalDate.now().plusDays(6)));
        weekButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        var toolbar = new HorizontalLayout(todayButton, tomorrowButton, weekButton);
        toolbar.setWidthFull();
        toolbar.addClassNames(
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.Gap.SMALL
        );
        toolbar.getStyle().set("background", "var(--lumo-base-color)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("flex-shrink", "0");

        reviewSwimlane = new BakerySwimlane("To Review",
                List.of(OrderItemStatus.PENDING_REVIEW));
        acceptedSwimlane = new BakerySwimlane("Reviewed",
                List.of(OrderItemStatus.REJECTED, OrderItemStatus.ACCEPTED));
        inProgressSwimlane = new BakerySwimlane("In Progress",
                List.of(OrderItemStatus.IN_PROGRESS));
        completedSwimlane = new BakerySwimlane("Done",
                List.of(OrderItemStatus.PRODUCED, OrderItemStatus.CANCELED),
                Map.of(OrderItemStatus.PRODUCED, 2));

        var swimlanesLayout = new HorizontalLayout(
                reviewSwimlane, acceptedSwimlane, inProgressSwimlane, completedSwimlane);
        swimlanesLayout.setWidthFull();
        swimlanesLayout.setSpacing(false);
        swimlanesLayout.addClassName("bakery-swimlanes");
        swimlanesLayout.getStyle().set("flex", "1 1 0").set("min-height", "0");
        swimlanesLayout.setFlexGrow(1,
                reviewSwimlane, acceptedSwimlane, inProgressSwimlane, completedSwimlane);

        // Event handlers
        var allSwimlanes = List.of(reviewSwimlane, acceptedSwimlane, inProgressSwimlane, completedSwimlane);
        for (var swimlane : allSwimlanes) {
            swimlane.setTileClickHandler(this::onTileClick);
            swimlane.setTileDropHandler(this::onTileDrop);
            swimlane.setDragStartHandler(this::onTileDragStart);
            swimlane.setDragEndHandler(this::onTileDragEnd);
        }

        // Signal bindings
        Signal.effect(this, () -> {
            DataChangeSignals.orderVersion().get();
            refreshTriggerSignal.get();
            refreshBoard();
        });

        // Content layout
        var content = getContent();
        content.addClassName("bakery-view");
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.add(toolbar, swimlanesLayout);

    }

    private void setDateRange(LocalDate start, LocalDate end) {
        this.startDate = start;
        this.endDate = end;
        triggerRefresh();
    }

    private void triggerRefresh() {
        refreshTriggerSignal.update(v -> v + 1);
    }

    private void refreshBoard() {
        cachedTiles = bakeryService.listTiles(startDate, endDate);
        reviewSwimlane.setTiles(cachedTiles);
        acceptedSwimlane.setTiles(cachedTiles);
        inProgressSwimlane.setTiles(cachedTiles);
        completedSwimlane.setTiles(cachedTiles);
    }

    private void onTileClick(BakeryTile tile) {
        var overlay = new TileDetailOverlay(tile, bakeryService);
        overlay.addUndoListener(e -> undoTransition(e.getTile()));
        overlay.open();
    }

    private void onTileDragStart(BakeryTile tile) {
        var activeTargets = computeActiveTargets(tile);
        reviewSwimlane.enterDragMode(tile, activeTargets);
        acceptedSwimlane.enterDragMode(tile, activeTargets);
        inProgressSwimlane.enterDragMode(tile, activeTargets);
        completedSwimlane.enterDragMode(tile, activeTargets);

        // Enable auto-scroll on all swimlanes
        reviewSwimlane.enableDragAutoScroll();
        acceptedSwimlane.enableDragAutoScroll();
        inProgressSwimlane.enableDragAutoScroll();
        completedSwimlane.enableDragAutoScroll();
    }

    private void onTileDragEnd() {
        reviewSwimlane.exitDragMode();
        acceptedSwimlane.exitDragMode();
        inProgressSwimlane.exitDragMode();
        completedSwimlane.exitDragMode();

        // Disable auto-scroll on all swimlanes
        reviewSwimlane.disableDragAutoScroll();
        acceptedSwimlane.disableDragAutoScroll();
        inProgressSwimlane.disableDragAutoScroll();
        completedSwimlane.disableDragAutoScroll();
    }

    /**
     * Unified drop handler for both status transitions and reordering.
     *
     * <p>If the target status matches the tile's current status, this is a reorder operation.
     * Otherwise, it's a status transition with an optional position.
     */
    private void onTileDrop(BakeryTile tile, OrderItemStatus targetStatus, int position) {
        if (tile.getStatus() == targetStatus) {
            // Reorder within same status
            reorderTile(tile, targetStatus, position);
        } else {
            // Status transition (with optional position)
            transitionTile(tile, targetStatus, position);
        }
    }

    /**
     * Reorders a tile within its current status by computing the full new order
     * and persisting all positions at once.
     */
    private void reorderTile(BakeryTile tile, OrderItemStatus status, int position) {
        try {
            // Build the current ordered list of grouping keys for this status + date
            var currentOrder = new ArrayList<>(cachedTiles.stream()
                    .filter(t -> t.getStatus() == status && t.getDueDate().equals(tile.getDueDate()))
                    .map(BakeryTile::getGroupingKey)
                    .toList());

            // Remove the dragged tile from its current position
            currentOrder.remove(tile.getGroupingKey());

            // Insert at the target position (clamped to list size)
            var insertAt = Math.min(position, currentOrder.size());
            currentOrder.add(insertAt, tile.getGroupingKey());

            bakeryService.saveTileOrder(status, tile.getDueDate(), currentOrder);
            triggerRefresh();
        } catch (Exception e) {
            Notification.show("Failed to reorder: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Computes the set of statuses that are valid (active) drop targets for the given tile.
     * Applies static transition rules from the enum plus dynamic constraints.
     */
    private Set<OrderItemStatus> computeActiveTargets(BakeryTile tile) {
        var targets = new HashSet<>(tile.getStatus().getValidBakeryTargets());

        // Today-only constraint: IN_PROGRESS only allowed for today's items
        if (targets.contains(OrderItemStatus.IN_PROGRESS)
                && tile.getDueDate().isAfter(LocalDate.now())) {
            targets.remove(OrderItemStatus.IN_PROGRESS);
        }

        // Hold constraint: IN_PROGRESS blocked if tile is on hold
        if (targets.contains(OrderItemStatus.IN_PROGRESS) && tile.isOnHold()) {
            targets.remove(OrderItemStatus.IN_PROGRESS);
        }

        return targets;
    }

    private void transitionTile(BakeryTile tile, OrderItemStatus newStatus, int position) {
        // Rejection requires a message dialog
        if (newStatus == OrderItemStatus.REJECTED) {
            var rejectDialog = new RejectMessageDialog();
            rejectDialog.addConfirmListener(e -> {
                performTransition(tile, newStatus, e.getMessage());
                savePositionAfterTransition(tile, newStatus, position);
            });
            rejectDialog.open();
            return;
        }

        // Hold enforcement: prevent IN_PROGRESS if on hold
        if (newStatus == OrderItemStatus.IN_PROGRESS && tile.isOnHold()) {
            Notification.show("Item is on hold — resolve rejected sibling items first",
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        // Today-only rule for IN_PROGRESS
        if (newStatus == OrderItemStatus.IN_PROGRESS
                && tile.getDueDate().isAfter(LocalDate.now())) {
            Notification.show("Can only start production for today's items",
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        performTransition(tile, newStatus, null);
        savePositionAfterTransition(tile, newStatus, position);
    }

    private void performTransition(BakeryTile tile, OrderItemStatus newStatus, String rejectionMessage) {
        try {
            if (tile.isBatchable()) {
                // Transition all items in the batch
                var itemIds = tile.getItemIds();
                var itemVersions = tile.getItemVersions();
                for (var i = 0; i < itemIds.size(); i++) {
                    // Find the order for this item
                    var details = bakeryService.getTileDetails(tile.getGroupingKey());
                    for (var detail : details) {
                        if (detail.getItemId().equals(itemIds.get(i))) {
                            if (newStatus == OrderItemStatus.REJECTED && rejectionMessage != null) {
                                orderService.rejectItem(
                                        detail.getOrderId(), detail.getItemId(),
                                        rejectionMessage, itemVersions.get(i));
                            } else {
                                orderService.updateItemStatus(
                                        detail.getOrderId(), detail.getItemId(),
                                        newStatus, itemVersions.get(i));
                            }
                            break;
                        }
                    }
                }
            } else {
                if (newStatus == OrderItemStatus.REJECTED && rejectionMessage != null) {
                    orderService.rejectItem(
                            tile.getOrderId(), tile.getItemId(),
                            rejectionMessage, tile.getItemVersion());
                } else {
                    orderService.updateItemStatus(
                            tile.getOrderId(), tile.getItemId(),
                            newStatus, tile.getItemVersion());
                }
            }
            Notification.show("Status updated to " + newStatus.getDisplayName(),
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (StaleDataException _) {
            Notification.show("Item was modified by another user. Board refreshed.",
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
        } catch (Exception e) {
            Notification.show("Failed to update status: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Saves the tile position after a status transition, if a specific position was requested.
     */
    private void savePositionAfterTransition(BakeryTile tile, OrderItemStatus newStatus, int position) {
        try {
            bakeryService.saveTilePosition(tile.getGroupingKey(), newStatus, tile.getDueDate(), position);
        } catch (Exception e) {
            // Position save failure is non-critical; the transition already succeeded
        }
    }

    private void undoTransition(BakeryTile tile) {
        try {
            var previousStatus = bakeryService.undoTileTransition(tile.getGroupingKey());
            Notification.show("Reverted to " + previousStatus.getDisplayName(),
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Failed to undo: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
