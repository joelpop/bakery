# Locations View

The Locations view provides administrators with the ability to manage pickup locations.

**Route**: `/locations`

**Access**: Admin only (`@RolesAllowed(UserRole.ROLE_ADMIN)`)

**Menu**: Under Admin section

---

## Layout

### Location List

A grid displaying all locations with the following columns:

| Column | Description | Sortable |
|--------|-------------|----------|
| Name | Display name of the location | Yes |
| Address | Physical address | No |
| Timezone | IANA timezone ID | No |
| Active | Whether location accepts new orders | Yes |
| Sort Order | Display order in dropdowns | Yes |

### Toolbar

| Element | Description |
|---------|-------------|
| + New location | Button to create a new location |

### Cross-Session Updates
The location grid auto-refreshes via shared signals when locations are modified in another session. Changed rows receive a temporary gold highlight animation.

---

## Location Dialog

A dialog for creating and editing locations. Uses delegation pattern (wraps Dialog rather than extending it). Includes stale data detection with a warning banner when external changes are detected.

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| Name | Text | Yes | Display name (e.g., "Downtown Store") |
| Address | Text Area | No | Physical address |
| Timezone | Text | Yes | IANA timezone ID for the location (e.g., "America/New_York") |
| Default Country Code | Text | Yes | Country code for phone formatting at this location (e.g., "+1") |
| Default Area Code | Text | Yes | Area code for 7-digit phone numbers (e.g., "212") |
| Active | Checkbox | Yes | Whether location is available |
| Sort Order | Number | Yes | Display order in lists |

### Validation

| Field | Rule |
|-------|------|
| Name | Required, unique |
| Sort Order | Required, positive integer |
| Active | At least one location must remain active |

---

## Actions

### Create Location

| Step | Description |
|------|-------------|
| 1 | Admin clicks "New Location" button |
| 2 | Empty dialog opens |
| 3 | Admin fills in location details |
| 4 | Admin clicks "Save" |
| 5 | Location is created and appears in the list |

### Edit Location

| Step | Description |
|------|-------------|
| 1 | Admin clicks on a location row or edit button |
| 2 | Dialog opens with location data |
| 3 | Admin modifies fields |
| 4 | Admin clicks "Save" |
| 5 | Changes are persisted |

### Deactivate Location

| Step | Description |
|------|-------------|
| 1 | Admin opens location for editing |
| 2 | Admin unchecks "Active" checkbox |
| 3 | Admin clicks "Save" |
| 4 | Location is no longer available for new orders |
| 5 | Existing orders at this location are unaffected |

### Delete Location

| Condition | Behavior |
|-----------|----------|
| No orders | Location can be deleted |
| Has orders | Location cannot be deleted; must be deactivated instead |

---

## Business Rules

| Rule | Description |
|------|-------------|
| Unique names | Each location must have a unique name |
| Active requirement | At least one location must remain active |
| Deletion protection | Locations with associated orders cannot be deleted |

---

## Related Documentation

- [LocationEntity](../persistence/model/entities/location.md) - Entity definition
- [Orders](../features/orders.md) - Orders reference locations
- [Storefront](storefront.md) - Location filter in order list
