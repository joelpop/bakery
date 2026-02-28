# Preferences View

The Preferences view allows users to manage their account settings, security credentials, and display preferences.

**Route**: `/preferences`

**Access**: All authenticated users (Admin, Baker, Barista)

---

## Layout

The preferences view is organized into sections:

1. Profile Settings — **implemented**
2. Security Settings (password change) — **implemented**
3. Security Settings (passkey management) — *deferred*
4. Display Settings (theme) — *deferred*

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

### Passkey Management *(Deferred)*

> **Note**: Passkey management deferred — requires WebAuthn integration. See [Login View](login.md) for details.

---

> **Note**: The originally planned notification preferences section has been removed. Staff communication is handled by the order messaging system, which uses the activity timeline on each order. See [Messaging](../features/messaging.md) for details.


## Display Settings *(Deferred)*

> **Note**: Theme selection (Light/Dark/System) is planned but not yet implemented.

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
