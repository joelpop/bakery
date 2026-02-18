package org.vaadin.bakery.ui.view.users;

import com.vaadin.flow.signals.impl.Effect;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.StaleDataException;
import org.vaadin.bakery.service.UserService;
import org.vaadin.bakery.ui.component.StaleDataBanner;
import org.vaadin.bakery.ui.component.StaleDataHelper;
import org.vaadin.bakery.ui.event.DataChangeSignals;
import org.vaadin.bakery.ui.event.NonComponent;
import org.vaadin.bakery.ui.event.NonComponentEvent;
import org.vaadin.bakery.ui.event.NonComponentEventSupport;
import org.vaadin.bakery.uimodel.data.LocationSummary;
import org.vaadin.bakery.uimodel.data.UserDetail;
import org.vaadin.bakery.uimodel.type.UserRole;

import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog for creating and editing users.
 * Uses delegation rather than inheritance to avoid exposing Dialog's full API.
 */
public class UserDialog implements NonComponent {

    private final Dialog dialog;
    private final NonComponentEventSupport<UserDialog> eventSupport;

    private final UserService userService;
    private UserDetail user;
    private final boolean isNew;
    private final boolean isEditingSelf;
    // Banner shown when another session modifies or deletes the user being edited
    private StaleDataBanner staleDataBanner;

    private final TextField emailField;
    private final TextField firstNameField;
    private final TextField lastNameField;
    private final PasswordField passwordField;
    private final ComboBox<UserRole> roleComboBox;
    private final ComboBox<LocationSummary> primaryLocationComboBox;

    private final List<LocationSummary> locations;

    private final Div photoContainerDiv;
    private byte[] uploadedPhoto;
    private String uploadedPhotoContentType;

    private final Binder<UserDetail> binder;

