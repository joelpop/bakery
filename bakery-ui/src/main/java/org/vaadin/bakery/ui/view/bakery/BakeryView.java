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
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.bakery.service.BakeryService;
import org.vaadin.bakery.service.StaleDataException;
import org.vaadin.bakery.ui.event.DataChangeSignals;
import org.vaadin.bakery.uimodel.data.BakeryTile;
import org.vaadin.bakery.uimodel.type.OrderItemStatus;
import org.vaadin.bakery.uimodel.type.UserRole;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Kanban-style bakery production board showing order items as tiles in status swimlanes.
 *
 * <p>Two update strategies:
 * <ul>
 *   <li><b>Full load</b> ({@link #fullLoad}): Initial render and date range changes.</li>
 *   <li><b>Reconcile</b> ({@link #reconcile}): Reads the {@code tileChanges} SharedMapSignal
 *       to identify which tiles changed, then re-renders affected swimlanes and applies
 *       highlights. Used for both local DnD and cross-session changes — one code path.</li>
 * </ul>
 */
@Route("bakery")
@PageTitle("Bakery Board")
@Menu(order = 1.5, icon = LineAwesomeIconUrl.BREAD_SLICE_SOLID)
@RolesAllowed({UserRole.ROLE_ADMIN, UserRole.ROLE_BAKER})
public class BakeryView extends Composite<VerticalLayout> implements HasSize, HasStyle {

    private final transient BakeryService bakeryService;
    private final ValueSignal<Integer> refreshTriggerSignal;

    private final BakerySwimlane reviewSwimlane;
    private final BakerySwimlane acceptedSwimlane;
    private final BakerySwimlane inProgressSwimlane;
    private final BakerySwimlane completedSwimlane;

    private LocalDate startDate;
    private LocalDate endDate;
    private List<BakeryTile> cachedTiles;
    private long lastUpdateTimestamp;
    private boolean initialized;
    private boolean dateRangeChanged;
    private boolean operationInProgress;

    /** Creates the bakery board view with toolbar and four swimlane columns. */
    public BakeryView(BakeryService bakeryService) {
        this.bakeryService = bakeryService;

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
                java.util.Map.of(OrderItemStatus.PRODUCED, 2));

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

        // Signal bindings — dispatcher: full load vs cross-session reconciliation
        Signal.effect(this, () -> {
            DataChangeSignals.orderVersion().get();
            refreshTriggerSignal.get();
            if (operationInProgress) {
                return;
            }

            if (!initialized || dateRangeChanged) {
                initialized = true;
                dateRangeChanged = false;
                fullLoad();
            } else {
                reconcile();
            }
        });

        // Content layout
        var content = getContent();
        content.addClassName("bakery-view");
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.add(toolbar, swimlanesLayout);
    }

    // ========== Path 1: Full Load ==========

    /**
     * Fetches all tiles and rebuilds all swimlanes from scratch.
     * Used for initial load and date range changes.
     */
    private void fullLoad() {
        cachedTiles = bakeryService.listTiles(startDate, endDate);
        reviewSwimlane.renderAll(cachedTiles);
        acceptedSwimlane.renderAll(cachedTiles);
        inProgressSwimlane.renderAll(cachedTiles);
        completedSwimlane.renderAll(cachedTiles);
        lastUpdateTimestamp = System.currentTimeMillis();
    }

    // ========== Persist + Reconcile ==========

    /**
     * Reorders a tile within its current status. Persists the new order,
     * then triggers a reconcile to update the DOM.
     */
    private void reorderTile(BakeryTile tile, OrderItemStatus status, int position) {
        try {
            var currentOrder = cachedTiles.stream()
                    .filter(t -> t.getStatus() == status && t.getDueDate().equals(tile.getDueDate()))
                    .sorted(Comparator.comparingInt(BakeryTile::getPosition))
                    .map(BakeryTile::getGroupingKey)
                    .collect(Collectors.toCollection(ArrayList::new));
            currentOrder.remove(tile.getGroupingKey());
            var insertAt = Math.min(position, currentOrder.size());
            currentOrder.add(insertAt, tile.getGroupingKey());

            runGuarded(() -> bakeryService.saveTileOrder(status, tile.getDueDate(),
                    currentOrder, tile.getGroupingKey()));
            triggerRefresh();
        } catch (Exception e) {
            Notification.show("Failed to reorder: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Executes the atomic tile transition. Persists via the service,
     * then triggers a reconcile to update the DOM.
     */
    private void executeTransition(BakeryTile tile, OrderItemStatus newStatus,
                                    int position, String rejectionMessage) {
        try {
            runGuarded(() ->
                    bakeryService.transitionTile(tile, newStatus, position, rejectionMessage));
            triggerRefresh();

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

    // ========== Reconciliation ==========

    /**
     * Handles both local and cross-session changes. Reads the {@code tileChanges}
     * SharedMapSignal to identify which tiles changed, re-fetches all data, then
     * re-renders affected swimlanes and applies highlights.
     */
    private void reconcile() {
        var newTiles = bakeryService.listTiles(startDate, endDate);
        var tileChanges = DataChangeSignals.tileChanges().peek();
        var now = System.currentTimeMillis();

        // 1. Build key indexes
        var oldByKey = new HashMap<String, BakeryTile>();
        for (var t : cachedTiles) {
            oldByKey.put(t.getGroupingKey(), t);
        }
        var newByKey = new HashMap<String, BakeryTile>();
        for (var t : newTiles) {
            newByKey.put(t.getGroupingKey(), t);
        }

        // 2. Find highlighted keys from tileChanges SharedMapSignal
        var highlightKeys = new HashSet<String>();
        for (var key : oldByKey.keySet()) {
            var entry = tileChanges.get(key);
            if (entry != null && entry.peek() > lastUpdateTimestamp) {
                highlightKeys.add(key);
            }
        }
        for (var key : newByKey.keySet()) {
            var entry = tileChanges.get(key);
            if (entry != null && entry.peek() > lastUpdateTimestamp) {
                highlightKeys.add(key);
            }
        }

        // 3. Find affected swimlanes from highlights and structural changes
        var affectedSwimlanes = new HashSet<BakerySwimlane>();

        for (var key : highlightKeys) {
            var oldTile = oldByKey.get(key);
            var newTile = newByKey.get(key);
            if (oldTile != null) {
                affectedSwimlanes.add(swimlaneForStatus(oldTile.getStatus()));
            }
            if (newTile != null) {
                affectedSwimlanes.add(swimlaneForStatus(newTile.getStatus()));
            }
        }

        // Tiles that appeared or disappeared (batch key changes on transition)
        for (var key : oldByKey.keySet()) {
            if (!newByKey.containsKey(key)) {
                affectedSwimlanes.add(swimlaneForStatus(oldByKey.get(key).getStatus()));
            }
        }
        for (var key : newByKey.keySet()) {
            if (!oldByKey.containsKey(key)) {
                affectedSwimlanes.add(swimlaneForStatus(newByKey.get(key).getStatus()));
            }
        }

        if (affectedSwimlanes.isEmpty()) {
            cachedTiles = newTiles;
            lastUpdateTimestamp = now;
            return;
        }

        // 4. Re-render affected swimlanes
        for (var swimlane : affectedSwimlanes) {
            swimlane.renderAll(newTiles);
        }

        // 5. Apply highlights
        for (var key : highlightKeys) {
            var newTile = newByKey.get(key);
            if (newTile != null) {
                swimlaneForStatus(newTile.getStatus()).highlightTile(key);
            }
        }

        cachedTiles = newTiles;
        lastUpdateTimestamp = now;
    }

    // ========== Event Handlers ==========

    private void setDateRange(LocalDate start, LocalDate end) {
        this.startDate = start;
        this.endDate = end;
        dateRangeChanged = true;
        triggerRefresh();
    }

    private void triggerRefresh() {
        refreshTriggerSignal.update(v -> v + 1);
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
     * Cleans up drag mode on all swimlanes first, because the subsequent
     * reconcile will replace tile components — destroying the drag source
     * element and preventing the browser's {@code dragend} event from
     * reaching the server.
     */
    private void onTileDrop(BakeryTile tile, OrderItemStatus targetStatus, int position) {
        onTileDragEnd();

        if (tile.getStatus() == targetStatus) {
            reorderTile(tile, targetStatus, position);
        } else {
            transitionTile(tile, targetStatus, position);
        }
    }

    private void transitionTile(BakeryTile tile, OrderItemStatus newStatus, int position) {
        // Rejection requires a message dialog
        if (newStatus == OrderItemStatus.REJECTED) {
            var rejectDialog = new RejectMessageDialog();
            rejectDialog.addConfirmListener(e ->
                    executeTransition(tile, newStatus, position, e.getMessage()));
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

        executeTransition(tile, newStatus, position, null);
    }

    private void undoTransition(BakeryTile tile) {
        try {
            var previousStatus = new OrderItemStatus[1];
            runGuarded(() ->
                    previousStatus[0] = bakeryService.undoTileTransition(tile.getGroupingKey()));
            fullLoad();
            Notification.show("Reverted to " + previousStatus[0].getDisplayName(),
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Failed to undo: " + e.getMessage(),
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // ========== Utilities ==========

    /**
     * Runs a service operation with the signal effect guard active.
     * Prevents mid-operation refreshes caused by synchronous signal notifications.
     */
    private void runGuarded(Runnable operation) {
        operationInProgress = true;
        try {
            operation.run();
        } finally {
            operationInProgress = false;
        }
    }

    private BakerySwimlane swimlaneForStatus(OrderItemStatus status) {
        for (var swimlane : List.of(reviewSwimlane, acceptedSwimlane, inProgressSwimlane, completedSwimlane)) {
            if (swimlane.handlesStatus(status)) {
                return swimlane;
            }
        }
        throw new IllegalArgumentException("No swimlane for " + status);
    }

    /**
     * Computes the set of statuses that are valid (active) drop targets for the given tile.
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
}
