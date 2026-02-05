# Storefront View

The Storefront is the primary view for managing customer orders. It provides a chronological list of orders with filtering capabilities and order creation functionality.

**Route**: `/storefront`

**Access**: All authenticated users (Admin, Baker, Barista)

## Screenshots
- Desktop: `legacy/images/storefront view/Desktop, order list.png`
- Desktop with filters: `legacy/images/storefront view/Desktop, order list, filters active.png`
- Phone: `legacy/images/storefront view/Phone, order list.png`

---

## Order List

### Layout
Orders are displayed in a card-based list, grouped by time period:
- **Today** - Orders due today with specific date shown
- **Tomorrow** - Orders due tomorrow
- **This Week** - Orders due within the current week (date range shown)
- **Upcoming** - Orders after the current week

### Order Card Information
Each order card displays:

| Field | Description | Example |
|-------|-------------|---------|
| Status Badge | Current order status | "New", "Verified", "Baked", "Ready for Pick Up" |
| Paid Indicator | Payment status | Checkmark or empty |
| Time | Pickup time | "12:00 p.m.", "08:00 a.m." |
| Location | Pickup location | "Café", "Bakery" |
| Customer Name | Full customer name | "Carolyn Christensen" |
| Order Items | Products with quantities | "1 Princess Cake, 12 ppl" |

### Card Actions
Clicking an order card opens the order detail view where staff can:
- View full order information
- Update status (progress to next state)
- Mark as paid
- Edit order details (if not yet picked up)

---

## Filtering

### Filter Bar
Located at the top of the view with:
- **Search/Filter Input** - Text field with dropdown for filter options
- **Show past orders** - Checkbox to include completed/cancelled orders
- **Clear filters** - Link to reset all filters

### Filter Options
The filter dropdown includes:
- **Status filters**: New, Verified, In Progress, Baked, etc.
- **Customer filters**: List of customer names
- **Paid status**: Paid / Unpaid

### Applied Filters
When filters are active:
- Selected filters appear as chips in the search bar
- Results update immediately
- "Clear filters" link becomes visible

---

## New Order Button

A prominent "+ New order" button is accessible from all states of the view.

**Design rationale**: Entering a new order should be fast regardless of the current application state, as orders may come in while working on other tasks.

---

## New Order Dialog

A single-page dialog for creating new orders, optimized for speed.

### Screenshots
- Empty form: `legacy/images/storefront view/new order dialog/Desktop, new order.png`
- Filled form: `legacy/images/storefront view/new order dialog/Desktop, new order, filled.png`

### Dialog Layout

#### Customer Section (Required)

The customer section is positioned first to create a personal, welcoming interaction. Phone number entry enables quick lookup of returning customers.

| Field | Type | Description |
|-------|------|-------------|
| Phone Number | Phone Input | Customer contact number (required); triggers autofill popup |
| Customer Name | Text Input | Customer's full name; read-only for existing customers |

**Phone Number Autofill Popup**

As the order taker (OT) types in the phone number field, a popup displays matching customers:
- Matches are found by partial phone number comparison (ignoring all punctuation)
- Each popup entry shows the phone number and customer name
- Selecting an entry populates both the phone number and customer name fields
- The popup updates dynamically as digits are typed

**Phone Number Formatting**

When leaving the phone number field with a new (non-matching) number:
- The number is formatted according to the country code provided
- If no country code is provided, the location's default country code is used
- If only 7 digits are entered, the location's default area code is prepended

**Customer Name Field Behavior**

| Scenario | Name Field State |
|----------|------------------|
| Initial state (empty phone) | Read-only |
| Existing customer selected from popup | Read-only (populated with customer name) |
| New phone number entered | Read-write (OT enters new customer name) |
| Phone changed from new to existing customer | Returns to read-only (updates to selected customer name) |

**Field Navigation**

- When an existing customer is selected, the customer name field is skipped (focus moves to next section)
- When a new phone number is entered, focus moves to the customer name field for entry

#### Pickup Section (Required)

| Field | Type | Description |
|-------|------|-------------|
| Location | Dropdown | Pickup location (auto-selected if only one active) |
| Due Date | Date Picker | Pickup date (defaults to today, min: today) |
| Due Time | Time Picker | Pickup time in hourly slots (08:00, 09:00, etc.) |
| Additional Details | Text Area | Special instructions (optional) |

#### Products Section (Required)

| Field | Type | Description |
|-------|------|-------------|
| Product | Combo Box | Select from available products (with autocomplete) |
| Quantity | Number Stepper (+/-) | Number of items (default: 1, min: 1) |
| Notes | Text Input | Per-item instructions (optional) |
| Add Button | Icon Button | Adds item to order |

- Items appear in a grid showing Product, Qty, Total, and Remove button
- Multiple products can be added to a single order
- At least one item required to save

**Note**: No separate product catalog is visible during order entry; staff and customers refer to an offline catalog.

#### Totals Section

| Field | Type | Description |
|-------|------|-------------|
| Discount | Currency Input | Order discount amount (optional) |
| Total | Display | Calculated total (sum of items minus discount) |

### Actions

- **Cancel** - Close dialog without saving
- **Save** - Validate and create order with "New" status

### Validation

- Customer name required
- Pickup location required
- Due date required (cannot be in past)
- Due time required
- At least one item required

---

## Order Detail View

Clicking an order opens a detail panel with full order information and actions.

### Status Progression

Staff can advance the order to the next status:

| Current Status | Next Action | Actor |
|----------------|-------------|-------|
| New | Verify order | Baker |
| Verified | Start production | Baker |
| In Progress | Mark as baked | Baker |
| Baked | Mark as packaged | Baker |
| Packaged | Mark as ready for pickup | Baker/Barista |
| Ready for Pick Up | Mark as picked up | Barista |

### Payment

- **Mark as Paid** button available at any status
- Payment status is independent of fulfillment status
- Orders can be picked up and billed later, or prepaid

### Problem Handling

- **Mark as Not OK** button available at New, Verified, or In Progress status
- Prompts for problem description
- Customer may need to be contacted

---

## Handling Pickup

When a customer arrives to collect their order:

1. **Find order** - Search by customer name, phone, or browse today's "Ready for Pick Up" orders
2. **Verify identity** - Confirm customer details
3. **Mark as Picked Up** - Update status
4. **Mark as Paid** - Update payment (if not prepaid)

---

## Direct Order Links

Orders can be referenced by URL: `/storefront/{orderId}`

This allows staff to share order links: "Take a look at order #234"

The storefront view will open with the specified order selected/expanded.

---

## Responsive Behavior

### Phone Layout
- Full-screen order list (no sidebar)
- Filter input at bottom with "+ New order" button
- Bottom navigation bar for view switching
- New order dialog takes full screen
- Date/time pickers use native mobile controls

---

## Related Documentation

- [Orders Feature](../features/orders.md) - Order workflow and data model
- [OrderStatusCode](../persistence/model/codes/order-status.md) - Status enum values
