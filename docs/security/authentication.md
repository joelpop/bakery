# Authentication

This document describes user authentication in the Bakery application.

## Authentication Methods

The application supports multiple authentication methods:

| Method | Description |
|--------|-------------|
| Password | Traditional email/password authentication |
| Passkey (WebAuthn) | Biometric authentication (TouchID, FaceID, Windows Hello) and hardware security keys |

See [Login View](../views/login.md) for the user interface details.

---

## Login Flow

### Password Authentication

| Step | Description |
|------|-------------|
| 1 | User navigates to a protected view |
| 2 | Spring Security redirects unauthenticated users to `/login` |
| 3 | User submits email and password |
| 4 | `UserDetailsService` loads the user by email |
| 5 | Password is verified against BCrypt hash |
| 6 | Session is created with SecurityContext |
| 7 | User is redirected to the originally requested view |

### Passkey Authentication (WebAuthn)

| Step | Description |
|------|-------------|
| 1 | User clicks the passkey authentication button |
| 2 | Browser invokes the WebAuthn API |
| 3 | Authenticator prompts for biometric/PIN verification |
| 4 | Signed assertion is sent to the server |
| 5 | Server verifies the credential signature |
| 6 | Session is created with SecurityContext |
| 7 | User is redirected to the originally requested view |

---

## Logout

| Step | Description |
|------|-------------|
| 1 | User clicks "Log out" in the user menu |
| 2 | Request is sent to the `/logout` endpoint |
| 3 | Session is invalidated |
| 4 | SecurityContext is cleared |
| 5 | Session cookie is deleted |
| 6 | User is redirected to the login page |

---

## Current User Access

A `CurrentUserService` component provides access to the authenticated user throughout the application.

### Available Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| getCurrentUserEmail() | Optional&lt;String&gt; | Email of the authenticated user |
| getCurrentUser() | Optional&lt;User&gt; | Current user as UI model |
| hasRole(role) | boolean | Check if user has a specific role |
| isAdmin() | boolean | Check if user has ADMIN role |

### Usage

Views and services can inject `CurrentUserService` to access the authenticated user's information and check permissions for conditional UI rendering.

## Session Management

| Setting | Value | Description |
|---------|-------|-------------|
| Session timeout | 30 minutes | Sessions expire after inactivity |
| Session fixation protection | Enabled | Session ID regenerated after login |
| Concurrent sessions | Unlimited | Users can be logged in on multiple devices simultaneously |

### Remember Me (Future Enhancement)

A "Remember Me" option could extend the session validity beyond the default timeout using a persistent token stored in a secure cookie.

---

## Error Handling

| Scenario | Behavior |
|----------|----------|
| Invalid credentials | Redirect to `/login?error`, display error on form |
| Account locked | Display "Account is locked" message |
| Session expired | Redirect to login page |
| AJAX request with expired session | 401 response, Vaadin displays session expired notification |

See [Exception Views](../views/exceptions.md) for error page details.

---

## Related Documentation

- [Login View](../views/login.md) - Login interface and authentication methods
- [Configuration](configuration.md) - Security configuration details
- [Authorization](authorization.md) - Role-based access control
