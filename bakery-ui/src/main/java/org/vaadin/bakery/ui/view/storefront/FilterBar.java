package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.shared.Registration;
import org.vaadin.bakery.uimodel.data.LocationSummary;
import org.vaadin.bakery.uimodel.type.OrderStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Filter bar for the storefront view.
 */
public class FilterBar extends HorizontalLayout {

    private final DatePicker fromDate;
    private final DatePicker toDate;
    private final MultiSelectComboBox<OrderStatus> statusFilter;
    private final ComboBox<LocationSummary> locationFilter;

    public FilterBar(List<LocationSummary> locations) {
        addClassName("filter-bar");
        setWidthFull();
        setAlignItems(Alignment.END);
        setSpacing(true);

        // Date range
        fromDate = new DatePicker("From");
        fromDate.setValue(LocalDate.now());
        fromDate.setWidth("150px");
        fromDate.addValueChangeListener(e -> fireFilterChanged());

        toDate = new DatePicker("To");
        toDate.setValue(LocalDate.now().plusDays(7));
        toDate.setWidth("150px");
        toDate.addValueChangeListener(e -> fireFilterChanged());

        // Status filter
        statusFilter = new MultiSelectComboBox<>("Status");
        statusFilter.setItems(OrderStatus.values());
        statusFilter.setItemLabelGenerator(OrderStatus::getDisplayName);
        statusFilter.setWidth("250px");
        statusFilter.setPlaceholder("All statuses");
        statusFilter.addValueChangeListener(e -> fireFilterChanged());

        // Location filter
        locationFilter = new ComboBox<>("Location");
        locationFilter.setItems(locations);
        locationFilter.setItemLabelGenerator(LocationSummary::getName);
        locationFilter.setWidth("200px");
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

    public LocationSummary getSelectedLocation() {
        return locationFilter.getValue();
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
