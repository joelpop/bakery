package org.vaadin.bakery.ui.view.locations;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.uimodel.data.LocationSummary;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

/**
 * Location management view (Admin only).
 * Displays a grid of pickup locations with CRUD operations.
 */
@Route("locations")
@PageTitle("Locations")
@Menu(order = 3, icon = LineAwesomeIconUrl.MAP_MARKER_SOLID)
@RolesAllowed("ADMIN")
public class LocationsView extends VerticalLayout {

    private final LocationService locationService;
    private final Grid<LocationSummary> grid;

    public LocationsView(LocationService locationService) {
        this.locationService = locationService;

        addClassName("locations-view");
        setSizeFull();

        // Header with title and add button
        var header = createHeader();

        // Grid
        grid = createGrid();

        add(header, grid);
        refreshGrid();
    }

    private HorizontalLayout createHeader() {
        var header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.addClassNames(LumoUtility.Padding.Horizontal.MEDIUM);

        var title = new Span("Locations");
        title.addClassNames(
                LumoUtility.FontSize.XLARGE,
                LumoUtility.FontWeight.SEMIBOLD
        );

        var addButton = new Button("New location", new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> openDialog(new LocationSummary()));

        var spacer = new Span();
        spacer.addClassNames(LumoUtility.Flex.GROW);

        header.add(title, spacer, addButton);
        return header;
    }

    private Grid<LocationSummary> createGrid() {
        var grid = new Grid<>(LocationSummary.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        grid.addColumn(LocationSummary::getName)
                .setHeader("Name")
                .setSortable(true)
                .setFlexGrow(2);

        grid.addColumn(LocationSummary::getCode)
                .setHeader("Code")
                .setSortable(true)
                .setFlexGrow(1);

        grid.addColumn(LocationSummary::getAddress)
                .setHeader("Address")
                .setSortable(true)
                .setFlexGrow(2);

        grid.addComponentColumn(location -> {
            var badge = new Span(location.isActive() ? "Active" : "Inactive");
            badge.getElement().getThemeList().add("badge " + (location.isActive() ? "success" : "contrast"));
            return badge;
        }).setHeader("Status").setFlexGrow(0).setAutoWidth(true);

        grid.addColumn(LocationSummary::getSortOrder)
                .setHeader("Sort Order")
                .setSortable(true)
                .setFlexGrow(0)
                .setAutoWidth(true);

        grid.addItemClickListener(event -> openDialog(event.getItem()));

        return grid;
    }

    private void openDialog(LocationSummary location) {
        var dialog = new LocationDialog(location, locationService);
        dialog.addSaveListener(e -> refreshGrid());
        dialog.addDeleteListener(e -> refreshGrid());
        dialog.open();
    }

    private void refreshGrid() {
        grid.setItems(locationService.list());
    }
}
