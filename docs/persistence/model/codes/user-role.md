# UserRoleCode

Defines the authorization role assigned to staff members.

**Package**: `bakery-jpamodel.code`

**Used by**: [UserEntity](../entities/user.md)

---

## Values

| Value | Description |
|-------|-------------|
| ADMIN | Full access to all features including user management and system configuration |
| BAKER | Access to bakery operations and order preparation; read-only access to products |
| BARISTA | Access to storefront operations and order fulfillment |

---

## Authorization

User roles control access to views and operations:

| Role | Accessible Views | Key Permissions |
|------|-----------------|-----------------|
| ADMIN | All views | Manage users, products, locations; edit orders at any status |
| BAKER | Dashboard, Storefront, Products (read-only) | Update order status, add order notes |
| BARISTA | Dashboard, Storefront | View and fulfill orders, add order notes |

**Note**: There is no role hierarchy. Each role has explicit permissions defined independently.

---

## Related Documentation

- [UserEntity](../entities/user.md) - Entity that uses this code
- [Authorization](../../security/authorization.md) - Role-based access control details
