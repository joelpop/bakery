package org.vaadin.bakery.ui.view.bakery;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.dnd.DropEvent;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.uimodel.data.BakeryTile;
import org.vaadin.bakery.uimodel.type.OrderItemStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A single swimlane column in the bakery board.
 * Displays tiles filtered by one or more item statuses, grouped by date.
 */
public class BakerySwimlane extends Composite<Div> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE, MMM d");

    private final String title;
    private final List<OrderItemStatus> statuses;
    private final Span countBadge;
    private final Div tilesContainer;
    private Consumer<BakeryTile> tileClickHandler;
    private BiConsumer<BakeryTile, OrderItemStatus> dropHandler;

    /**
     * Creates a swimlane for the given statuses.
     *
     * @param title    the swimlane header title
     * @param statuses the item statuses this swimlane shows
     */
    public BakerySwimlane(String title, List<OrderItemStatus> statuses) {
        this.title = title;
        this.statuses = statuses;

        var root = getContent();
        root.addClassName("bakery-swimlane");
        root.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN
        );

        // Header
        var header = new Div();
        header.addClassName("swimlane-header");
        header.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Vertical.SMALL
        );

        var titleSpan = new H3(title);
        titleSpan.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.SMALL);

        countBadge = new Span("0");
        countBadge.getElement().getThemeList().add("badge small contrast");

        header.add(titleSpan, countBadge);
        root.add(header);

        // Scrollable tiles area
        tilesContainer = new Div();
        tilesContainer.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL,
                LumoUtility.Padding.SMALL
        );

        // Configure as drop target
        var dropTarget = DropTarget.create(tilesContainer);
        dropTarget.setActive(true);
        dropTarget.addDropListener(this::onDrop);

        var scroller = new Scroller(tilesContainer);
        scroller.setSizeFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        root.add(scroller);
    }

    /**
     * Sets the tiles to display in this swimlane.
     *
     * @param allTiles all tiles from the board; this method filters to relevant statuses
     */
    public void setTiles(List<BakeryTile> allTiles) {
        tilesContainer.removeAll();

        var myTiles = allTiles.stream()
                .filter(t -> statuses.contains(t.getStatus()))
                .toList();

        countBadge.setText(String.valueOf(myTiles.size()));

        // Group by date
        Map<LocalDate, List<BakeryTile>> byDate = myTiles.stream()
                .collect(Collectors.groupingBy(
                        BakeryTile::getDueDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // For multi-status swimlanes, group by status within date
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
            byDate.forEach(this::addDateGroup);
        }
    }

    /** Sets the handler invoked when a tile is clicked. */
    public void setTileClickHandler(Consumer<BakeryTile> handler) {
        this.tileClickHandler = handler;
    }

    /** Sets the handler invoked when a tile is dropped onto this swimlane. */
    public void setDropHandler(BiConsumer<BakeryTile, OrderItemStatus> handler) {
        this.dropHandler = handler;
    }

    /** Returns the primary status this swimlane represents. */
    public OrderItemStatus getPrimaryStatus() {
        return statuses.getFirst();
    }

    private void addDateGroup(LocalDate date, List<BakeryTile> tiles) {
        var dateLabel = formatDateLabel(date);
        var header = new Span(dateLabel);
        header.addClassNames(
                LumoUtility.FontSize.XSMALL,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.Padding.Horizontal.XSMALL
        );
        tilesContainer.add(header);

        for (var tile : tiles) {
            tilesContainer.add(createTileComponent(tile));
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

        // Sub-group by date within status section
        Map<LocalDate, List<BakeryTile>> byDate = tiles.stream()
                .collect(Collectors.groupingBy(
                        BakeryTile::getDueDate,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        byDate.forEach((date, dateTiles) -> {
            var dateLabel = new Span(formatDateLabel(date));
            dateLabel.addClassNames(
                    LumoUtility.FontSize.XXSMALL,
                    LumoUtility.TextColor.SECONDARY,
                    LumoUtility.Padding.Left.SMALL
            );
            tilesContainer.add(dateLabel);

            for (var tile : dateTiles) {
                tilesContainer.add(createTileComponent(tile));
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
        return component;
    }

    private void onDrop(DropEvent<Div> event) {
        event.getDragData().ifPresent(data -> {
            if (data instanceof BakeryTile draggedTile && dropHandler != null) {
                // Determine target status: for multi-status swimlanes, use primary
                var targetStatus = statuses.getFirst();
                dropHandler.accept(draggedTile, targetStatus);
            }
        });
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
}
