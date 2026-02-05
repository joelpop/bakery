package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.service.UserLocationService;
import org.vaadin.bakery.uimodel.data.LocationSummary;
import org.vaadin.bakery.uimodel.type.OrderStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Filter bar for the storefront view.
 */
public class FilterBar extends Div {

    /**
     * Sentinel ID for the "Current Location" option.
     */
    public static final Long CURRENT_LOCATION_ID = -1L;

    private final DatePicker fromDate;
    private final DatePicker toDate;
    private final MultiSelectComboBox<OrderStatus> statusFilter;
    private final ComboBox<LocationSummary> locationFilter;
    private final UserLocationService userLocationService;
    private final LocationSummary currentLocationSentinel;

    public FilterBar(List<LocationSummary> locations, UserLocationService userLocationService) {
        this.userLocationService = userLocationService;

        // Create sentinel for "Current Location" option
        currentLocationSentinel = new LocationSummary();
        currentLocationSentinel.setId(CURRENT_LOCATION_ID);
        currentLocationSentinel.setName("Current Location");
        addClassName("filter-bar");
        addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexWrap.WRAP,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.AlignItems.END,
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.Padding.Horizontal.LARGE
        );
        getStyle().set("background", "var(--lumo-contrast-5pct)");
        getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");

        // Date range
        fromDate = new DatePicker("From");
        fromDate.setValue(LocalDate.now());
        fromDate.setWidth("140px");
        fromDate.addValueChangeListener(e -> fireFilterChanged());

        toDate = new DatePicker("To");
        toDate.setValue(LocalDate.now().plusDays(7));
        toDate.setWidth("140px");
        toDate.addValueChangeListener(e -> fireFilterChanged());

        // Status filter
        statusFilter = new MultiSelectComboBox<>("Status");
        statusFilter.setItems(OrderStatus.values());
        statusFilter.setItemLabelGenerator(OrderStatus::getDisplayName);
        statusFilter.setWidth("200px");
        statusFilter.setPlaceholder("All statuses");
        statusFilter.addValueChangeListener(e -> fireFilterChanged());

        // Location filter with "Current Location" sentinel at the top
        locationFilter = new ComboBox<>("Location");
        var locationItems = new ArrayList<LocationSummary>();
        locationItems.add(currentLocationSentinel);
        locationItems.addAll(locations);
        locationFilter.setItems(locationItems);
        locationFilter.setItemLabelGenerator(LocationSummary::getName);
        locationFilter.setWidth("180px");
        locationFilter.setPlaceholder("All locations");
        locationFilter.setClearButtonVisible(true);
        locationFilter.addValueChangeListener(e -> fireFilterChanged());

        add(fromDate, toDate, statusFilter, locationFilter);
    }

    private void fireFilterChanged() {
        fireEvent(new FilterChangedEvent(this));
    }

    public LocalDate getFromDate() {
        return fromDate.getValue();
    }

    public void setFromDate(LocalDate date) {
        fromDate.setValue(date);
    }

    public LocalDate getToDate() {
        return toDate.getValue();
    }

    public void setToDate(LocalDate date) {
        toDate.setValue(date);
    }

    public Set<OrderStatus> getSelectedStatuses() {
        return statusFilter.getValue();
    }

    /**
     * Returns the selected location. If "Current Location" is selected,
     * returns the actual current location from UserLocationService.
     */
    public LocationSummary getSelectedLocation() {
        var selected = locationFilter.getValue();
        if (selected != null && CURRENT_LOCATION_ID.equals(selected.getId())) {
            return userLocationService.getCurrentLocation();
        }
        return selected;
    }

    /**
     * Checks if "Current Location" is currently selected in the filter.
     */
    public boolean isCurrentLocationSelected() {
        var selected = locationFilter.getValue();
        return selected != null && CURRENT_LOCATION_ID.equals(selected.getId());
    }

    // Event for filter changes
    public static class FilterChangedEvent extends ComponentEvent<FilterBar> {
        public FilterChangedEvent(FilterBar source) {
            super(source, false);
        }
    }

    public Registration addFilterChangedListener(ComponentEventListener<FilterChangedEvent> listener) {
        return addListener(FilterChangedEvent.class, listener);
    }
}
