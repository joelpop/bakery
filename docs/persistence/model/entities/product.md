# ProductEntity

Represents bakery products available for order.

**Table**: `products`

**Package**: `bakery-jpamodel.entity`

---

## Fields

| Field | Type | Nullable | Unique | Description |
|-------|------|----------|--------|-------------|
| id | Long | No | Yes | Primary key (inherited from AbstractEntity) |
| version | Integer | No | No | Optimistic locking version (inherited) |
| name | String | No | Yes | Product display name |
| description | String | Yes | No | Product description |
| size | String | No | No | Serving size indicator (e.g., "12 ppl", "individual") |
| price | BigDecimal(10,2) | No | No | Unit price |
| available | boolean | No | No | Whether product can be ordered (default: true) |
| photo | byte[] | Yes | No | Product image binary data |
| photoContentType | String | Yes | No | MIME type of photo |

---

## Relationships

None (referenced by OrderItemEntity).

---

## Projections

### ProductSummaryProjection

**Package**: `bakery-jpamodel.projection`

Product data for admin grid display with full details.

| Method | Return Type | Description |
|--------|-------------|-------------|
| getId() | Long | Product ID |
| getName() | String | Product name |
| getSize() | String | Serving size |
| getPrice() | BigDecimal | Unit price |
| isAvailable() | boolean | Availability status |

**Used by**: Product management grid in admin view

---

### ProductSelectProjection

**Package**: `bakery-jpamodel.projection`

Minimal product data for order form dropdown selection.

| Method | Return Type | Description |
|--------|-------------|-------------|
| getId() | Long | Product ID |
| getName() | String | Product name |
| getSize() | String | Serving size |
| getPrice() | BigDecimal | Unit price |

**Used by**: Product dropdown in new order dialog

---

## Indexes

| Columns | Purpose |
|---------|---------|
| (name) | Unique constraint, product search |
| (available) | Available product queries |
