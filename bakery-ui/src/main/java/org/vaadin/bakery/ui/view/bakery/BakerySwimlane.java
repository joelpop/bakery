package org.vaadin.bakery.ui.view.bakery;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.uimodel.data.BakeryTile;
import org.vaadin.bakery.uimodel.type.OrderItemStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A single swimlane column in the bakery board.
 * Displays tiles filtered by one or more item statuses, grouped by date.
 *
 * <p>Supports two display modes:
 * <ul>
 *   <li><b>Normal mode:</b> Tiles in a scrollable list, grouped by date/status</li>
 *   <li><b>Drag mode:</b> A translucent overlay panel on the left ~30% of the
 *       column with status drop zones (First/Last sub-zones), plus reorder
 *       insertion points between tiles on the right. Applied uniformly to ALL
 *       swimlanes during a drag — source and target alike.</li>
 * </ul>
 *
 * <p>Updates are handled by calling {@link #renderAll} to rebuild the tile list,
 * followed by {@link #highlightTile} for changed tiles. This single update path
 * is used for both local DnD and cross-session changes.
 *
 * <p><b>Important:</b> The source swimlane must NOT remove the dragged component
 * from the DOM during drag, or the browser will cancel the drag operation.
 * The overlay panel approach avoids this by leaving all tiles in place.
 */
public class BakerySwimlane extends Composite<Div> implements HasSize, HasStyle {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d");

    private final List<OrderItemStatus> statuses;
    private final Map<OrderItemStatus, Integer> flexWeights;
    private final Span countBadge;
    private final Div tilesContainer;
    private final Scroller scroller;
    private final Div body;
    private Consumer<BakeryTile> tileClickHandler;
    private Consumer<BakeryTile> dragStartHandler;
    private Runnable dragEndHandler;
    private TileDropHandler tileDropHandler;

    /** O(1) component lookup by grouping key. Maintained by all update operations. */
    private Map<String, BakeryTileComponent> componentsByKey;

    // Drag mode state — tracks components inserted during drag so they can be removed on exit
    private List<Component> dragModeInsertions;
    private Div dropZonePanel;

    /**
     * Creates a swimlane for the given statuses with equal flex weights.
     *
     * @param title    the swimlane header title
     * @param statuses the item statuses this swimlane shows
     */
    public BakerySwimlane(String title, List<OrderItemStatus> statuses) {
        this(title, statuses, Map.of());
    }

    /**
     * Creates a swimlane for the given statuses with custom flex weights for drop zones.
     *
     * @param title       the swimlane header title
     * @param statuses    the item statuses this swimlane shows
     * @param flexWeights flex weight overrides per status (default is 1)
     */
    public BakerySwimlane(String title, List<OrderItemStatus> statuses,
                          Map<OrderItemStatus, Integer> flexWeights) {
        this.statuses = statuses;
        this.flexWeights = flexWeights;
        componentsByKey = new HashMap<>();

        // Component initializations
        var root = getContent();
        root.addClassName("bakery-swimlane");
        root.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN
        );

        var header = new Div();
        header.addClassName("swimlane-header");
        header.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.BoxSizing.BORDER
        );

        var titleSpan = new H3(title);
        titleSpan.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.SMALL);

        countBadge = new Span("0");
        countBadge.getElement().getThemeList().add("badge small contrast");

        header.add(titleSpan, countBadge);

        tilesContainer = new Div();
        tilesContainer.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL,
                LumoUtility.Padding.SMALL,
                LumoUtility.BoxSizing.BORDER
        );

        scroller = new Scroller(tilesContainer);
        scroller.addClassName("swimlane-tiles-layer");
        scroller.setSizeFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);

        body = new Div();
        body.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        body.getStyle()
                .set("flex", "1 1 0")
                .set("min-height", "0")
                .set("position", "relative");
        body.add(scroller);

        root.add(header, body);
    }

    // ========== Path 1: Full Rebuild ==========

    /**
     * Rebuilds the entire swimlane from scratch with the given tiles.
     * Used for initial load and date range changes. Populates {@link #componentsByKey}.
     *
     * @param allTiles all tiles from the board; this method filters to relevant statuses
     */
    public void renderAll(List<BakeryTile> allTiles) {
        cleanUpDragMode();
        componentsByKey = new HashMap<>();
        tilesContainer.removeAll();
        tilesContainer.addClassName(LumoUtility.Gap.SMALL);
        tilesContainer.addClassName(LumoUtility.Padding.SMALL);
        tilesContainer.getStyle().remove("flex");
        tilesContainer.getStyle().remove("min-height");

        var myTiles = allTiles.stream()
                .filter(t -> statuses.contains(t.getStatus()))
                .toList();

        countBadge.setText(String.valueOf(myTiles.size()));

        if (statuses.size() > 1) {
            for (var status : statuses) {
                var statusTiles = myTiles.stream()
                        .filter(t -> t.getStatus() == status)
                        .toList();
                if (!statusTiles.isEmpty()) {
                    addStatusSection(status.getDisplayName(), statusTiles);
                }
            }
        } else {
            myTiles.stream()
                    .collect(Collectors.groupingBy(
                            BakeryTile::getDueDate,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ))
                    .forEach(this::addDateGroup);
        }
    }

    /**
     * Applies a highlight animation to the tile with the given key, if present.
     *
     * @param key the tile's grouping key
     */
    public void highlightTile(String key) {
        var comp = componentsByKey.get(key);
        if (comp != null) {
            comp.addClassName("tile-highlight");
        }
    }

    // ========== Drag Mode ==========

    /** Returns whether this swimlane handles the given item status. */
    public boolean handlesStatus(OrderItemStatus status) {
        return statuses.contains(status);
    }

    /**
     * Enters drag mode with a translucent overlay panel and reorder insertion points.
     *
     * @param draggedTile   the tile being dragged
     * @param activeTargets the set of statuses that are valid transition targets
     */
    public void enterDragMode(BakeryTile draggedTile, Set<OrderItemStatus> activeTargets) {
        var sourceStatus = draggedTile.getStatus();

        // 1. Create and show overlay panel (all statuses in this swimlane)
        dropZonePanel = createDropZonePanel(activeTargets, sourceStatus);
        body.add(dropZonePanel);

        // 2. Insert reorder zones for active statuses that have tiles in this swimlane
        var reorderStatuses = new HashSet<>(activeTargets);
        reorderStatuses.add(sourceStatus); // always allow reorder of source status
        reorderStatuses.retainAll(new HashSet<>(statuses)); // only statuses in this swimlane
        insertReorderDropZones(draggedTile, reorderStatuses);

        // 3. Mark the dragged tile with CSS class
        var draggedComp = componentsByKey.get(draggedTile.getGroupingKey());
        if (draggedComp != null) {
            draggedComp.addClassName("v-dragged");
        }
    }

    /**
     * Exits drag mode, surgically removing the overlay panel and reorder zones.
     * Tiles were never moved, so no rebuild is needed.
     */
    public void exitDragMode() {
        cleanUpDragMode();
    }

    /**
     * Enables auto-scroll when dragging near the top or bottom edge of the scroller.
     * Installs a client-side dragover listener that smoothly scrolls the content.
     */
    public void enableDragAutoScroll() {
        scroller.getElement().executeJs(
                "const el = this;" +
                "let animId = null;" +
                "const EDGE = 50;" +
                "const SPEED = 6;" +
                "function onDragOver(e) {" +
                "  const rect = el.getBoundingClientRect();" +
                "  const y = e.clientY - rect.top;" +
                "  const h = rect.height;" +
                "  if (animId) { cancelAnimationFrame(animId); animId = null; }" +
                "  if (y < EDGE) {" +
                "    (function scroll() {" +
                "      const prev = el.scrollTop;" +
                "      el.scrollTop -= SPEED;" +
                "      if (el.scrollTop !== prev) animId = requestAnimationFrame(scroll);" +
                "    })();" +
                "  } else if (y > h - EDGE) {" +
                "    (function scroll() {" +
                "      const prev = el.scrollTop;" +
                "      el.scrollTop += SPEED;" +
                "      if (el.scrollTop !== prev) animId = requestAnimationFrame(scroll);" +
                "    })();" +
                "  }" +
                "}" +
                "el.addEventListener('dragover', onDragOver);" +
                "el.__bakeryDragOver = onDragOver;" +
                "el.__bakeryAnimId = null;"
        );
    }

    /**
     * Disables auto-scroll, removing the dragover listener installed by
     * {@link #enableDragAutoScroll()}.
     */
    public void disableDragAutoScroll() {
        scroller.getElement().executeJs(
                "if (this.__bakeryDragOver) {" +
                "  this.removeEventListener('dragover', this.__bakeryDragOver);" +
                "  delete this.__bakeryDragOver;" +
                "}" +
                "if (this.__bakeryAnimId) {" +
                "  cancelAnimationFrame(this.__bakeryAnimId);" +
                "  delete this.__bakeryAnimId;" +
                "}"
        );
    }

    // ========== Handler Setters ==========

    /** Sets the handler invoked when a tile is clicked. */
    public void setTileClickHandler(Consumer<BakeryTile> handler) {
        this.tileClickHandler = handler;
    }

    /**
     * Sets the unified drop handler invoked for both status transitions and reordering.
     *
     * @param handler receives the dragged tile, target status, and position
     */
    public void setTileDropHandler(TileDropHandler handler) {
        this.tileDropHandler = handler;
    }

    /** Sets the handler invoked when a drag starts on a tile in this swimlane. */
    public void setDragStartHandler(Consumer<BakeryTile> handler) {
        this.dragStartHandler = handler;
    }

    /** Sets the handler invoked when a drag ends on a tile in this swimlane. */
    public void setDragEndHandler(Runnable handler) {
        this.dragEndHandler = handler;
    }

    // ========== Private: Rendering Helpers ==========

    private void addDateGroup(LocalDate date, List<BakeryTile> tiles) {
        var header = createDateHeaderSpan(date);
        tilesContainer.add(header);

        for (var tile : tiles) {
            var comp = createTileComponent(tile);
            componentsByKey.put(tile.getGroupingKey(), comp);
            tilesContainer.add(comp);
        }
    }

    private void addStatusSection(String sectionTitle, List<BakeryTile> tiles) {
        var header = new Span(sectionTitle);
        header.addClassNames(
                LumoUtility.FontSize.XSMALL,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.Padding.Horizontal.XSMALL,
                LumoUtility.Margin.Top.SMALL
        );
        tilesContainer.add(header);

        tiles.stream()
                .collect(Collectors.groupingBy(
                        BakeryTile::getDueDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .forEach((date, dateTiles) -> {
                    var dateLabel = createDateHeaderSpan(date);
                    dateLabel.removeClassNames(
                            LumoUtility.FontSize.XSMALL,
                            LumoUtility.TextColor.SECONDARY,
                            LumoUtility.FontWeight.SEMIBOLD,
                            LumoUtility.Padding.Horizontal.XSMALL
                    );
                    dateLabel.addClassNames(
                            LumoUtility.FontSize.XXSMALL,
                            LumoUtility.TextColor.SECONDARY,
                            LumoUtility.Padding.Left.SMALL
                    );
                    tilesContainer.add(dateLabel);

                    for (var tile : dateTiles) {
                        var comp = createTileComponent(tile);
                        componentsByKey.put(tile.getGroupingKey(), comp);
                        tilesContainer.add(comp);
                    }
                });
    }

    private BakeryTileComponent createTileComponent(BakeryTile tile) {
        var component = new BakeryTileComponent(tile);
        component.addTileClickListener(e -> {
            if (tileClickHandler != null) {
                tileClickHandler.accept(e.getTile());
            }
        });

        // Add drag listeners for view-level coordination
        var dragSource = DragSource.configure(component);
        dragSource.addDragStartListener(_ -> {
            if (dragStartHandler != null) {
                dragStartHandler.accept(tile);
            }
        });
        dragSource.addDragEndListener(_ -> {
            if (dragEndHandler != null) {
                dragEndHandler.run();
            }
        });

        return component;
    }

    private Span createDateHeaderSpan(LocalDate date) {
        var header = new Span(formatDateLabel(date));
        header.addClassNames(
                LumoUtility.FontSize.XSMALL,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.Padding.Horizontal.XSMALL
        );
        header.getElement().setAttribute("data-date", date.toString());
        return header;
    }

    private String formatDateLabel(LocalDate date) {
        var today = LocalDate.now();
        if (date.equals(today)) {
            return "Today";
        } else if (date.equals(today.plusDays(1))) {
            return "Tomorrow";
        } else if (date.isBefore(today)) {
            return "Overdue - " + DATE_FORMATTER.format(date);
        } else {
            return DATE_FORMATTER.format(date);
        }
    }

    // ========== Private: Drag Mode ==========

    /**
     * Removes overlay panel, reorder zones, and drag CSS classes.
     * Safe to call even when not in drag mode (no-op if nothing to clean up).
     */
    private void cleanUpDragMode() {
        // Remove overlay panel
        if (dropZonePanel != null) {
            body.remove(dropZonePanel);
            dropZonePanel = null;
        }

        // Remove reorder zones
        if (dragModeInsertions != null) {
            for (var comp : dragModeInsertions) {
                tilesContainer.remove(comp);
            }
            dragModeInsertions = null;
            tilesContainer.removeClassName("reorder-active");
        }

        // Remove v-dragged class from all tile components
        componentsByKey.values().forEach(c -> c.removeClassName("v-dragged"));
    }

    /**
     * Creates the overlay panel with status drop zones for this swimlane.
     */
    private Div createDropZonePanel(Set<OrderItemStatus> activeTargets, OrderItemStatus sourceStatus) {
        var panel = new Div();
        panel.addClassName("drop-zone-panel");

        for (var status : statuses) {
            var isActive = activeTargets.contains(status) || status == sourceStatus;
            var weight = flexWeights.getOrDefault(status, 1);

            var zone = new Div();
            zone.addClassName("panel-status-zone");
            zone.addClassName("panel-zone-" + status.getBadgeTheme());
            zone.getStyle().set("flex", weight + " 1 0");

            var label = new Span(status.getDisplayName());
            label.addClassName("panel-status-label");

            if (isActive) {
                zone.addClassName("panel-zone-active");

                var topZone = createPanelSubZone(status, 0, VaadinIcon.ANGLE_DOUBLE_UP);
                var bottomZone = createPanelSubZone(status, Integer.MAX_VALUE, VaadinIcon.ANGLE_DOUBLE_DOWN);

                zone.add(topZone, label, bottomZone);
            } else {
                zone.addClassName("panel-zone-disabled");
                zone.add(label);
            }

            panel.add(zone);
        }

        return panel;
    }

    /**
     * Creates a "Top" or "Bottom" sub-zone within a panel status zone.
     */
    private Div createPanelSubZone(OrderItemStatus targetStatus, int position, VaadinIcon vaadinIcon) {
        var subZone = new Div();
        subZone.addClassName("panel-sub-zone");

        var icon = new Icon(vaadinIcon);
        icon.addClassName("panel-sub-zone-icon");
        subZone.add(icon);

        var dropTarget = DropTarget.create(subZone);
        dropTarget.setActive(true);
        dropTarget.addDropListener(event ->
                event.getDragData().ifPresent(data -> {
                    if (data instanceof BakeryTile tile && tileDropHandler != null) {
                        tileDropHandler.onDrop(tile, targetStatus, position);
                    }
                }));

        return subZone;
    }

    /**
     * Inserts reorder drop zones between tiles of the given statuses.
     */
    private void insertReorderDropZones(BakeryTile draggedTile, Set<OrderItemStatus> reorderStatuses) {
        if (reorderStatuses.isEmpty()) {
            return;
        }

        dragModeInsertions = new ArrayList<>();
        var sourceStatus = draggedTile.getStatus();

        // Build current order per reorderable status
        var orderByStatus = new java.util.HashMap<OrderItemStatus, List<String>>();
        for (var status : reorderStatuses) {
            var order = tilesContainer.getChildren()
                    .filter(c -> c instanceof BakeryTileComponent)
                    .map(c -> ((BakeryTileComponent) c).getTile())
                    .filter(t -> t.getStatus() == status)
                    .map(BakeryTile::getGroupingKey)
                    .toList();
            orderByStatus.put(status, order);
        }

        var draggedKey = draggedTile.getGroupingKey();

        // Switch from flex gap to margin-based spacing so reorder zones add zero space
        tilesContainer.addClassName("reorder-active");

        // Snapshot current children before modification
        var children = tilesContainer.getChildren().toList();

        // Track per-status index for no-op detection
        var statusIndex = new java.util.HashMap<OrderItemStatus, Integer>();
        for (var status : reorderStatuses) {
            statusIndex.put(status, 0);
        }

        int insertOffset = 0;

        for (var i = 0; i < children.size(); i++) {
            var child = children.get(i);
            if (child instanceof BakeryTileComponent tileComp) {
                var tileStatus = tileComp.getTile().getStatus();
                if (reorderStatuses.contains(tileStatus)) {
                    var currentOrder = orderByStatus.get(tileStatus);
                    var idx = statusIndex.get(tileStatus);
                    var isSourceStatus = (tileStatus == sourceStatus);

                    // For source status: skip no-op positions adjacent to dragged tile
                    var draggedIdx = isSourceStatus ? currentOrder.indexOf(draggedKey) : -1;
                    var skipZone = isSourceStatus
                            && (idx == draggedIdx || idx == draggedIdx + 1);

                    if (!skipZone) {
                        var reorderZone = createReorderDropZone(tileStatus, idx, currentOrder,
                                isSourceStatus ? draggedKey : null);
                        tilesContainer.addComponentAtIndex(i + insertOffset, reorderZone);
                        dragModeInsertions.add(reorderZone);
                        insertOffset++;
                    }

                    statusIndex.put(tileStatus, idx + 1);
                }
            }
        }

        // Trailing reorder zones after the last tile of each reorderable status
        for (var status : reorderStatuses) {
            var currentOrder = orderByStatus.get(status);
            if (currentOrder.isEmpty()) {
                continue; // no tiles = no trailing zone (use panel First/Last instead)
            }
            var idx = statusIndex.get(status);
            var isSourceStatus = (status == sourceStatus);
            var draggedIdx = isSourceStatus ? currentOrder.indexOf(draggedKey) : -1;
            var skipZone = isSourceStatus && (idx == draggedIdx + 1);

            if (!skipZone) {
                var reorderZone = createReorderDropZone(status, idx, currentOrder,
                        isSourceStatus ? draggedKey : null);
                // Insert after the last tile of this status
                var lastTileIndex = findLastTileIndex(status);
                if (lastTileIndex >= 0) {
                    tilesContainer.addComponentAtIndex(lastTileIndex + 1, reorderZone);
                } else {
                    tilesContainer.add(reorderZone);
                }
                dragModeInsertions.add(reorderZone);
            }
        }
    }

    /**
     * Finds the index of the last tile component with the given status in the tilesContainer.
     */
    private int findLastTileIndex(OrderItemStatus status) {
        var children = tilesContainer.getChildren().toList();
        int lastIndex = -1;
        for (var i = 0; i < children.size(); i++) {
            if (children.get(i) instanceof BakeryTileComponent tileComp
                    && tileComp.getTile().getStatus() == status) {
                lastIndex = i;
            }
        }
        return lastIndex;
    }

    /**
     * Creates a reorder drop zone that invokes the unified drop handler with a computed position.
     */
    private Div createReorderDropZone(OrderItemStatus targetStatus, int targetIndex,
                                       List<String> currentOrder, String draggedKey) {
        var dropZone = new Div();
        dropZone.addClassName("reorder-drop-zone");

        var dropTarget = DropTarget.create(dropZone);
        dropTarget.setActive(true);
        dropTarget.addDropListener(event ->
                event.getDragData().ifPresent(data -> {
                    if (data instanceof BakeryTile tile && tileDropHandler != null) {
                        int position;
                        if (draggedKey != null) {
                            // Same status: compute actual position after removal
                            var draggedIdx = currentOrder.indexOf(draggedKey);
                            position = (draggedIdx >= 0 && draggedIdx < targetIndex)
                                    ? targetIndex - 1
                                    : targetIndex;
                        } else {
                            // Different status: direct index
                            position = targetIndex;
                        }
                        tileDropHandler.onDrop(tile, targetStatus, position);
                    }
                }));

        return dropZone;
    }

    /**
     * Unified handler for tile drops — covers both status transitions and reordering.
     */
    @FunctionalInterface
    public interface TileDropHandler {

        /**
         * Called when a tile is dropped on a status zone or reorder position.
         *
         * @param tile         the tile that was dropped
         * @param targetStatus the target status (may be same as current for reorder)
         * @param position     the target position (0 = first, Integer.MAX_VALUE = last,
         *                     or a specific index for reorder zones)
         */
        void onDrop(BakeryTile tile, OrderItemStatus targetStatus, int position);
    }
}
