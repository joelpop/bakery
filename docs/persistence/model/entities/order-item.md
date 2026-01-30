# OrderItemEntity

Represents a line item within an order.

**Table**: `order_items`

**Package**: `bakery-jpamodel.entity`

---

## Fields

| Field | Type | Nullable | Unique | Description |
|-------|------|----------|--------|-------------|
| id | Long | No | Yes | Primary key (inherited from AbstractEntity) |
| version | Integer | No | No | Optimistic locking version (inherited) |
| quantity | Integer | No | No | Number of items ordered |
| details | String | Yes | No | Per-item customization notes |
| unitPrice | BigDecimal(10,2) | No | No | Price snapshot at order time |
| lineTotal | BigDecimal(10,2) | No | No | Calculated: quantity × unitPrice |

---

## Relationships

| Relationship | Target | Type | Cascade | Description |
|--------------|--------|------|---------|-------------|
| order | OrderEntity | Many-to-One | - | Parent order |
| product | ProductEntity | Many-to-One | - | Product being ordered |

---

## Projections

### OrderItemSummaryProjection

**Package**: `bakery-jpamodel.projection`

Order item data for display in order lists.

| Method | Return Type | Description |
|--------|-------------|-------------|
| getQuantity() | Integer | Number of items |
| getProductName() | String | Product name (from joined product) |
| getProductSize() | String | Product size (from joined product) |

**Used by**: Order list items display, order card item summaries

---

## Indexes

| Columns | Purpose |
|---------|---------|
| (order_id) | Fetch items for an order |
| (product_id) | Product usage analytics |