    /**
     * Creates a user dialog. Opens in create mode if the user is {@code null} or new,
     * or edit mode for an existing user.
     *
     * @param user             the user to create or edit, or {@code null} for a new user
     * @param userService      service for persisting user changes
     * @param locationService  service for loading available locations
     * @param currentUserEmail email of the currently logged-in user, used to prevent self-deletion and role changes
     */
    public UserDialog(UserDetail user, UserService userService, LocationService locationService, String currentUserEmail) {
        this.userService = userService;

        // Determine user state
        if (user == null) {
            this.user = new UserDetail();
            this.isNew = true;
        } else {
            this.user = user;
            this.isNew = user.isNew();
        }

        this.isEditingSelf = !isNew && this.user.getEmail() != null &&
                this.user.getEmail().equalsIgnoreCase(currentUserEmail);

        dialog = new Dialog();
        eventSupport = new NonComponentEventSupport<>();

        // Load data
        locations = locationService.listActive();

        // Component initializations
        emailField = new TextField("Email");
        emailField.setRequired(true);
        emailField.setWidthFull();

        firstNameField = new TextField("First Name");
        firstNameField.setRequired(true);
        firstNameField.setWidthFull();

        lastNameField = new TextField("Last Name");
        lastNameField.setRequired(true);
        lastNameField.setWidthFull();

        passwordField = new PasswordField("Password");
        passwordField.setWidthFull();
        passwordField.setHelperText(isNew ?
                "Required for new users" :
                "Leave empty to keep current password");
        passwordField.setRevealButtonVisible(true);

        roleComboBox = new ComboBox<>("Role");
        roleComboBox.setItems(UserRole.values());
        roleComboBox.setItemLabelGenerator(UserRole::getDisplayName);
        roleComboBox.setRequired(true);
        roleComboBox.setWidthFull();
        if (isEditingSelf) {
            roleComboBox.setEnabled(false);
            roleComboBox.setHelperText("You cannot change your own role");
        }

        primaryLocationComboBox = new ComboBox<>("Primary Location");
        primaryLocationComboBox.setItems(locations);
        primaryLocationComboBox.setItemLabelGenerator(LocationSummary::getName);
        primaryLocationComboBox.setClearButtonVisible(true);
        primaryLocationComboBox.setWidthFull();
        primaryLocationComboBox.setPlaceholder("Select primary location...");

        photoContainerDiv = new Div();
        photoContainerDiv.getStyle()
                .set("width", "80px")
                .set("height", "80px");

        var upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            uploadedPhoto = data;
            uploadedPhotoContentType = metadata.contentType();
            updatePhotoPreview();
        }));
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif");
        upload.setMaxFileSize(2 * 1024 * 1024); // 2MB

        var photoSection = new Div();
        photoSection.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Margin.Bottom.MEDIUM
        );
        photoSection.add(photoContainerDiv, upload);

        var cancelButton = new Button("Cancel", _ -> close());

        var saveButton = new Button("Save", _ -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Binder bindings
        binder = new Binder<>(UserDetail.class);

        binder.forField(emailField)
                .asRequired("Email is required")
                .withValidator(email -> email.contains("@"), "Please enter a valid email address")
                .withValidator(email -> isNew ?
                                !userService.emailExists(email) :
                                !userService.emailExistsForOtherUser(email, this.user.getId()),
                        "A user with this email already exists")
                .bind(UserDetail::getEmail, UserDetail::setEmail);

        binder.forField(firstNameField)
                .asRequired("First name is required")
                .bind(UserDetail::getFirstName, UserDetail::setFirstName);

        binder.forField(lastNameField)
                .asRequired("Last name is required")
                .bind(UserDetail::getLastName, UserDetail::setLastName);

        binder.forField(passwordField)
                .withValidator(password -> !isNew || !password.isEmpty(),
                        "Password is required for new users")
                .withValidator(password -> password.isEmpty() || password.length() >= 8,
                        "Password must be at least 8 characters")
                .bind(UserDetail::getPassword, UserDetail::setPassword);

        binder.forField(roleComboBox)
                .asRequired("Role is required")
                .bind(UserDetail::getRole, UserDetail::setRole);

        binder.forField(primaryLocationComboBox)
                .bind(
                        userDetail -> {
                            var locationId = userDetail.getPrimaryLocationId();
                            if (locationId == null) {
                                return null;
                            }
                            return locations.stream()
                                    .filter(loc -> loc.getId().equals(locationId))
                                    .findFirst()
                                    .orElse(null);
                        },
                        (userDetail, location) -> userDetail.setPrimaryLocationId(
                                location != null ? location.getId() : null
                        )
                );

        // Value settings
        binder.readBean(this.user);
        updatePhotoPreview();

        // Layout assembly
        var form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );
        form.add(photoSection, 2);
        form.add(emailField, 2);
        form.add(firstNameField, 1);
        form.add(lastNameField, 1);
        form.add(passwordField, 2);
        form.add(roleComboBox, 1);
        form.add(primaryLocationComboBox, 1);

        var footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        if (!isNew) {
            var deleteButton = new Button("Delete", _ -> confirmDelete());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            if (isEditingSelf) {
                deleteButton.setEnabled(false);
                deleteButton.setTooltipText("You cannot delete your own account");
            }

            var spacer = new Span();
            footer.add(deleteButton, spacer, cancelButton, saveButton);
            footer.setFlexGrow(1, spacer);
        } else {
            footer.add(cancelButton, saveButton);
        }

        // Stale data detection - when editing an existing user, monitor for changes from other sessions
        if (!isNew) {
            staleDataBanner = new StaleDataBanner();

            // Reactive effect: checks if the user was modified or deleted by another session
            // whenever the shared userVersion signal changes
            Effect.effect(dialog, () -> {
                DataChangeSignals.userVersion().get();
                checkForExternalChanges();
            });
        }

        // Dialog configuration
        dialog.setHeaderTitle(isNew ? "New User" : "Edit User");
        dialog.setCloseOnOutsideClick(false);
        dialog.getElement().getThemeList().add("responsive-dialog");
        dialog.setWidth("100%");
        dialog.setMaxWidth("600px");
        if (!isNew) {
            dialog.add(staleDataBanner);
        }
        dialog.add(form);
        dialog.getFooter().add(footer);
    }

    /** Opens the dialog. */
    public void open() {
        dialog.open();
    }

    /** Closes the dialog. */
    public void close() {
        dialog.close();
    }

    private void updatePhotoPreview() {
        photoContainerDiv.removeAll();

        byte[] photoData = uploadedPhoto != null ? uploadedPhoto : user.getPhoto();
        String name = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "");

        var avatar = new Avatar(name.trim());
        avatar.setWidth("80px");
        avatar.setHeight("80px");

        if (photoData != null && photoData.length > 0) {
            avatar.setImageHandler(event -> event.getOutputStream().write(photoData));
        }

        photoContainerDiv.add(avatar);
    }

    private void save() {
        try {
            binder.writeBean(user);

            // Apply uploaded photo if any
            if (uploadedPhoto != null) {
                user.setPhoto(uploadedPhoto);
                user.setPhotoContentType(uploadedPhotoContentType);
            }

            if (isNew) {
                userService.create(user);
                Notification.show("User created", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                // Pre-save freshness check: abort save if the user was modified or deleted by another session
                if (StaleDataHelper.isStale(
                        () -> userService.getVersion(user.getId()),
                        user.getVersion(), staleDataBanner,
                        this::reloadData, this::close)) {
                    return;
                }

                userService.update(user.getId(), user);
                Notification.show("User updated", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }

            eventSupport.fireEvent(new SaveEvent(this));
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
        if (!dialog.isOpened()) return;
        StaleDataHelper.checkForExternalChanges(
                () -> userService.getVersion(user.getId()),
                user.getVersion(), staleDataBanner,
                this::reloadData, this::close);
    }

    /** Reloads the latest user data from the database into the form, or shows deleted banner. */
    private void reloadData() {
        userService.get(user.getId()).ifPresentOrElse(
                freshUser -> {
                    user = freshUser;
                    binder.readBean(user);
                    updatePhotoPreview();
                    staleDataBanner.hide();
                },
                () -> staleDataBanner.showDeleted(this::close)
        );
    }

    private void confirmDelete() {
        if (isEditingSelf) {
            Notification.show("You cannot delete your own account", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        var confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Delete User");
        confirmDialog.add(new Span("Are you sure you want to delete \"" +
                user.getFirstName() + " " + user.getLastName() + "\"?"));

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
            userService.delete(user.getId());
            Notification.show("User deleted", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            eventSupport.fireEvent(new DeleteEvent(this));
            close();
        } catch (Exception e) {
            Notification.show("Cannot delete user: " + e.getMessage(), 5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // Event Handlers

    // NonComponent implementation

    @Override
    @SuppressWarnings("unchecked")
    public <E extends NonComponentEvent<?>> Registration addListener(Class<E> eventType, Consumer<E> listener) {
        return eventSupport.addListener((Class<NonComponentEvent<UserDialog>>) eventType,
                (Consumer<NonComponentEvent<UserDialog>>) listener);
    }

    /** Registers a listener for save events. */
    public Registration addSaveListener(Consumer<SaveEvent> listener) {
        return eventSupport.addListener(SaveEvent.class, listener);
    }

    /** Registers a listener for delete events. */
    public Registration addDeleteListener(Consumer<DeleteEvent> listener) {
        return eventSupport.addListener(DeleteEvent.class, listener);
    }

    // Events

    /** Event fired when a user is successfully saved. */
    public static class SaveEvent extends NonComponentEvent<UserDialog> {
        /** Creates a save event. */
        public SaveEvent(UserDialog source) {
            super(source);
        }
    }

    /** Event fired when a user is successfully deleted. */
    public static class DeleteEvent extends NonComponentEvent<UserDialog> {
        /** Creates a delete event. */
        public DeleteEvent(UserDialog source) {
            super(source);
        }
    }
}
