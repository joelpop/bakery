# Login View

The login view provides user authentication for the Bakery application.

**Route**: `/login`

**Access**: Anonymous (unauthenticated users)

## Screenshots
- Desktop: `legacy/images/login view/Desktop, login form.png` (if available)
- Phone: `legacy/images/login view/Phone, login form.png` (if available)

---

## Layout

The login view is centered on the screen and contains:

| Element | Description |
|---------|-------------|
| Logo | Café Sunshine branding/logo image |
| Title | Application name heading |
| Authentication Options | Credential and passkey authentication methods |

---

## Authentication Methods

The login view supports multiple authentication methods for user convenience and security.

### Password Authentication

Traditional username/password authentication using Spring Security form login.

| Field | Type | Description |
|-------|------|-------------|
| Email | Text input | User's email address (login identifier) |
| Password | Password input | User's password |
| Login Button | Button | Submits credentials for authentication |

**Behavior**:
- Credentials are submitted to Spring Security's login endpoint
- Invalid credentials display an error message on the form
- Successful authentication redirects to the originally requested view, or to the Storefront (default route) if no specific view was requested

**Planned**: Role-based default redirect (Admin → Dashboard, Baker → Bakery, Barista → Storefront). Currently all roles land on Storefront.

### Passkey Authentication (WebAuthn) *(Deferred)*

> **Note**: Full WebAuthn implementation deferred due to webauthn4j dependency issues. A placeholder passkey button is present in the UI but disabled with a "Coming Soon" label.

| Element | Description |
|---------|-------------|
| Passkey Button | Disabled placeholder — "Sign in with Passkey (Coming Soon)" |

**Planned Supported Authenticators** (when implemented):
- **Platform authenticators**: TouchID, FaceID, Windows Hello, Android biometrics
- **Roaming authenticators**: YubiKey, other FIDO2 security keys

---

## Error States

| Scenario | Display |
|----------|---------|
| Invalid credentials | Error message on login form: "Incorrect email or password" |
| Account locked | Error message: "Account is locked. Contact an administrator." |
| Session expired | Redirect to login with informational message |
| Passkey failed | Error message: "Passkey authentication failed. Try again or use password." |

---

## Security Considerations

| Feature | Implementation |
|---------|----------------|
| CSRF protection | Handled automatically by Vaadin/Spring Security |
| Secure cookies | `HttpOnly`, `Secure`, `SameSite=Strict` flags |
| Rate limiting | Failed login attempts are rate-limited to prevent brute force |
| Session fixation | Session ID is regenerated after successful authentication |

---

## Accessibility

| Feature | Description |
|---------|-------------|
| Keyboard navigation | Full keyboard support for form fields and buttons |
| Screen reader support | Proper ARIA labels and error announcements |
| Focus management | Focus moves to error messages when displayed |

---

## Related Documentation

- [Authentication](../security/authentication.md) - Authentication flow and session management
- [User Menu](../features/user-menu.md) - Passkey registration in preferences
