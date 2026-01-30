# Preferences View

The Preferences view allows users to manage their account settings, security credentials, and display preferences.

**Route**: `/preferences`

**Access**: All authenticated users (Admin, Baker, Barista)

---

## Layout

The preferences view is organized into sections:

1. Profile Settings
2. Security Settings
3. Notification Preferences
4. Display Settings

---

## Profile Settings

### Fields

| Field | Type | Editable | Description |
|-------|------|----------|-------------|
| Profile Photo | Image Upload | Yes | User's profile picture |
| First Name | Text | No | Display only (managed by admin) |
| Last Name | Text | No | Display only (managed by admin) |
| Email | Text | No | Login identifier (managed by admin) |
| Role | Badge | No | Current role (Admin, Baker, Barista) |

### Photo Upload

- Accepts JPEG, PNG formats
- Maximum file size: 2MB
- Image is cropped/resized to standard dimensions
- Preview shown before saving

---

## Security Settings

### Change Password

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| Current Password | Password | Yes | Verify identity before changing |
| New Password | Password | Yes | Must meet entropy requirements |
| Confirm Password | Password | Yes | Must match new password |

**Password Requirements**:
- Minimum 50 bits entropy (strength indicator shown)
- Not in common password blocklist
- Visual strength meter: Weak → Fair → Good → Strong → Very Strong

### Passkey Management

| Element | Description |
|---------|-------------|
| Registered Passkeys | List of registered WebAuthn credentials |
| Add Passkey | Button to register new passkey |
| Remove Passkey | Button to delete existing passkey |

#### Add Passkey Flow

| Step | Description |
|------|-------------|
| 1 | User clicks "Add Passkey" |
| 2 | Browser prompts for authenticator selection |
| 3 | User completes biometric/PIN verification |
| 4 | Passkey is registered with descriptive name |
| 5 | New passkey appears in list |

#### Passkey List Item

| Field | Description |
|-------|-------------|
| Name | User-provided name (e.g., "MacBook TouchID") |
| Created | Registration date |
| Last Used | Last authentication date |
| Remove | Button to delete credential |

---

## Notification Preferences

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| Email Notifications | Toggle | Off | Receive email alerts for important events |
| In-App Notifications | Toggle | On | Show notifications in user menu |

**Note**: Full notification system is deferred for future implementation. These preferences are placeholders.

---

## Display Settings

| Setting | Type | Options | Description |
|---------|------|---------|-------------|
| Theme | Radio/Toggle | Light, Dark, System | UI color scheme preference |

**Note**: Language selection may be added as future enhancement.

---

## Actions

| Button | Description |
|--------|-------------|
| Save Changes | Persist all modified settings |
| Cancel | Discard changes and return to previous view |

---

## Validation

| Field | Rule |
|-------|------|
| Current Password | Must be correct for password change |
| New Password | Minimum 50 bits entropy |
| Confirm Password | Must match new password |
| Photo | Valid image format, max 2MB |

---

## Responsive Behavior

### Phone Layout
- Sections stack vertically
- Full-width form fields
- Photo upload uses native file picker

---

## Related Documentation

- [User Menu](../features/user-menu.md) - Accesses preferences
- [Authentication](../security/authentication.md) - Password and passkey details
- [Security Configuration](../security/configuration.md) - Password requirements
