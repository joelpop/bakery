# Orders

This document describes the order data model, statuses, and workflow throughout the application.

## Order Statuses

Orders progress through several statuses during their lifecycle:

| Status | Badge Color | Description |
|--------|-------------|-------------|
| **New** | Blue | Order just received, not yet reviewed |
| **Verified** | Primary | Order reviewed and accepted by bakery |
| **Not OK** | Orange/Warning | Problem requiring attention; customer may need contact |
| **Cancelled** | Gray | Order cancelled, no further action needed |
| **In Progress** | Primary | Being manufactured at bakery |
| **Baked** | Light Green | Baking completed |
| **Packaged** | Green | Packaged and ready for transport |
| **Ready for Pick Up** | Bright Green | Available at café for customer pickup |
| **Picked Up** | Gray | Order complete, picked up by customer |

### Status Workflow

```
┌─────────┐     ┌──────────┐     ┌─────────────┐     ┌────────┐
│   New   │────>│ Verified │────>│ In Progress │────>│ Baked  │
└─────────┘     └──────────┘     └─────────────┘     └────────┘
                     │                                    │
                     v                                    v
                ┌─────────┐                          ┌──────────┐
                │ Not OK  │                          │ Packaged │
                └─────────┘                          └──────────┘
                     │                                    │
                     v                                    v
               ┌───────────┐                    ┌─────────────────┐
               │ Cancelled │                    │ Ready for Pick  │
               └───────────┘                    │       Up        │
                                                └─────────────────┘
                                                         │
                                                         v
                                                   ┌───────────┐
                                                   │ Picked Up │
                                                   └───────────┘
```

### Status Transitions

| From | To | Trigger |
|------|----|---------|
| New | Verified | Bakery reviews and accepts order |
| New | Not OK | Problem identified before verification |
| Verified | In Progress | Production begins |
| Verified | Not OK | Problem identified |
| Not OK | Verified | Issue resolved |
| Not OK | Cancelled | Order cannot be fulfilled |
| In Progress | Baked | Baking completed |
| Baked | Packaged | Order packaged for transport |
| Packaged | Ready for Pick Up | Order delivered to café |
| Ready for Pick Up | Picked Up | Customer picks up order |

---

## Payment Status

Payment is tracked separately from order status via the `paid` boolean field.

### Why Paid is Separate

- Orders can be picked up but not yet paid (e.g., billing later)
- Orders can be paid before pickup (e.g., prepayment)
- Payment and fulfillment are independent business concerns

### Payment Handling

| Scenario | Status | Paid |
|----------|--------|------|
| Order placed, awaiting pickup | Ready for Pick Up | false |
| Prepaid order | Ready for Pick Up | true |
| Picked up, to be billed | Picked Up | false |
| Normal completion | Picked Up | true |

---

## Order Data Model

### Order

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Unique order identifier |
| status | OrderStatus | Current workflow status |
| dueDate | LocalDate | Scheduled pickup date |
| dueTime | LocalTime | Scheduled pickup time |
| location | Location | Pickup location (Café or Bakery) |
| customer | Customer | Customer details (for autofill and contact) |
| additionalDetails | String | Special instructions (optional) |
| items | List&lt;OrderItem&gt; | Line items |
| total | BigDecimal | Calculated order total |
| discount | BigDecimal | Discount amount (optional) |
| paid | Boolean | Payment received |
| createdAt | LocalDateTime | Order creation timestamp |
| createdBy | User | User who created the order |
| updatedAt | LocalDateTime | Last modification timestamp |
| updatedBy | User | User who last modified the order |

### Order Item

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Item identifier |
| order | Order | Parent order |
| product | Product | Product reference |
| quantity | Integer | Number of items |
| details | String | Per-item notes (optional, e.g., customizations) |
| unitPrice | BigDecimal | Price at order time (snapshot) |
| lineTotal | BigDecimal | quantity × unitPrice |

### Customer

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Customer identifier |
| name | String | Full name |
| phoneNumber | String | Contact phone |
| email | String | Email (optional) |

**Note**: Customers do not have system access. Customer data is used for autofill when placing orders and for contact purposes.

---

## Order Locations

Orders are picked up at one of the configured locations:

