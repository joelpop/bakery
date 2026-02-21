package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;
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
public class FilterBar extends Composite<Div> implements HasSize, HasStyle {

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
    private final ValueSignal<LocalDate> fromDateSignal;
    private final ValueSignal<LocalDate> toDateSignal;
    private final ValueSignal<Set<OrderStatus>> selectedStatusesSignal;
    private final ValueSignal<LocationSummary> selectedLocationSignal;

    // Signals - computed/derived
    private final Signal<LocationSummary> resolvedLocationSignal;

    /**
     * Creates the storefront filter bar with date, status, and location filters.
     *
     * @param locations           the available locations for the location filter
     * @param userLocationService service for resolving the "Current Location" sentinel
     */
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
        locationFilterComboBox.setWidth("220px");
        locationFilterComboBox.setClearButtonVisible(true);

        // Signal definitions
        var today = LocalDate.now();
        var weekFromNow = today.plusDays(7);

        fromDateSignal = new ValueSignal<>(today);
        toDateSignal = new ValueSignal<>(weekFromNow);
        selectedStatusesSignal = new ValueSignal<>(Set.of());
        selectedLocationSignal = new ValueSignal<>(currentLocationSentinel);

        resolvedLocationSignal = Signal.computed(() -> {
            var selected = selectedLocationSignal.get();
            if (selected != null && CURRENT_LOCATION_ID.equals(selected.getId())) {
                return userLocationService.getCurrentLocation().orElse(null);
            }
            return selected;
        });

        // Signal bindings (UI → Signal)
        fromDatePicker.addValueChangeListener(this::onFromDatePickerValueChanged);
        toDatePicker.addValueChangeListener(this::onToDatePickerValueChanged);
        statusFilterComboBox.addValueChangeListener(this::onStatusFilterComboBoxValueChanged);
        locationFilterComboBox.addValueChangeListener(this::onLocationFilterComboBoxValueChanged);

        // Value settings (initial values)
        fromDatePicker.setValue(today);
        toDatePicker.setValue(weekFromNow);
        locationFilterComboBox.setValue(currentLocationSentinel);

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

    private void onFromDatePickerValueChanged(
            ComponentValueChangeEvent<DatePicker, LocalDate> event) {
        fromDateSignal.set(event.getValue());
        fireFilterChanged();
    }

    private void onToDatePickerValueChanged(
            ComponentValueChangeEvent<DatePicker, LocalDate> event) {
        toDateSignal.set(event.getValue());
        fireFilterChanged();
    }

    private void onStatusFilterComboBoxValueChanged(
            ComponentValueChangeEvent<MultiSelectComboBox<OrderStatus>, Set<OrderStatus>> event) {
        selectedStatusesSignal.set(event.getValue());
        fireFilterChanged();
    }

    private void onLocationFilterComboBoxValueChanged(
            ComponentValueChangeEvent<ComboBox<LocationSummary>, LocationSummary> event) {
        selectedLocationSignal.set(event.getValue());
        fireFilterChanged();
    }

    private void fireFilterChanged() {
        fireEvent(new FilterChangedEvent(this));
    }

    public LocalDate getFromDate() {
        return fromDateSignal.get();
    }

    public void setFromDate(LocalDate date) {
        fromDateSignal.set(date);
        fromDatePicker.setValue(date);
    }

    public LocalDate getToDate() {
        return toDateSignal.get();
    }

    public void setToDate(LocalDate date) {
        toDateSignal.set(date);
        toDatePicker.setValue(date);
    }

    public Set<OrderStatus> getSelectedStatuses() {
        return selectedStatusesSignal.get();
    }

    /**
     * Returns the selected location. If "Current Location" is selected,
     * returns the actual current location from UserLocationService.
     */
    public LocationSummary getSelectedLocation() {
        return resolvedLocationSignal.get();
    }

    /**
     * Checks if "Current Location" is currently selected in the filter.
     */
    public boolean isCurrentLocationSelected() {
        var selected = selectedLocationSignal.get();
        return selected != null && CURRENT_LOCATION_ID.equals(selected.getId());
    }

    /** Event fired when any filter value changes. */
    public static class FilterChangedEvent extends ComponentEvent<FilterBar> {
        public FilterChangedEvent(FilterBar source) {
            super(source, false);
        }
    }

    /** Registers a listener for filter change events. */
    public Registration addFilterChangedListener(ComponentEventListener<FilterChangedEvent> listener) {
        return addListener(FilterChangedEvent.class, listener);
    }
}
