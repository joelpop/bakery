# OrderEntity

Represents a customer order.

**Table**: `orders`

**Package**: `bakery-jpamodel.entity`

---

## Fields

| Field | Type | Nullable | Unique | Description |
|-------|------|----------|--------|-------------|
| id | Long | No | Yes | Primary key (inherited from AbstractEntity) |
| version | Integer | No | No | Optimistic locking version (inherited) |
| status | OrderStatusCode | No | No | Current order status (default: NEW) |
| dueDate | LocalDate | No | No | Scheduled pickup date |
| dueTime | LocalTime | No | No | Scheduled pickup time |
| additionalDetails | String | Yes | No | Special instructions from customer |
| total | BigDecimal(10,2) | No | No | Calculated order total |
| discount | BigDecimal(10,2) | Yes | No | Discount amount applied to order |
| paid | Boolean | No | No | Whether payment has been received (default: false) |
| createdAt | LocalDateTime | No | No | Timestamp when order was created |
| updatedAt | LocalDateTime | Yes | No | Timestamp of last modification |

### Payment Notes

The `paid` field is deliberately separate from `status` because:
- Orders can be picked up but not yet paid (e.g., to be billed later)
- Orders can be paid before pickup (e.g., prepayment)
- Payment status and fulfillment status are independent concerns

---

## Relationships

| Relationship | Target | Type | Cascade | Description |
|--------------|--------|------|---------|-------------|
| customer | CustomerEntity | Many-to-One | - | Customer who placed the order |
| location | [LocationEntity](location.md) | Many-to-One | - | Pickup location (Café or Bakery) |
| items | OrderItemEntity | One-to-Many | ALL, orphan removal | Line items in the order |
| createdBy | UserEntity | Many-to-One | - | User who created the order |
| updatedBy | UserEntity | Many-to-One | - | User who last modified the order |

---

## Lifecycle Callbacks

| Callback | Behavior |
|----------|----------|
| PrePersist | Sets `createdAt` to current timestamp |
| PreUpdate | Sets `updatedAt` to current timestamp |

---

## Codes

| Code | Description |
|------|-------------|
| [OrderStatusCode](../codes/order-status.md) | Order lifecycle states (NEW, VERIFIED, NOT_OK, CANCELLED, IN_PROGRESS, BAKED, PACKAGED, READY_FOR_PICK_UP, PICKED_UP) |

---

## Projections

### OrderListProjection

**Package**: `bakery-jpamodel.projection`

Order data for storefront list display with items.

| Method | Return Type | Description |
|--------|-------------|-------------|
| getId() | Long | Order ID |
| getStatus() | OrderStatusCode | Current status |
| getDueDate() | LocalDate | Pickup date |
| getDueTime() | LocalTime | Pickup time |
| getLocationName() | String | Location name (from joined location) |
| getCustomerName() | String | Customer name (from joined customer) |
| isPaid() | Boolean | Payment status |
| getItems() | List&lt;OrderItemSummaryProjection&gt; | Order items |

**Used by**: Storefront order list view

---

### OrderDashboardProjection

**Package**: `bakery-jpamodel.projection`

Order data for dashboard upcoming orders widget.

| Method | Return Type | Description |
|--------|-------------|-------------|
| getId() | Long | Order ID |
| getStatus() | OrderStatusCode | Current status |
| getDueDate() | LocalDate | Pickup date |
| getDueTime() | LocalTime | Pickup time |
| getLocationName() | String | Location name (from joined location) |
| getCustomerName() | String | Customer name |

**Used by**: Dashboard upcoming orders widget

---

### OrderTimeProjection

**Package**: `bakery-jpamodel.projection`

Minimal order data for time-based dashboard queries.

| Method | Return Type | Description |
|--------|-------------|-------------|
| getDueTime() | LocalTime | Pickup time |
| getCreatedAt() | LocalDateTime | Creation timestamp |

**Used by**: Dashboard KPI calculations (next pickup time, last new order time)

---

## Indexes

| Columns | Purpose |
|---------|---------|
| (due_date, due_time) | Storefront list queries, sorted by pickup time |
| (status) | Status filter queries |
| (customer_id) | Customer order history |
| (location_id) | Location order queries |
| (paid) | Payment status filtering |

---

## Related Documentation

- [OrderStatusCode](../codes/order-status.md) - Order status enum
- [OrderItemEntity](order-item.md) - Order line items
- [Orders Feature](../../features/orders.md) - Order workflow details
