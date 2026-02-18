package org.vaadin.bakery.ui.view.locations;

import com.vaadin.flow.signals.impl.Effect;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.ui.component.ChangeTracker;
import org.vaadin.bakery.ui.component.ViewHeader;
import org.vaadin.bakery.ui.event.DataChangeSignals;
import org.vaadin.bakery.uimodel.data.LocationSummary;
import org.vaadin.bakery.uimodel.type.UserRole;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

/**
 * Location management view (Admin only).
 * Displays a grid of pickup locations with CRUD operations.
 */
@Route(LocationsView.ROUTE)
@PageTitle("Locations")
@Menu(order = 3, icon = LineAwesomeIconUrl.MAP_MARKER_SOLID)
@RolesAllowed(UserRole.ROLE_ADMIN)
public class LocationsView extends VerticalLayout {

    /** Route path for this view. */
    public static final String ROUTE = "locations";

    private final transient LocationService locationService;
    private final Grid<LocationSummary> grid;

    // Signal incremented to trigger a same-session data refresh (e.g., after dialog save or delete)
    private final transient ValueSignal<Integer> refreshTriggerSignal;

    // Tracks version changes between refreshes to identify new and modified locations for row highlight
    private final transient ChangeTracker<LocationSummary> changeTracker;

    /** Creates the locations management view with a grid of pickup locations. */
    public LocationsView(LocationService locationService) {
        this.locationService = locationService;

        // Component initializations
        addClassName("locations-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        var header = new ViewHeader("Locations")
                .withAction("New location", () -> openDialog(new LocationSummary()));

        var gridContainer = new Div();
        gridContainer.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.BoxSizing.BORDER);
        gridContainer.setSizeFull();

        grid = new Grid<>(LocationSummary.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();
        grid.addColumn(LocationSummary::getName)
                .setHeader("Name")
                .setSortable(true)
                .setFlexGrow(2);
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

        // Signal definitions
        refreshTriggerSignal = new ValueSignal<>(0);
        changeTracker = new ChangeTracker<>();

        // Signal bindings - apply "row-highlight" CSS part to changed rows for animated highlight
        grid.setPartNameGenerator(location -> changeTracker.isHighlighted(location.getId()) ? "row-highlight" : null);

        // Reactive effect: re-fetches and rebuilds the grid whenever location data changes
        // in any session (via shared locationVersion signal) or locally (via refreshTriggerSignal)
        Effect.effect(this, () -> {
            DataChangeSignals.locationVersion().get();
            refreshTriggerSignal.get();
            refreshGrid();
        });

        // Layout assembly
        gridContainer.add(grid);
        add(header, gridContainer);
        setFlexGrow(1, gridContainer);
    }

    private void openDialog(LocationSummary location) {
        var dialog = new LocationDialog(location, locationService);
        dialog.addSaveListener(_ -> triggerRefresh());
        dialog.addDeleteListener(_ -> triggerRefresh());
        dialog.open();
    }

    private void triggerRefresh() {
        refreshTriggerSignal.update(v -> v + 1);
    }

    private void refreshGrid() {
        var newData = locationService.list();
        changeTracker.detectChanges(newData);
        grid.setItems(newData);
    }
}