| Location | Description |
|----------|-------------|
| **Café** | Main retail storefront in city center |
| **Bakery** | Production facility outside city center |

Locations are managed via the [Locations view](../views/locations.md).

---

## Order Timeline Grouping

Orders in the storefront are displayed grouped by pickup date:

| Group | Description | Example |
|-------|-------------|---------|
| **Today** | Orders due today | "Today Tue, Jun 6" |
| **Tomorrow** | Orders due tomorrow | "Tomorrow Wed, Jun 7" |
| **This Week** | Orders due later this week | "This week Thu, Jun 8 – Sun, Jun 11" |
| **Upcoming** | Orders after this week | "Upcoming After this week" |

### Sorting

Within each group, orders are sorted by:
1. Due time (ascending)
2. Customer name (alphabetical, secondary)

---

## Order Filtering

### Available Filters

| Filter | Type | Options |
|--------|------|---------|
| Status | Multi-select chips | New, Verified, In Progress, etc. |
| Customer | Searchable dropdown | Customer names |
| Show past orders | Checkbox | Include completed/cancelled orders |
| Paid | Checkbox | Filter by payment status |

### Filter Behavior

- Filters are applied immediately (no submit button)
- Multiple filters combine with AND logic
- "Clear filters" resets all filters
- Filter state may persist in URL for sharing/bookmarking

---

## Creating Orders

### Order Entry Dialog

Orders are created via a single-page dialog accessible from the "+ New order" button. The dialog is designed for speed during customer interactions.

### Field Order

1. **Customer** - Name and phone number (phone triggers autofill for returning customers)
2. **Pickup details** - Location, date, time, and any special instructions
3. **Products** - Add items with quantities and per-item notes
4. **Totals** - Discount (optional) and calculated total

### Required Fields

- Customer name
- Pickup location
- Due date and time
- At least one product

### Optional Fields

- Customer phone number
- Additional details (special instructions)
- Per-item notes
- Discount

### Validation

- Due date cannot be in the past
- Product quantities must be positive integers (minimum 1)
- At least one item must be added

### Customer Autofill

When entering a phone number that matches an existing customer, the customer name field is automatically populated. This speeds up order entry for returning customers and reduces errors.

---

## Handling Pickup

When a customer arrives to collect their order:

1. **Find order** - Search by customer name, phone, or order ID
2. **Verify identity** - Confirm customer details
3. **Mark as Picked Up** - Update status to Picked Up
4. **Mark as Paid** - Update paid flag (if not prepaid)

---

## Problem Handling

When an issue is identified with an order:

1. Change status to **Not OK**
2. Record the problem in notes/additional details
3. Contact customer if needed
4. Either:
   - Resolve issue and return to **Verified** status
   - Cancel order if unresolvable

### Common Problems

- Ingredient unavailable
- Short notice for complex order
- Misread/misunderstood specifications
- Customer requested change

---

## Order History

### Past Orders

- Accessed via "Show past orders" filter
- Includes Picked Up and Cancelled orders
- Read-only (cannot modify completed orders)

### Audit Trail

Each order maintains history of:
- Status changes with timestamps
- Who made changes
- Original vs modified values

---

## Direct Order Links

Orders can be referenced directly via URL for communication:

- URL pattern: `/storefront/{orderId}`
- Allows staff to share specific orders: "Take a look at order #234"
- Link opens storefront view with the specified order selected

---

## Order Editing Permissions

Editing permissions vary by role and order status:

| Role | Before Production (NEW, VERIFIED) | During/After Production | After Completion |
|------|-----------------------------------|-------------------------|------------------|
| Admin | Full edit | Full edit | Read-only |
| Baker | Add notes only | Add notes only | Read-only |
| Barista | Add notes only | Add notes only | Read-only |

**Notes**:
- "Production" starts at IN_PROGRESS status
- "Completion" means PICKED_UP or CANCELLED status
- Order notes can be added by Baker or Admin until the order is picked up or cancelled
- Only Admin can modify order details (items, customer, dates) once production has started

---

## Related Documentation

- [Storefront View](../views/storefront.md) - Order list and creation UI
- [OrderStatusCode](../persistence/model/codes/order-status.md) - Status enum values
- [OrderEntity](../persistence/model/entities/order.md) - Database entity
