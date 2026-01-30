# Locations View

The Locations view provides administrators with the ability to manage pickup locations.

**Route**: `/locations`

**Access**: Admin only (`@RolesAllowed("ADMIN")`)

**Menu**: Under Admin section

---

## Layout

### Location List

A grid displaying all locations with the following columns:

| Column | Description | Sortable |
|--------|-------------|----------|
| Name | Display name of the location | Yes |
| Code | Short identifier code | Yes |
| Address | Physical address | No |
| Active | Whether location accepts new orders | Yes |
| Order | Sort order in dropdowns | Yes |

### Toolbar

| Element | Description |
|---------|-------------|
| Search | Filter locations by name or code |
| New Location | Button to create a new location |

---

## Location Dialog

A dialog for creating and editing locations.

### Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| Name | Text | Yes | Display name (e.g., "Downtown Store") |
| Code | Text | Yes | Short unique code (e.g., "DOWNTOWN") |
| Address | Text Area | No | Physical address |
| Active | Checkbox | Yes | Whether location is available |
| Sort Order | Number | Yes | Display order in lists |

### Validation

| Field | Rule |
|-------|------|
| Name | Required, unique, max 100 characters |
| Code | Required, unique, max 20 characters, uppercase letters and underscores only |
| Sort Order | Required, positive integer |

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
| Unique codes | Each location must have a unique code |
| Active requirement | At least one location must remain active |
| Deletion protection | Locations with associated orders cannot be deleted |

---

## Related Documentation

- [LocationEntity](../persistence/model/entities/location.md) - Entity definition
- [Orders](../features/orders.md) - Orders reference locations
- [Storefront](storefront.md) - Location filter in order list
