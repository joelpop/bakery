package org.vaadin.bakery.ui.view.locations;

import com.vaadin.flow.component.ComponentEffect;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.StaleDataException;
import org.vaadin.bakery.ui.component.StaleDataBanner;
import org.vaadin.bakery.ui.component.StaleDataHelper;
import org.vaadin.bakery.ui.event.DataChangeSignals;
import org.vaadin.bakery.uimodel.data.LocationSummary;

/**
 * Dialog for creating and editing locations.
 */
public class LocationDialog extends Dialog {

    private final LocationService locationService;
    private LocationSummary location;
    private final boolean isNew;
    // Banner shown when another session modifies or deletes the location being edited
    private StaleDataBanner staleDataBanner;

    private final TextField nameField;
    private final TextArea addressField;
    private final TextField defaultCountryCodeField;
    private final TextField defaultAreaCodeField;
    private final Checkbox activeCheckbox;
    private final IntegerField sortOrderField;

    private final Binder<LocationSummary> binder;

    /**
     * Creates a location dialog. Opens in create mode if the location has no ID,
     * or edit mode if it has an existing ID.
     *
     * @param location        the location to create or edit
     * @param locationService service for persisting location changes
     */
    public LocationDialog(LocationSummary location, LocationService locationService) {
        this.locationService = locationService;
        this.location = location;
        this.isNew = location.getId() == null;

        // Component initializations
        nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setWidthFull();

        addressField = new TextArea("Address");
        addressField.setWidthFull();
        addressField.setMinHeight("100px");

        defaultCountryCodeField = new TextField("Default Country Code");
        defaultCountryCodeField.setPlaceholder("e.g., 1");
        defaultCountryCodeField.setHelperText("For phone number formatting");

        defaultAreaCodeField = new TextField("Default Area Code");
        defaultAreaCodeField.setPlaceholder("e.g., 212");
        defaultAreaCodeField.setHelperText("For 7-digit phone numbers");

        activeCheckbox = new Checkbox("Active");

        sortOrderField = new IntegerField("Sort Order");
        sortOrderField.setMin(0);
        sortOrderField.setStepButtonsVisible(true);

        var cancelButton = new Button("Cancel", _ -> close());

        var saveButton = new Button("Save", _ -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Binder bindings
        binder = new Binder<>(LocationSummary.class);

        binder.forField(nameField)
                .asRequired("Name is required")
                .withValidator(name -> isNew ?
                                !locationService.nameExists(name) :
                                !locationService.nameExistsForOtherLocation(name, location.getId()),
                        "A location with this name already exists")
                .bind(LocationSummary::getName, LocationSummary::setName);

        binder.forField(addressField)
                .bind(LocationSummary::getAddress, LocationSummary::setAddress);

        binder.forField(defaultCountryCodeField)
                .bind(LocationSummary::getDefaultCountryCode, LocationSummary::setDefaultCountryCode);

        binder.forField(defaultAreaCodeField)
                .bind(LocationSummary::getDefaultAreaCode, LocationSummary::setDefaultAreaCode);

        binder.forField(activeCheckbox)
                .bind(LocationSummary::isActive, LocationSummary::setActive);

        binder.forField(sortOrderField)
                .bind(LocationSummary::getSortOrder, LocationSummary::setSortOrder);

        // Value settings
        sortOrderField.setValue(0);

        if (isNew) {
            activeCheckbox.setValue(true);
        }

        binder.readBean(location);

        // Layout assembly
        var form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );
        form.add(nameField, 2);
        form.add(sortOrderField, 1);
        form.add(addressField, 2);
        form.add(defaultCountryCodeField, 1);
        form.add(defaultAreaCodeField, 1);
        form.add(activeCheckbox, 2);

        var footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        if (!isNew) {
            var deleteButton = new Button("Delete", _ -> confirmDelete());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

            var spacer = new Span();
            footer.add(deleteButton, spacer, cancelButton, saveButton);
            footer.setFlexGrow(1, spacer);
        } else {
            footer.add(cancelButton, saveButton);
        }

        // Stale data detection - when editing an existing location, monitor for changes from other sessions
        if (!isNew) {
            staleDataBanner = new StaleDataBanner();

            // Reactive effect: checks if the location was modified or deleted by another session
            // whenever the shared locationVersion signal changes
            ComponentEffect.effect(this, () -> {
                DataChangeSignals.locationVersion().value();
                checkForExternalChanges();
            });
        }

        // Dialog configuration
        setHeaderTitle(isNew ? "New Location" : "Edit Location");
        setModal(true);
        setCloseOnOutsideClick(false);
        getElement().getThemeList().add("responsive-dialog");
        setWidth("100%");
        setMaxWidth("500px");
        if (!isNew) {
            add(staleDataBanner);
        }
        add(form);
        getFooter().add(footer);
    }

    private void save() {
        try {
            binder.writeBean(location);

            if (isNew) {
                locationService.create(location);
                Notification.show("Location created", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                // Pre-save freshness check: abort save if the location was modified or deleted by another session
                if (StaleDataHelper.isStale(
                        () -> locationService.getVersion(location.getId()),
                        location.getVersion(), staleDataBanner,
                        this::reloadData, this::close)) {
                    return;
                }

                locationService.update(location.getId(), location);
                Notification.show("Location updated", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }

            fireEvent(new SaveEvent(this));
            close();
        } catch (StaleDataException _) {
            // Fallback: optimistic lock failed during flush — show stale data banner
            staleDataBanner.showModified(this::reloadData);
        } catch (ValidationException _) {
            Notification.show("Please fix the validation errors", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /** Live detection: compares the DB version against the version loaded into the form. */
    private void checkForExternalChanges() {
        if (!isOpened()) return;
        StaleDataHelper.checkForExternalChanges(
                () -> locationService.getVersion(location.getId()),
                location.getVersion(), staleDataBanner,
                this::reloadData, this::close);
    }

    /** Reloads the latest location data from the database into the form, or shows deleted banner. */
    private void reloadData() {
        locationService.get(location.getId()).ifPresentOrElse(
                freshLocation -> {
                    location = freshLocation;
                    binder.readBean(location);
                    staleDataBanner.hide();
                },
                () -> staleDataBanner.showDeleted(this::close)
        );
    }

    private void confirmDelete() {
        var confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Delete Location");
        confirmDialog.add(new Span("Are you sure you want to delete \"" + location.getName() + "\"?"));

        var cancelButton = new Button("Cancel", _ -> confirmDialog.close());
        var deleteButton = new Button("Delete", _ -> {
            confirmDialog.close();
            delete();
        });
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        confirmDialog.getFooter().add(cancelButton, deleteButton);
        confirmDialog.open();
    }

    private void delete() {
        try {
            locationService.delete(location.getId());
            Notification.show("Location deleted", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            fireEvent(new DeleteEvent(this));
            close();
        } catch (Exception e) {
            Notification.show("Cannot delete location: " + e.getMessage(), 5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // Events

    /** Event fired when a location is successfully saved. */
    public static class SaveEvent extends ComponentEvent<LocationDialog> {
        public SaveEvent(LocationDialog source) {
            super(source, false);
        }
    }

    /** Event fired when a location is successfully deleted. */
    public static class DeleteEvent extends ComponentEvent<LocationDialog> {
        public DeleteEvent(LocationDialog source) {
            super(source, false);
        }
    }

    /** Registers a listener for save events. */
    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    /** Registers a listener for delete events. */
    public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
        return addListener(DeleteEvent.class, listener);
    }
}
