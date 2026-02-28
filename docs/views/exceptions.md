# Exception Views

The application provides user-friendly error pages for various error conditions. All exceptions are logged for troubleshooting.

---

## Error Types

| Error | HTTP Status | Route | Description |
|-------|-------------|-------|-------------|
| Not Found | 404 | N/A (automatic) | Requested page does not exist |
| Access Denied | 403 | N/A (automatic) | User lacks permission to access the resource |
| System Error | 500 | N/A (automatic) | Unexpected server error |
| Invalid Parameters | 400 | N/A (automatic) | Request contains invalid or malformed parameters |

---

## Not Found View

Displayed when a user navigates to a route that does not exist. Also used as the display for 403 Access Denied (see below).

### Layout

| Element | Description |
|---------|-------------|
| Error Icon | Search icon |
| Heading | "Page Not Found" |
| Message | "The page you're looking for doesn't exist or has been moved." |
| Home Link | Button/link to return to the Storefront |

### Logging

| Field | Value |
|-------|-------|
| Level | WARN |
| Message | Requested path, user ID (if authenticated), timestamp |

---

## Access Denied View

Displayed when an authenticated user attempts to access a resource they don't have permission for. **For security, this view is disguised as a 404 page** — it returns HTTP status `SC_NOT_FOUND` and shows the same content as the Not Found view to prevent information disclosure about protected resources.

### Layout

| Element | Description |
|---------|-------------|
| Error Icon | Search icon (same as 404) |
| Heading | "Page Not Found" (same as 404) |
| Message | Same message as Not Found view |
| Home Link | Button/link to return to the Storefront |

### Logging

| Field | Value |
|-------|-------|
| Level | WARN |
| Message | Requested path, user ID, user role, timestamp (logged server-side only) |

---

## System Error View

Displayed when an unexpected server error occurs.

### Layout

| Element | Description |
|---------|-------------|
| Error Icon | Warning icon |
| Heading | "Something Went Wrong" |
| Message | Generic message — no stack traces or internal details exposed to the user |
| Error Reference | UUID-based error reference ID for support correlation |
| Home Link | Button/link to return to the Storefront |
| Retry Option | Page reload button |

### Logging

| Field | Value |
|-------|-------|
| Level | ERROR |
| Message | Exception type, message, stack trace, user ID, request details, timestamp |
| Error ID | Unique identifier displayed to user for correlation |

---

## Invalid Parameters View

Displayed when request parameters are invalid, malformed, or fail validation. Only shown if no 403 error is possible (otherwise displayed as 404 for security).

### Layout

| Element | Description |
|---------|-------------|
| Error Icon | Exclamation circle icon |
| Heading | "Invalid Request" |
| Message | "The request contained invalid parameters." |
| Details | Validation error details (when safe to display) |
| Home Link | Button/link to return to the Storefront |

### Logging

| Field | Value |
|-------|-------|
| Level | WARN |
| Message | Parameter names, validation errors, user ID, timestamp |

---

## Related Documentation

- [Authentication](../security/authentication.md) - Login error handling
- [Authorization](../security/authorization.md) - Access control and denied scenarios
