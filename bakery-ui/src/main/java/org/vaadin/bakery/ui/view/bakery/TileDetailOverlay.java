package org.vaadin.bakery.ui.view.bakery;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.service.BakeryService;
import org.vaadin.bakery.uimodel.data.BakeryTile;
import org.vaadin.bakery.uimodel.data.BakeryTileDetail;
import org.vaadin.bakery.ui.event.NonComponent;
import org.vaadin.bakery.ui.event.NonComponentEvent;
import org.vaadin.bakery.ui.event.NonComponentEventSupport;
import org.vaadin.bakery.ui.view.storefront.OrderDetailView;

import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog overlay showing the contributing order items for a bakery tile.
 * Uses delegation pattern rather than extending Dialog.
 */
public class TileDetailOverlay implements NonComponent {

    private final Dialog dialog;
    private final NonComponentEventSupport<TileDetailOverlay> eventSupport;

    /**
     * Creates the tile detail overlay for the given tile.
     *
     * @param tile           the tile to show details for
     * @param bakeryService  the bakery service for fetching tile details
     */
    public TileDetailOverlay(BakeryTile tile, BakeryService bakeryService) {
        dialog = new Dialog();
        eventSupport = new NonComponentEventSupport<>();

        dialog.setHeaderTitle(tile.getProductName() + " - " + tile.getStatus().getDisplayName());
        dialog.setWidth("500px");
        dialog.setMaxHeight("80vh");

        // Tile summary
        var summary = new Div();
        summary.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.Padding.Bottom.MEDIUM,
                LumoUtility.Border.BOTTOM
        );

        var qtyBadge = new Span(tile.getTotalQuantity() + "x");
        qtyBadge.getElement().getThemeList().add("badge");
        summary.add(qtyBadge);

        if (tile.getProductSize() != null) {
            var sizeBadge = new Span(tile.getProductSize());
            sizeBadge.getElement().getThemeList().add("badge contrast");
            summary.add(sizeBadge);
        }

        var orderCountSpan = new Span(tile.getOrderCount() + " order" + (tile.getOrderCount() != 1 ? "s" : ""));
        orderCountSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
        summary.add(orderCountSpan);

        dialog.add(summary);

        // Contributing items
        var details = bakeryService.getTileDetails(tile.getGroupingKey());
        if (!details.isEmpty()) {
            var itemsHeader = new H4("Contributing Orders");
            itemsHeader.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.Margin.Bottom.SMALL);
            dialog.add(itemsHeader);

            for (var detail : details) {
                dialog.add(createDetailCard(detail));
            }
        }

        // Undo button
        if (tile.isUndoAvailable()) {
            var undoButton = new Button("Undo Last Transition", VaadinIcon.BACKWARDS.create());
            undoButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            undoButton.addClickListener(_ -> {
                dialog.close();
                fireEvent(new UndoEvent(this, tile));
            });
            dialog.getFooter().add(undoButton);
        }

        var closeButton = new Button("Close", _ -> dialog.close());
        dialog.getFooter().add(closeButton);
    }

    /** Opens the dialog. */
    public void open() {
        dialog.open();
    }

    /** Closes the dialog. */
    public void close() {
        dialog.close();
    }

    @Override
    public <E extends NonComponentEvent<?>> Registration addListener(Class<E> eventType, Consumer<E> listener) {
        return eventSupport.addListener((Class<NonComponentEvent<TileDetailOverlay>>) eventType,
                (Consumer<NonComponentEvent<TileDetailOverlay>>) listener);
    }

    /** Adds a listener for undo events. */
    public Registration addUndoListener(Consumer<UndoEvent> listener) {
        return eventSupport.addListener(UndoEvent.class, listener);
    }

    private void fireEvent(NonComponentEvent<TileDetailOverlay> event) {
        eventSupport.fireEvent(event);
    }

    private Div createDetailCard(BakeryTileDetail detail) {
        var card = new Div();
        card.addClassName("card");
        card.addClassNames(
                LumoUtility.Margin.Bottom.SMALL,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.XSMALL
        );

        // Customer and quantity
        var topRow = new HorizontalLayout();
        topRow.setWidthFull();
        topRow.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);

        var customerSpan = new Span(detail.getCustomerName());
        customerSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD);

        var qtySpan = new Span(detail.getQuantity() + "x");
        qtySpan.getElement().getThemeList().add("badge small");

        topRow.add(customerSpan, qtySpan);
        card.add(topRow);

        // Item details
        if (detail.getItemDetails() != null && !detail.getItemDetails().isBlank()) {
            var detailsSpan = new Span(detail.getItemDetails());
            detailsSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
            card.add(detailsSpan);
        }

        // Additional details
        if (detail.getAdditionalDetails() != null && !detail.getAdditionalDetails().isBlank()) {
            var additionalSpan = new Span(detail.getAdditionalDetails());
            additionalSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
            card.add(additionalSpan);
        }

        // Unread indicator
        if (detail.isHasUnreadMessages()) {
            var unreadSpan = new Span("Has unread messages");
            unreadSpan.addClassNames(LumoUtility.FontSize.XSMALL, LumoUtility.TextColor.PRIMARY);
            card.add(unreadSpan);
        }

        // Link to order detail
        var viewOrderButton = new Button("View Order #" + detail.getOrderId(),
                VaadinIcon.EXTERNAL_LINK.create());
        viewOrderButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        viewOrderButton.addClickListener(_ ->
                viewOrderButton.getUI().ifPresent(ui ->
                        ui.navigate(OrderDetailView.class,
                                new RouteParameters("orderId", detail.getOrderId().toString()))));
        card.add(viewOrderButton);

        return card;
    }

    /** Event fired when the user clicks the undo button. */
    public static class UndoEvent extends NonComponentEvent<TileDetailOverlay> {
        private final BakeryTile tile;

        public UndoEvent(TileDetailOverlay source, BakeryTile tile) {
            super(source);
            this.tile = tile;
        }

        /** Returns the tile whose transition should be undone. */
        public BakeryTile getTile() {
            return tile;
        }
    }
}
