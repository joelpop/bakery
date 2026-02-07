package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.signals.Signal;
import com.vaadin.signals.local.ValueSignal;
import org.vaadin.bakery.service.UserLocationService;
import org.vaadin.bakery.uimodel.data.LocationSummary;
import org.vaadin.bakery.uimodel.type.OrderStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Filter bar for the storefront view.
 * Uses Vaadin Signals for reactive filter state management.
 */
public class FilterBar extends Composite<Div> {

    /**
     * Sentinel ID for the "Current Location" option.
     */
    public static final Long CURRENT_LOCATION_ID = -1L;

    // UI Components
    private final DatePicker fromDatePicker;
    private final DatePicker toDatePicker;
    private final MultiSelectComboBox<OrderStatus> statusFilterComboBox;
    private final ComboBox<LocationSummary> locationFilterComboBox;

   // Signals - primary state
    private final transient ValueSignal<LocalDate> fromDateSignal;
    private final transient ValueSignal<LocalDate> toDateSignal;
    private final transient ValueSignal<Set<OrderStatus>> selectedStatusesSignal;
    private final transient ValueSignal<LocationSummary> selectedLocationSignal;

    // Signals - computed/derived
    private final transient Signal<LocationSummary> resolvedLocationSignal;

    public FilterBar(List<LocationSummary> locations, UserLocationService userLocationService) {
       // Services

       // Component initializations
        fromDatePicker = new DatePicker("From");
        fromDatePicker.setWidth("140px");

        toDatePicker = new DatePicker("To");
        toDatePicker.setWidth("140px");

        statusFilterComboBox = new MultiSelectComboBox<>("Status");
        statusFilterComboBox.setItems(OrderStatus.values());
        statusFilterComboBox.setItemLabelGenerator(OrderStatus::getDisplayName);
        statusFilterComboBox.setWidth("200px");
        statusFilterComboBox.setPlaceholder("All statuses");

        // Create sentinel for "Current Location" option
        var currentLocationSentinel = new LocationSummary();
        currentLocationSentinel.setId(CURRENT_LOCATION_ID);
        currentLocationSentinel.setName("Current Location");

        var locationItems = new ArrayList<LocationSummary>();
        locationItems.add(currentLocationSentinel);
        locationItems.addAll(locations);

        locationFilterComboBox = new ComboBox<>("Location");
        locationFilterComboBox.setItems(locationItems);
        locationFilterComboBox.setItemLabelGenerator(LocationSummary::getName);
        locationFilterComboBox.setWidth("180px");
        locationFilterComboBox.setPlaceholder("All locations");
        locationFilterComboBox.setClearButtonVisible(true);

        // Signal definitions
        var today = LocalDate.now();
        var weekFromNow = today.plusDays(7);

        fromDateSignal = new ValueSignal<>(today);
        toDateSignal = new ValueSignal<>(weekFromNow);
        selectedStatusesSignal = new ValueSignal<>(Set.of());
        selectedLocationSignal = new ValueSignal<>(null);

        resolvedLocationSignal = Signal.computed(() -> {
            var selected = selectedLocationSignal.value();
            if (selected != null && CURRENT_LOCATION_ID.equals(selected.getId())) {
                return userLocationService.getCurrentLocation();
            }
            return selected;
        });

        // Signal bindings (UI → Signal)
        fromDatePicker.addValueChangeListener(_ -> {
            fromDateSignal.value(fromDatePicker.getValue());
            fireFilterChanged();
        });

        toDatePicker.addValueChangeListener(_ -> {
            toDateSignal.value(toDatePicker.getValue());
            fireFilterChanged();
        });

        statusFilterComboBox.addValueChangeListener(_ -> {
            selectedStatusesSignal.value(statusFilterComboBox.getValue());
            fireFilterChanged();
        });

        locationFilterComboBox.addValueChangeListener(_ -> {
            selectedLocationSignal.value(locationFilterComboBox.getValue());
            fireFilterChanged();
        });

        // Value settings (initial values)
        fromDatePicker.setValue(today);
        toDatePicker.setValue(weekFromNow);

        // Layout assembly
        var content = getContent();
        content.addClassName("filter-bar");
        content.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexWrap.WRAP,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.AlignItems.END,
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.Padding.Horizontal.LARGE
        );
        content.getStyle().set("background", "var(--lumo-contrast-5pct)");
        content.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-10pct)");
        content.add(fromDatePicker, toDatePicker, statusFilterComboBox, locationFilterComboBox);
    }

    private void fireFilterChanged() {
        fireEvent(new FilterChangedEvent(this));
    }

    public LocalDate getFromDate() {
        return fromDateSignal.value();
    }

    public void setFromDate(LocalDate date) {
        fromDateSignal.value(date);
        fromDatePicker.setValue(date);
    }

    public LocalDate getToDate() {
        return toDateSignal.value();
    }

    public void setToDate(LocalDate date) {
        toDateSignal.value(date);
        toDatePicker.setValue(date);
    }

    public Set<OrderStatus> getSelectedStatuses() {
        return selectedStatusesSignal.value();
    }

    /**
     * Returns the selected location. If "Current Location" is selected,
     * returns the actual current location from UserLocationService.
     */
    public LocationSummary getSelectedLocation() {
        return resolvedLocationSignal.value();
    }

    /**
     * Checks if "Current Location" is currently selected in the filter.
     */
    public boolean isCurrentLocationSelected() {
        var selected = selectedLocationSignal.value();
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
