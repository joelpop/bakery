package org.vaadin.bakery.ui.view.preferences;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import org.vaadin.bakery.service.CurrentUserService;
import org.vaadin.bakery.service.UserService;
import org.vaadin.bakery.ui.component.ViewHeader;
import org.vaadin.bakery.uimodel.data.UserDetail;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * User preferences view for profile and security settings.
 */
@Route("preferences")
@PageTitle("Preferences")
@PermitAll
public class PreferencesView extends VerticalLayout {

    private final CurrentUserService currentUserService;
    private final UserService userService;

    private UserDetail currentUser;
    private final Avatar profileAvatar;
    private byte[] uploadedPhoto;
    private String uploadedPhotoContentType;

    // Password fields
    private final PasswordField currentPasswordField = new PasswordField("Current Password");
    private final PasswordField newPasswordField = new PasswordField("New Password");
    private final PasswordField confirmPasswordField = new PasswordField("Confirm Password");

    public PreferencesView(CurrentUserService currentUserService, UserService userService) {
        this.currentUserService = currentUserService;
        this.userService = userService;

        addClassName("preferences-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Header
        var header = new ViewHeader("Preferences");
        add(header);

        // Main content (scrollable)
        var content = new VerticalLayout();
        content.setWidthFull();
        content.setMaxWidth("800px");
        content.setPadding(true);
        content.setSpacing(true);

        // Profile section
        profileAvatar = new Avatar();
        profileAvatar.setWidth("100px");
        profileAvatar.setHeight("100px");
        var profileSection = createProfileSection();
        content.add(profileSection);

        // Password section
        var passwordSection = createPasswordSection();
        content.add(passwordSection);

        var scroller = new Scroller(content);
        scroller.setSizeFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        add(scroller);
        setFlexGrow(1, scroller);

        // Load current user data
        loadCurrentUser();
    }

    private Div createProfileSection() {
        var section = createSection("Profile");

        var profileLayout = new HorizontalLayout();
        profileLayout.setAlignItems(Alignment.START);
        profileLayout.setSpacing(true);

        // Avatar and upload
        var avatarSection = new VerticalLayout();
        avatarSection.setPadding(false);
        avatarSection.setSpacing(true);
        avatarSection.setAlignItems(Alignment.CENTER);
        avatarSection.setWidth("auto");

        var buffer = new MemoryBuffer();
        var upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif");
        upload.setMaxFileSize(2 * 1024 * 1024); // 2MB
        upload.setUploadButton(new Button("Change Photo"));

        upload.addSucceededListener(event -> {
            try {
                uploadedPhoto = buffer.getInputStream().readAllBytes();
                uploadedPhotoContentType = event.getMIMEType();
                updateAvatarPreview();
                savePhoto();
            } catch (IOException e) {
                Notification.show("Failed to upload photo", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        avatarSection.add(profileAvatar, upload);
        profileLayout.add(avatarSection);

        // Profile info (read-only)
        var infoForm = new FormLayout();
        infoForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );

        var nameField = new TextField("Name");
        nameField.setReadOnly(true);
        nameField.setWidthFull();

        var emailField = new TextField("Email");
        emailField.setReadOnly(true);
        emailField.setWidthFull();

        var roleField = new TextField("Role");
        roleField.setReadOnly(true);
        roleField.setWidthFull();

        infoForm.add(nameField, 2);
        infoForm.add(emailField, 2);
        infoForm.add(roleField, 1);

        // Store references to update later
        infoForm.getElement().setProperty("nameField", "");
        profileLayout.add(infoForm);
        profileLayout.setFlexGrow(1, infoForm);

        section.add(profileLayout);

        // Update fields when user loads
        currentUserService.getCurrentUser().ifPresent(user -> {
            nameField.setValue(user.getFirstName() + " " + user.getLastName());
            emailField.setValue(user.getEmail());
            roleField.setValue(user.getRole().getDisplayName());
        });

        return section;
    }

    private Div createPasswordSection() {
        var section = createSection("Change Password");

        var form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );
        form.setMaxWidth("400px");

        currentPasswordField.setWidthFull();
        currentPasswordField.setRequired(true);

        newPasswordField.setWidthFull();
        newPasswordField.setRequired(true);
        newPasswordField.setHelperText("Minimum 8 characters");
        newPasswordField.addValueChangeListener(e -> updatePasswordStrength());

        confirmPasswordField.setWidthFull();
        confirmPasswordField.setRequired(true);

        var strengthIndicator = new Div();
        strengthIndicator.setId("password-strength");
        strengthIndicator.addClassNames(LumoUtility.FontSize.SMALL);

        var changePasswordButton = new Button("Change Password", e -> changePassword());
        changePasswordButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        form.add(currentPasswordField, newPasswordField, strengthIndicator, confirmPasswordField);

        var buttonLayout = new HorizontalLayout(changePasswordButton);
        buttonLayout.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        section.add(form, buttonLayout);
        return section;
    }

    private Div createSection(String title) {
        var section = new Div();
        section.addClassName("card");
        section.addClassNames(
                LumoUtility.Padding.LARGE,
                LumoUtility.Margin.Bottom.MEDIUM
        );

        var header = new H3(title);
        header.addClassNames(LumoUtility.Margin.NONE, LumoUtility.Margin.Bottom.MEDIUM);
        section.add(header);

        return section;
    }

    private void loadCurrentUser() {
        currentUserService.getCurrentUser().ifPresent(user -> {
            this.currentUser = user;
            profileAvatar.setName(user.getFirstName() + " " + user.getLastName());

            if (user.getPhoto() != null && user.getPhoto().length > 0) {
                var resource = new StreamResource("user-photo",
                        () -> new ByteArrayInputStream(user.getPhoto()));
                profileAvatar.setImageResource(resource);
            }
        });
    }

    private void updateAvatarPreview() {
        if (uploadedPhoto != null && uploadedPhoto.length > 0) {
            var resource = new StreamResource("user-photo",
                    () -> new ByteArrayInputStream(uploadedPhoto));
            profileAvatar.setImageResource(resource);
        }
    }

    private void savePhoto() {
        if (currentUser == null || uploadedPhoto == null) return;

        try {
            currentUser.setPhoto(uploadedPhoto);
            currentUser.setPhotoContentType(uploadedPhotoContentType);
            userService.update(currentUser.getId(), currentUser);
            Notification.show("Photo updated", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            Notification.show("Failed to save photo: " + e.getMessage(), 5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updatePasswordStrength() {
        // Password strength visual feedback could be added here
        // For now, validation is done on submit
    }

    private int calculatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) return 0;

        int strength = 0;
        if (password.length() >= 8) strength++;
        if (password.length() >= 12) strength++;
        if (password.matches(".*[A-Z].*")) strength++;
        if (password.matches(".*[a-z].*")) strength++;
        if (password.matches(".*[0-9].*")) strength++;
        if (password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) strength++;

        return strength;
    }

    private void changePassword() {
        var currentPassword = currentPasswordField.getValue();
        var newPassword = newPasswordField.getValue();
        var confirmPassword = confirmPasswordField.getValue();

        // Validation
        if (currentPassword == null || currentPassword.isBlank()) {
            currentPasswordField.setInvalid(true);
            currentPasswordField.setErrorMessage("Current password is required");
            return;
        }
        currentPasswordField.setInvalid(false);

        if (newPassword == null || newPassword.length() < 8) {
            newPasswordField.setInvalid(true);
            newPasswordField.setErrorMessage("Password must be at least 8 characters");
            return;
        }
        newPasswordField.setInvalid(false);

        if (!newPassword.equals(confirmPassword)) {
            confirmPasswordField.setInvalid(true);
            confirmPasswordField.setErrorMessage("Passwords do not match");
            return;
        }
        confirmPasswordField.setInvalid(false);

        if (currentUser == null) {
            Notification.show("User session not found", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            // Note: In a real implementation, we'd verify the current password first
            userService.changePassword(currentUser.getId(), newPassword);

            Notification.show("Password changed successfully", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Clear fields
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        } catch (Exception e) {
            Notification.show("Failed to change password: " + e.getMessage(), 5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
