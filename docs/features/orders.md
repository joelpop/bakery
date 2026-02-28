# Orders

This document describes the order data model, statuses, and workflow throughout the application.

## Order Statuses

Orders progress through several statuses during their lifecycle. Pre-production statuses are **derived from item statuses** (see [Order Status Roll-Up](bakery-workflow.md#order-status-roll-up)); post-production statuses are manual staff actions.

| Status | Badge Color | Description |
|--------|-------------|-------------|
| **In Review** | Blue | Order needs review — newly received or item(s) flagged with a problem |
| **Verified** | Primary | Order reviewed and accepted — all items accepted |
| **In Progress** | Primary | Being manufactured — at least one item in production |
| **Produced** | Light Green | Production completed — all non-canceled items produced |
| **Packaged** | Green | Packaged for transport |
| **In Transit** | Green | Being transported to pickup location |
| **Ready for Pick Up** | Bright Green | Available at café for customer pickup |
| **Picked Up** | Gray | Order complete, picked up by customer |
| **Canceled** | Gray | Order will not be fulfilled |

**Renames**: In Review (was New), Produced (was Baked), Canceled (was Cancelled). Not OK has been removed — its function is absorbed by In Review.

### Status Workflow

```
[Derived from item statuses]                   [Manual post-production]
IN_REVIEW → VERIFIED → IN_PROGRESS → PRODUCED → PACKAGED → IN_TRANSIT → READY_FOR_PICK_UP → PICKED_UP
     ↑           │
     └───────────┘ (item rejected on bakery board)
     │
     ↓
  CANCELED
```

### Pre-Production Transitions (Derived)

Pre-production order statuses are computed from the order's item statuses. Bakers transition items on the [Bakery board](bakery-workflow.md); the order status follows automatically.

| Priority | Condition | Order Status |
|----------|-----------|--------------|
| 1 | All items CANCELED | CANCELED |
| 2 | Any item PENDING_REVIEW or REJECTED | IN_REVIEW |
| 3 | All items ACCEPTED or CANCELED | VERIFIED |
| 4 | Any item IN_PROGRESS | IN_PROGRESS |
| 5 | All non-canceled items PRODUCED | PRODUCED |

### Post-Production Transitions (Manual)

| From | To | Trigger | Actor |
|------|----|---------|-------|
| Produced | Packaged | Order packaged for transport | Baker |
| Packaged | In Transit | Order dispatched to pickup location | Baker |
| In Transit | Ready for Pick Up | Order received at pickup location | Barista |
| Ready for Pick Up | Picked Up | Customer collects order | Barista, Admin |

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
| status | OrderItemStatus | Item lifecycle status (PENDING_REVIEW, ACCEPTED, IN_PROGRESS, PRODUCED, REJECTED, CANCELED) |
| quantity | Integer | Number of items |
| details | String | Per-item notes (optional, e.g., customizations) |
| unitPrice | BigDecimal | Price at order time (snapshot) |
| lineTotal | BigDecimal | quantity × unitPrice |

Item statuses are managed on the [Bakery production board](bakery-workflow.md) and drive the order's pre-production status via [roll-up rules](bakery-workflow.md#order-status-roll-up). When a new order is created, all items enter PENDING_REVIEW.

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
| Status | Multi-select chips | In Review, Verified, In Progress, etc. |
| Customer | Searchable dropdown | Customer names |
| Show past orders | Checkbox | Include completed/canceled orders |
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

### Dialog Layout

The dialog header contains:
- **Title**: "New Order"
- **Pickup Location**: Dropdown selector (pre-populated from current working location)

The dialog body contains:
1. **Customer** - Phone number and name fields (phone-first for quick lookup)
2. **Pickup Date/Time** - Date picker and time picker (15-minute increments)
3. **Items Section** - Product entry and items grid
4. **Totals** - Subtotal, discount, and total
5. **Additional Details** - Special instructions (optional, at bottom)

### Required Fields

- Customer phone number
- Customer name
- Pickup location (in header)
- Pickup date and time
- At least one product

### Optional Fields

- Additional details (special instructions)
- Per-item notes
- Discount (% or $)

### Validation

- Pickup date cannot be in the past
- Product quantities must be positive integers (minimum 1)
- At least one item must be added
- Discount cannot be negative
- Discount cannot exceed subtotal

### Current Working Location

The application maintains a "current working location" for each user session:
- Initializes from the user's primary location on login
- Can be changed via the location selector in the main navigation header
- Pre-populates the pickup location in new order dialogs
- Provides a "Current Location" filter option in the Storefront view

### Customer Phone Number Entry

The phone number field is first, making the interaction more personal and welcoming. As the order taker (OT) types:

1. **Autofill Popup**: Partially matching phone numbers (ignoring punctuation) display in a popup with customer names
2. **Selection**: Selecting a match populates both phone and name fields; name becomes read-only
3. **New Number**: If no match, leaving the field formats the number and enables the name field for entry

**Phone Number Formatting Rules**:
- Numbers are formatted based on the country code provided
- If no country code: uses the pickup location's default country code
- If only 7 digits: prepends the pickup location's default area code

### Customer Name Field Behavior

| Phone Number Status | Name Field | Notes |
|---------------------|------------|-------|
| Empty | Read-only | Waiting for phone entry |
| Existing customer selected | Read-only | Auto-populated from customer record |
| New number entered | Read-write | OT enters new customer name |
| Changed to existing customer | Read-only | Updates to selected customer name |

When an existing customer is selected, the name field is skipped and focus moves to the pickup date field.

### Adding Items

The item entry section consists of:
- **Product dropdown** - Shows product name, size, and price in dropdown list
- **Qty field** - Integer field with stepper buttons (min 1, max 99)
- **Add button** - Plus icon to add the item
- **Notes field** - Optional special instructions for the item

**Item Entry Flow**:
1. Select product from dropdown (price shown in list)
2. Adjust quantity if needed (defaults to 1)
3. Add notes if needed (e.g., "extra crispy", "no nuts")
4. Click the plus button to add

**Combining Duplicate Items**: When adding an item with the same product and notes as an existing item, the quantities are combined instead of creating a duplicate line.

### Items Grid

The items grid displays added items with columns:
- **Product** - Two-line display: product name (size) on first line, notes on second line (if any)
- **Qty** - Right-aligned with tabular numbers for vertical alignment
- **Total** - Right-aligned line total with tabular numbers
- **Remove** - Trash icon button to delete the item

### Editing Items

To edit an existing item's quantity or notes:

1. **Click/tap the row** to select it for editing
2. Fields populate: Product (disabled), Qty, and Notes show current values
3. **Button changes**: Plus icon becomes a checkmark
4. **Modify values** as needed
5. **Click checkmark** to save changes, or **click the row again** to cancel

**Combining on Edit**: If editing notes causes the item to match another item with the same product and notes, the items are combined (quantities added, edited item removed).

**Focus Management**:
- Selecting a row focuses the Qty field
- After adding/updating or canceling, focus returns to the Product dropdown

### Discount

The discount section allows applying either a percentage or dollar amount discount:

- **Type selector**: Radio buttons for "%" (default) or "$"
- **Value field**: Right-aligned number input
- **Calculated amount**: Shows the dollar amount (e.g., "-$10.00" or "$0.00")

**Validation**:
- Discount cannot be negative
- Discount cannot exceed the subtotal

### Totals Display

The totals section shows:
- **Subtotal**: Sum of all line totals
- **Discount**: Type selector, value input, and calculated amount
- **Total**: Final amount after discount

All monetary values use tabular numbers for proper vertical alignment.

---

## Handling Pickup

When a customer arrives to collect their order:

1. **Find order** - Search by customer name, phone, or order ID
2. **Verify identity** - Confirm customer details
3. **Mark as Picked Up** - Update status to Picked Up
4. **Mark as Paid** - Update paid flag (if not prepaid)

---

## Problem Handling

Problems are handled at the **item level** on the [Bakery board](bakery-workflow.md), not at the order level:

1. Baker **rejects** an item on the bakery board (item → REJECTED) with a **required message** explaining the issue (e.g., "Out of pink sugarcoating", "Instructions unclear")
2. Order status automatically drops to **In Review**; other accepted items are [held](bakery-workflow.md#hold-behavior)
3. The order appears in the **"Needs Attention"** group at the top of the Storefront with a pink background
4. The order detail view shows each item's current status; rejected items display **Resolve** and **Cancel Item** buttons
5. Storefront staff reviews the baker's rejection message in the activity timeline, then:
   - **Resolve**: Optionally edits the order to fix the issue, clicks Resolve, enters a required message explaining the correction → item returns to Pending Review
   - **Cancel Item**: Clicks Cancel Item, enters a required message explaining why → item moves to Canceled
   - **Cancel Order**: Cancels the entire order if no longer viable

Both resolve and cancel actions require a message, which is posted to the activity timeline referencing the item. Once all rejected items are addressed, the order recalculates per [roll-up rules](bakery-workflow.md#order-status-roll-up) and returns to its normal position in the storefront.

### Common Problems

- Ingredient unavailable for a specific item
- Short notice for complex order
- Misread/misunderstood specifications
- Customer requested change to an item

---

## Order History

### Past Orders

- Accessed via "Show past orders" filter
- Includes Picked Up and Canceled orders
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

| Role | Before Production (IN_REVIEW, VERIFIED) | During/After Production | After Completion |
|------|------------------------------------------|-------------------------|------------------|
| Admin | Full edit | Full edit | Read-only |
| Baker | Add notes only | Add notes only | Read-only |
| Barista | Add notes only | Add notes only | Read-only |

**Notes**:
- "Production" starts at IN_PROGRESS status
- "Completion" means PICKED_UP or CANCELED status
- Order notes can be added by Baker or Admin until the order is picked up or canceled
- Only Admin can modify order details (items, customer, dates) once production has started

---

## Related Documentation

- [Storefront View](../views/storefront.md) - Order list and creation UI
- [Bakery Workflow](bakery-workflow.md) - Item-level production workflow and order status roll-up
- [OrderStatusCode](../persistence/model/codes/order-status.md) - Order status enum values
- [OrderItemStatusCode](../persistence/model/codes/order-item-status.md) - Item status enum values
- [OrderEntity](../persistence/model/entities/order.md) - Database entity
