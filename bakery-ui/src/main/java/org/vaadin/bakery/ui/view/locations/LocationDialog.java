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

    private final TextField nameField = new TextField("Name");
    private final TextField codeField = new TextField("Code");
    private final TextArea addressField = new TextArea("Address");
    private final Checkbox activeCheckbox = new Checkbox("Active");
    private final IntegerField sortOrderField = new IntegerField("Sort Order");

    private final Binder<LocationSummary> binder = new Binder<>(LocationSummary.class);

    public LocationDialog(LocationSummary location, LocationService locationService) {
        this.locationService = locationService;
        this.location = location;
        this.isNew = location.getId() == null;

        setHeaderTitle(isNew ? "New Location" : "Edit Location");
        setModal(true);
        setCloseOnOutsideClick(false);
        // Responsive sizing: max-width on desktop, full-screen on mobile via CSS theme
        getElement().getThemeList().add("responsive-dialog");
        setWidth("100%");
        setMaxWidth("500px");

        configureFields();
        configureBinder();
        createLayout();
        createFooter();

        binder.readBean(location);
    }

    private void configureFields() {
        nameField.setRequired(true);
        nameField.setWidthFull();

        codeField.setRequired(true);
        codeField.setWidthFull();
        codeField.setHelperText("Short identifier (e.g., STORE, BAKERY)");

        addressField.setWidthFull();
        addressField.setMinHeight("100px");

        sortOrderField.setMin(0);
        sortOrderField.setStepButtonsVisible(true);
        sortOrderField.setValue(0);

        if (isNew) {
            activeCheckbox.setValue(true);
        }
    }

    private void configureBinder() {
        binder.forField(nameField)
                .asRequired("Name is required")
                .withValidator(name -> isNew ?
                                !locationService.nameExists(name) :
                                !locationService.nameExistsForOtherLocation(name, location.getId()),
                        "A location with this name already exists")
                .bind(LocationSummary::getName, LocationSummary::setName);

        binder.forField(codeField)
                .asRequired("Code is required")
                .withValidator(code -> isNew ?
                                !locationService.codeExists(code) :
                                !locationService.codeExistsForOtherLocation(code, location.getId()),
                        "A location with this code already exists")
                .bind(LocationSummary::getCode, LocationSummary::setCode);

        binder.forField(addressField)
                .bind(LocationSummary::getAddress, LocationSummary::setAddress);

        binder.forField(activeCheckbox)
                .bind(LocationSummary::isActive, LocationSummary::setActive);

        binder.forField(sortOrderField)
                .bind(LocationSummary::getSortOrder, LocationSummary::setSortOrder);
    }

    private void createLayout() {
        var form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );

        form.add(nameField, 2);
        form.add(codeField, 1);
        form.add(sortOrderField, 1);
        form.add(addressField, 2);
        form.add(activeCheckbox, 2);

        add(form);
    }

    private void createFooter() {
        var footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        var cancelButton = new Button("Cancel", e -> close());

        var saveButton = new Button("Save", e -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        if (!isNew) {
            var deleteButton = new Button("Delete", e -> confirmDelete());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

            var spacer = new Span();
            footer.add(deleteButton, spacer, cancelButton, saveButton);
            footer.setFlexGrow(1, spacer);
        } else {
            footer.add(cancelButton, saveButton);
        }

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
