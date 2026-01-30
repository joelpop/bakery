# UserEntity

Represents application users (staff members).

**Table**: `users`

**Package**: `bakery-jpamodel.entity`

---

## Fields

| Field | Type | Nullable | Unique | Description |
|-------|------|----------|--------|-------------|
| id | Long | No | Yes | Primary key (inherited from AbstractEntity) |
| version | Integer | No | No | Optimistic locking version (inherited) |
| email | String | No | Yes | Login identifier, used for authentication |
| firstName | String | No | No | User's first name |
| lastName | String | No | No | User's last name |
| passwordHash | String | No | No | BCrypt-hashed password |
| role | UserRoleCode | No | No | User's role determining access permissions |
| photo | byte[] | Yes | No | Profile photo binary data |
| photoContentType | String | Yes | No | MIME type of photo (e.g., "image/jpeg") |

---

## Relationships

None (standalone entity).

---

## Codes

| Code | Description |
|------|-------------|
| [UserRoleCode](../codes/user-role.md) | User access levels (ADMIN, BAKER, BARISTA) |

---

## Projections

### UserSummaryProjection

**Package**: `bakery-jpamodel.projection`

Lightweight user data for list displays and grids.

| Method | Return Type | Description |
|--------|-------------|-------------|
| getId() | Long | User ID |
| getEmail() | String | Login email |
| getFirstName() | String | First name |
| getLastName() | String | Last name |
| getRole() | UserRoleCode | User role |

**Used by**: User list grid, user search results

---

## Indexes

| Columns | Purpose |
|---------|---------|
| (email) | Login queries, unique constraint |
