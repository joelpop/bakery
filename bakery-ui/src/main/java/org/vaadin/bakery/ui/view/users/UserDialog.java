package org.vaadin.bakery.ui.view.users;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
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
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.service.UserService;
import org.vaadin.bakery.uimodel.data.UserDetail;
import org.vaadin.bakery.uimodel.type.UserRole;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Dialog for creating and editing users.
 */
public class UserDialog extends Dialog {

    private final UserService userService;
    private final UserDetail user;
    private final boolean isNew;
    private final String currentUserEmail;
    private final boolean isEditingSelf;

    private final TextField emailField = new TextField("Email");
    private final TextField firstNameField = new TextField("First Name");
    private final TextField lastNameField = new TextField("Last Name");
    private final PasswordField passwordField = new PasswordField("Password");
    private final ComboBox<UserRole> roleComboBox = new ComboBox<>("Role");

    private final Div photoContainer = new Div();
    private byte[] uploadedPhoto;
    private String uploadedPhotoContentType;

    private final Binder<UserDetail> binder = new Binder<>(UserDetail.class);

    public UserDialog(UserDetail user, UserService userService, String currentUserEmail) {
        this.userService = userService;
        this.currentUserEmail = currentUserEmail;

        // Create new user if null
        if (user == null) {
            this.user = new UserDetail();
            this.isNew = true;
        } else {
            this.user = user;
            this.isNew = user.isNew();
        }

        this.isEditingSelf = !isNew && this.user.getEmail() != null &&
                this.user.getEmail().equalsIgnoreCase(currentUserEmail);

        setHeaderTitle(isNew ? "New User" : "Edit User");
        setModal(true);
        setCloseOnOutsideClick(false);
        setWidth("600px");

        configureFields();
        configureBinder();
        createLayout();
        createFooter();

        binder.readBean(this.user);
        updatePhotoPreview();
    }

    private void configureFields() {
        emailField.setRequired(true);
        emailField.setWidthFull();

        firstNameField.setRequired(true);
        firstNameField.setWidthFull();

        lastNameField.setRequired(true);
        lastNameField.setWidthFull();

        passwordField.setWidthFull();
        passwordField.setHelperText(isNew ?
                "Required for new users" :
                "Leave empty to keep current password");
        passwordField.setRevealButtonVisible(true);

        roleComboBox.setItems(UserRole.values());
        roleComboBox.setItemLabelGenerator(UserRole::getDisplayName);
        roleComboBox.setRequired(true);
        roleComboBox.setWidthFull();

        // Self-edit restrictions
        if (isEditingSelf) {
            roleComboBox.setEnabled(false);
            roleComboBox.setHelperText("You cannot change your own role");
        }
    }

    private void configureBinder() {
        binder.forField(emailField)
                .asRequired("Email is required")
                .withValidator(email -> email.contains("@"), "Please enter a valid email address")
                .withValidator(email -> isNew ?
                                !userService.emailExists(email) :
                                !userService.emailExistsForOtherUser(email, user.getId()),
                        "A user with this email already exists")
                .bind(UserDetail::getEmail, UserDetail::setEmail);

        binder.forField(firstNameField)
                .asRequired("First name is required")
                .bind(UserDetail::getFirstName, UserDetail::setFirstName);

        binder.forField(lastNameField)
                .asRequired("Last name is required")
                .bind(UserDetail::getLastName, UserDetail::setLastName);

        binder.forField(passwordField)
                .withValidator(password -> !isNew || (password != null && !password.isEmpty()),
                        "Password is required for new users")
                .withValidator(password -> password == null || password.isEmpty() || password.length() >= 8,
                        "Password must be at least 8 characters")
                .bind(UserDetail::getPassword, UserDetail::setPassword);

        binder.forField(roleComboBox)
                .asRequired("Role is required")
                .bind(UserDetail::getRole, UserDetail::setRole);
    }

    private void createLayout() {
        var form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );

        // Photo upload section
        var photoSection = createPhotoSection();
        form.add(photoSection, 2);

        form.add(emailField, 2);
        form.add(firstNameField, 1);
        form.add(lastNameField, 1);
        form.add(passwordField, 2);
        form.add(roleComboBox, 2);

        add(form);
    }

    private Div createPhotoSection() {
        var section = new Div();
        section.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Margin.Bottom.MEDIUM
        );

        photoContainer.getStyle()
                .set("width", "80px")
                .set("height", "80px");

        var buffer = new MemoryBuffer();
        var upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif");
        upload.setMaxFileSize(2 * 1024 * 1024); // 2MB

        upload.addSucceededListener(event -> {
            try {
                uploadedPhoto = buffer.getInputStream().readAllBytes();
                uploadedPhotoContentType = event.getMIMEType();
                updatePhotoPreview();
            } catch (IOException e) {
                Notification.show("Failed to upload image", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        section.add(photoContainer, upload);
        return section;
    }

    private void updatePhotoPreview() {
        photoContainer.removeAll();

        byte[] photoData = uploadedPhoto != null ? uploadedPhoto : user.getPhoto();
        String name = (user.getFirstName() != null ? user.getFirstName() : "") + " " +
                (user.getLastName() != null ? user.getLastName() : "");

        var avatar = new Avatar(name.trim());
        avatar.setWidth("80px");
        avatar.setHeight("80px");

        if (photoData != null && photoData.length > 0) {
            var resource = new StreamResource("user-photo",
                    () -> new ByteArrayInputStream(photoData));
            avatar.setImageResource(resource);
        }

        photoContainer.add(avatar);
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

            // Cannot delete yourself
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

        getFooter().add(footer);
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
                userService.update(user.getId(), user);
                Notification.show("User updated", 3000, Notification.Position.BOTTOM_START)
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
        if (isEditingSelf) {
            Notification.show("You cannot delete your own account", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        var confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Delete User");
        confirmDialog.add(new Span("Are you sure you want to delete \"" +
                user.getFirstName() + " " + user.getLastName() + "\"?"));

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
            userService.delete(user.getId());
            Notification.show("User deleted", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            fireEvent(new DeleteEvent(this));
            close();
        } catch (Exception e) {
            Notification.show("Cannot delete user: " + e.getMessage(), 5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // Events
    public static class SaveEvent extends ComponentEvent<UserDialog> {
        public SaveEvent(UserDialog source) {
            super(source, false);
        }
    }

    public static class DeleteEvent extends ComponentEvent<UserDialog> {
        public DeleteEvent(UserDialog source) {
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
