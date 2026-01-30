# LocationEntity

Represents a pickup location where customers can collect orders.

**Table**: `locations`

**Package**: `bakery-jpamodel.entity`

---

## Fields

| Field | Type | Nullable | Unique | Description |
|-------|------|----------|--------|-------------|
| id | Long | No | Yes | Primary key (inherited from AbstractEntity) |
| version | Integer | No | No | Optimistic locking version (inherited) |
| name | String(100) | No | Yes | Display name for the location |
| code | String(20) | No | Yes | Short code for the location (e.g., "STORE", "BAKERY") |
| address | String(500) | Yes | No | Physical address of the location |
| active | boolean | No | No | Whether the location is available for new orders |
| sortOrder | Integer | No | No | Display order in dropdowns and lists |

---

## Relationships

| Relationship | Target | Type | Cascade | Description |
|--------------|--------|------|---------|-------------|
| orders | OrderEntity | One-to-Many | - | Orders assigned to this location |

---

## Projections

### LocationSummaryProjection

**Package**: `bakery-jpamodel.projection`

Location data for dropdown selection.

| Method | Return Type | Description |
|--------|-------------|-------------|
| getId() | Long | Location ID |
| getName() | String | Display name |
| getCode() | String | Short code |

**Used by**: Order form location dropdown, storefront location filter

---

## Indexes

| Columns | Purpose |
|---------|---------|
| (code) | Lookup by code, unique constraint |
| (active, sort_order) | Active location list queries |

---

## Default Data

The application ships with default locations that can be modified by administrators:

| Name | Code | Description |
|------|------|-------------|
| Store | STORE | Main retail storefront |
| Bakery | BAKERY | Production facility |

---

## Related Documentation

- [OrderEntity](order.md) - Orders reference a location
- [Locations View](../../views/locations.md) - Admin interface for managing locations
