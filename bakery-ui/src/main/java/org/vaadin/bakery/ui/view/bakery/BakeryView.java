package org.vaadin.bakery.ui.view.bakery;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.ElementEffect;
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
import java.util.List;

/**
 * Kanban-style bakery production board showing order items as tiles in status swimlanes.
 */
@Route("bakery")
@PageTitle("Bakery Board")
@Menu(order = 1.5, icon = LineAwesomeIconUrl.BREAD_SLICE_SOLID)
@RolesAllowed({UserRole.ROLE_ADMIN, UserRole.ROLE_BAKER})
public class BakeryView extends VerticalLayout {

    private final transient BakeryService bakeryService;
    private final transient OrderService orderService;
    private final transient ValueSignal<Integer> refreshTriggerSignal;

    private final BakerySwimlane reviewSwimlane;
    private final BakerySwimlane acceptedSwimlane;
    private final BakerySwimlane inProgressSwimlane;
    private final BakerySwimlane completedSwimlane;

    private LocalDate startDate;
    private LocalDate endDate;

    /** Creates the bakery board view with toolbar and four swimlane columns. */
    public BakeryView(BakeryService bakeryService, OrderService orderService) {
        this.bakeryService = bakeryService;
        this.orderService = orderService;

        addClassName("bakery-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        startDate = LocalDate.now();
        endDate = LocalDate.now();

        // Signal definitions
        refreshTriggerSignal = new ValueSignal<>(0);

        // Toolbar with date range presets
        var toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.addClassNames(
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.Gap.SMALL
        );
        toolbar.getStyle().set("background", "var(--lumo-base-color)")
                .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
                .set("flex-shrink", "0");

        var todayButton = new Button("Today", _ -> setDateRange(LocalDate.now(), LocalDate.now()));
        todayButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        var tomorrowButton = new Button("Today + Tomorrow", _ ->
                setDateRange(LocalDate.now(), LocalDate.now().plusDays(1)));
        tomorrowButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        var weekButton = new Button("This Week", _ ->
                setDateRange(LocalDate.now(), LocalDate.now().plusDays(6)));
        weekButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

        toolbar.add(todayButton, tomorrowButton, weekButton);

        // Swimlanes
        reviewSwimlane = new BakerySwimlane("To Review",
                List.of(OrderItemStatus.PENDING_REVIEW));
        acceptedSwimlane = new BakerySwimlane("Accepted",
                List.of(OrderItemStatus.ACCEPTED, OrderItemStatus.REJECTED));
        inProgressSwimlane = new BakerySwimlane("In Progress",
                List.of(OrderItemStatus.IN_PROGRESS));
        completedSwimlane = new BakerySwimlane("Done",
                List.of(OrderItemStatus.PRODUCED, OrderItemStatus.CANCELED));

        // Wire up tile click handlers
        reviewSwimlane.setTileClickHandler(this::onTileClick);
        acceptedSwimlane.setTileClickHandler(this::onTileClick);
        inProgressSwimlane.setTileClickHandler(this::onTileClick);
        completedSwimlane.setTileClickHandler(this::onTileClick);

        // Wire up drop handlers
        reviewSwimlane.setDropHandler((tile, _) ->
                transitionTile(tile, OrderItemStatus.PENDING_REVIEW));
        acceptedSwimlane.setDropHandler(this::onAcceptedDrop);
        inProgressSwimlane.setDropHandler((tile, _) ->
                transitionTile(tile, OrderItemStatus.IN_PROGRESS));
        completedSwimlane.setDropHandler((tile, _) ->
                transitionTile(tile, OrderItemStatus.PRODUCED));

        var swimlanesLayout = new HorizontalLayout();
        swimlanesLayout.setSizeFull();
        swimlanesLayout.setSpacing(false);
        swimlanesLayout.addClassName("bakery-swimlanes");
        swimlanesLayout.add(reviewSwimlane, acceptedSwimlane, inProgressSwimlane, completedSwimlane);

        // Equal width swimlanes
        swimlanesLayout.setFlexGrow(1, reviewSwimlane, acceptedSwimlane, inProgressSwimlane, completedSwimlane);

        // Reactive effect: refreshes the board when data changes
        ElementEffect.effect(this.getElement(), () -> {
            DataChangeSignals.orderVersion().get();
            refreshTriggerSignal.get();
            refreshBoard();
        });

        // Layout assembly
        add(toolbar, swimlanesLayout);
        setFlexGrow(1, swimlanesLayout);
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
        var tiles = bakeryService.listTiles(startDate, endDate);
        reviewSwimlane.setTiles(tiles);
        acceptedSwimlane.setTiles(tiles);
        inProgressSwimlane.setTiles(tiles);
        completedSwimlane.setTiles(tiles);
    }

    private void onTileClick(BakeryTile tile) {
        var overlay = new TileDetailOverlay(tile, bakeryService);
        overlay.addUndoListener(e -> {
            undoTransition(e.getTile());
        });
        overlay.open();
    }

    private void onAcceptedDrop(BakeryTile tile, OrderItemStatus targetStatus) {
        // Dropping onto the Accepted swimlane: determine if it should be ACCEPTED or REJECTED
        if (tile.getStatus() == OrderItemStatus.PENDING_REVIEW) {
            // From review → accept
            transitionTile(tile, OrderItemStatus.ACCEPTED);
        } else {
            // Other drops onto accepted lane default to ACCEPTED
            transitionTile(tile, OrderItemStatus.ACCEPTED);
        }
    }

    private void transitionTile(BakeryTile tile, OrderItemStatus newStatus) {
        // Validate: don't transition to same status
        if (tile.getStatus() == newStatus) {
            return;
        }

        // Rejection requires a message dialog
        if (newStatus == OrderItemStatus.REJECTED) {
            var rejectDialog = new RejectMessageDialog();
            rejectDialog.addConfirmListener(e -> {
                // For rejected transitions, we'd need a reject method on the service
                // For now, just transition the status
                performTransition(tile, newStatus);
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
                && tile.getDueDate() != null
                && tile.getDueDate().isAfter(LocalDate.now())) {
            Notification.show("Can only start production for today's items",
                    5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        performTransition(tile, newStatus);
    }

    private void performTransition(BakeryTile tile, OrderItemStatus newStatus) {
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
                            orderService.updateItemStatus(
                                    detail.getOrderId(), detail.getItemId(),
                                    newStatus, itemVersions.get(i));
                            break;
                        }
                    }
                }
            } else {
                orderService.updateItemStatus(
                        tile.getOrderId(), tile.getItemId(),
                        newStatus, tile.getItemVersion());
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
