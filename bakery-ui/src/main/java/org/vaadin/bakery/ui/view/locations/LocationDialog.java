package org.vaadin.bakery.ui.view.locations;

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
import org.vaadin.bakery.uimodel.data.LocationSummary;

/**
 * Dialog for creating and editing locations.
 */
public class LocationDialog extends Dialog {

    private final LocationService locationService;
    private final LocationSummary location;
    private final boolean isNew;

    private final TextField nameField;
    private final TextArea addressField;
    private final TextField defaultCountryCodeField;
    private final TextField defaultAreaCodeField;
    private final Checkbox activeCheckbox;
    private final IntegerField sortOrderField;

    private final Binder<LocationSummary> binder;

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

        var cancelButton = new Button("Cancel", e -> close());

        var saveButton = new Button("Save", e -> save());
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
            var deleteButton = new Button("Delete", e -> confirmDelete());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

            var spacer = new Span();
            footer.add(deleteButton, spacer, cancelButton, saveButton);
            footer.setFlexGrow(1, spacer);
        } else {
            footer.add(cancelButton, saveButton);
        }

        // Dialog configuration
        setHeaderTitle(isNew ? "New Location" : "Edit Location");
        setModal(true);
        setCloseOnOutsideClick(false);
        getElement().getThemeList().add("responsive-dialog");
        setWidth("100%");
        setMaxWidth("500px");
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
                locationService.update(location.getId(), location);
                Notification.show("Location updated", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }

            fireEvent(new SaveEvent(this));
            close();
        } catch (ValidationException e) {
            Notification.show("Please fix the validation errors", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void confirmDelete() {
        var confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Delete Location");
        confirmDialog.add(new Span("Are you sure you want to delete \"" + location.getName() + "\"?"));

        var cancelButton = new Button("Cancel", e -> confirmDialog.close());
        var deleteButton = new Button("Delete", e -> {
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
    public static class SaveEvent extends ComponentEvent<LocationDialog> {
        public SaveEvent(LocationDialog source) {
            super(source, false);
        }
    }

    public static class DeleteEvent extends ComponentEvent<LocationDialog> {
        public DeleteEvent(LocationDialog source) {
            super(source, false);
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
        return addListener(DeleteEvent.class, listener);
    }
}
