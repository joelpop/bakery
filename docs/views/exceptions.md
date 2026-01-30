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

Displayed when a user navigates to a route that does not exist.

### Layout

| Element | Description |
|---------|-------------|
| Error Icon | Visual indicator (e.g., magnifying glass or 404 graphic) |
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

Displayed when an authenticated user attempts to access a resource they don't have permission for.

### Layout

| Element | Description |
|---------|-------------|
| Error Icon | Lock or shield icon |
| Heading | "Access Denied" |
| Message | "You don't have permission to view this page." |
| Home Link | Button/link to return to the Storefront |
| Contact Info | Suggestion to contact an administrator if access is needed |

### Logging

| Field | Value |
|-------|-------|
| Level | WARN |
| Message | Requested path, user ID, user role, timestamp |

---

## System Error View

Displayed when an unexpected server error occurs.

### Layout

| Element | Description |
|---------|-------------|
| Error Icon | Warning or error indicator |
| Heading | "Something Went Wrong" |
| Message | "We encountered an unexpected error. Our team has been notified." |
| Error Reference | Unique error ID for support reference |
| Home Link | Button/link to return to the Storefront |
| Retry Option | Option to retry the previous action (where applicable) |

### Logging

| Field | Value |
|-------|-------|
| Level | ERROR |
| Message | Exception type, message, stack trace, user ID, request details, timestamp |
| Error ID | Unique identifier displayed to user for correlation |

---

## Invalid Parameters View

Displayed when request parameters are invalid, malformed, or fail validation.

### Layout

| Element | Description |
|---------|-------------|
| Error Icon | Warning indicator |
| Heading | "Invalid Request" |
| Message | "The request contained invalid parameters." |
| Details | Specific validation errors (when safe to display) |
| Back Link | Button to go back or return to Storefront |

### Logging

| Field | Value |
|-------|-------|
| Level | WARN |
| Message | Parameter names, validation errors, user ID, timestamp |

---

## Related Documentation

- [Authentication](../security/authentication.md) - Login error handling
- [Authorization](../security/authorization.md) - Access control and denied scenarios
