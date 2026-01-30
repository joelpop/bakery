# Security Overview

This document describes the security architecture for the Bakery application.

## Technology Stack

| Component | Technology |
|-----------|------------|
| Framework | Spring Security |
| Authentication | Form-based login with session management |
| Password Storage | BCrypt hashing |
| Authorization | Role-based access control (RBAC) |
| CSRF Protection | Enabled (Vaadin handles automatically) |

## Authentication Flow

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Login Form │────>│ Spring Security  │────>│  UserService    │
│             │     │  Filter Chain    │     │  (load user)    │
└─────────────┘     └──────────────────┘     └────────┬────────┘
                             │                        │
                             │                        ▼
┌─────────────┐     ┌────────▼─────────┐     ┌─────────────────┐
│  Dashboard  │<────│  Session Created │<────│  UserRepository │
│  (redirect) │     │  + SecurityContext│     │  (verify creds) │
└─────────────┘     └──────────────────┘     └─────────────────┘
```

## User Roles

| Role | Description | Access Level |
|------|-------------|--------------|
| `ADMIN` | Store managers | Full access to all views |
| `BAKER` | Kitchen staff | Storefront, Dashboard |
| `BARISTA` | Front-of-house | Storefront, Dashboard |

## View Access Matrix

| View | ADMIN | BAKER | BARISTA | Anonymous |
|------|-------|-------|---------|-----------|
| Login | ✓ | ✓ | ✓ | ✓ |
| Storefront | ✓ | ✓ | ✓ | ✗ |
| Dashboard | ✓ | ✓ | ✓ | ✗ |
| Users | ✓ | ✗ | ✗ | ✗ |
| Products | ✓ (edit) | ✓ (read-only) | ✗ | ✗ |
| Locations | ✓ | ✗ | ✗ | ✗ |
| Preferences | ✓ | ✓ | ✓ | ✗ |

## Default Landing Page

After login, users are redirected based on role:
- **Admin**: Dashboard (business overview)
- **Baker/Barista**: Storefront (order management)

## Security Configuration

See [Configuration](configuration.md) for implementation details.

## Related Documentation

- [Configuration](configuration.md) - Spring Security setup
- [Authentication](authentication.md) - Login, logout, session management
- [Authorization](authorization.md) - Role-based access control
