package org.vaadin.bakery.ui.view.bakery;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.uimodel.data.BakeryTile;

/**
 * Card component representing a single bakery board tile.
 * Supports drag-and-drop for status transitions.
 */
public class BakeryTileComponent extends Composite<Div> {

    private final BakeryTile tile;

    /**
     * Creates a tile component for the given bakery tile model.
     *
     * @param tile the bakery tile data to display
     */
    public BakeryTileComponent(BakeryTile tile) {
        this.tile = tile;

        var content = getContent();
        content.addClassName("bakery-tile");
        content.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.XSMALL
        );

        // Configure as drag source
        var dragSource = DragSource.configure(this);
        dragSource.setDraggable(true);
        dragSource.setDragData(tile);

        // Header: product name + indicators
        var header = new Div();
        header.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.AlignItems.CENTER
        );

        var productName = new Span(tile.getProductName());
        productName.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        header.add(productName);

        var indicators = new Div();
        indicators.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.XSMALL
        );

        if (tile.isOnHold()) {
            var holdIcon = new Icon(VaadinIcon.LOCK);
            holdIcon.setSize("14px");
            holdIcon.setColor("var(--lumo-error-text-color)");
            holdIcon.getElement().setAttribute("title", "On hold — resolve rejected sibling items first");
            indicators.add(holdIcon);
        }
        if (tile.isHasUnreadMessages()) {
            var unreadDot = new Span();
            unreadDot.addClassName("unread-dot");
            indicators.add(unreadDot);
        }
        if (tile.isHasNotes()) {
            var notesIcon = new Icon(VaadinIcon.COMMENT_ELLIPSIS_O);
            notesIcon.setSize("14px");
            notesIcon.setColor("var(--lumo-secondary-text-color)");
            indicators.add(notesIcon);
        }
        header.add(indicators);
        content.add(header);

        // Quantity and size
        var quantityLine = new Span(tile.getTotalQuantity() + "x"
                + (tile.getProductSize() != null ? " " + tile.getProductSize() : ""));
        quantityLine.addClassNames(LumoUtility.FontSize.SMALL);
        content.add(quantityLine);

        // Order count (for batchable) or order reference (for non-batchable)
        if (tile.isBatchable() && tile.getOrderCount() > 1) {
            var orderCount = new Span(tile.getOrderCount() + " orders");
            orderCount.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);
            content.add(orderCount);
        } else if (!tile.isBatchable() && tile.getOrderReference() != null) {
            var orderRef = new Span(tile.getOrderReference());
            orderRef.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.SECONDARY);
            content.add(orderRef);
        }

        // Item details for non-batchable
        if (!tile.isBatchable() && tile.getItemDetails() != null && !tile.getItemDetails().isBlank()) {
            var details = new Span(tile.getItemDetails());
            details.addClassNames(
                    LumoUtility.FontSize.XSMALL,
                    LumoUtility.TextColor.SECONDARY,
                    LumoUtility.Whitespace.NOWRAP,
                    LumoUtility.Overflow.HIDDEN
            );
            details.getStyle().set("text-overflow", "ellipsis");
            content.add(details);
        }

        // Click handler
        content.addClickListener(_ -> fireEvent(new TileClickEvent(this, tile)));
    }

    /** Returns the tile model for this component. */
    public BakeryTile getTile() {
        return tile;
    }

    /** Registers a listener for tile click events. */
    public Registration addTileClickListener(ComponentEventListener<TileClickEvent> listener) {
        return addListener(TileClickEvent.class, listener);
    }

    /** Event fired when the tile is clicked. */
    public static class TileClickEvent extends ComponentEvent<BakeryTileComponent> {
        private final BakeryTile tile;

        public TileClickEvent(BakeryTileComponent source, BakeryTile tile) {
            super(source, false);
            this.tile = tile;
        }

        /** Returns the tile that was clicked. */
        public BakeryTile getTile() {
            return tile;
        }
    }
}
