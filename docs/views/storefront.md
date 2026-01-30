# Storefront View

The Storefront is the primary view for managing customer orders. It provides a chronological list of orders with filtering capabilities and order creation functionality.

**Route**: `/storefront`

**Access**: All authenticated users (Admin, Baker, Barista)

## Screenshots
- Desktop: `originals/images/storefront view/Desktop, order list.png`
- Desktop with filters: `originals/images/storefront view/Desktop, order list, filters active.png`
- Phone: `originals/images/storefront view/Phone, order list.png`

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

A two-step wizard for creating new orders, optimized for speed and error prevention.

### Screenshots
- Empty form: `originals/images/storefront view/new order dialog/Desktop, new order.png`
- Filled form: `originals/images/storefront view/new order dialog/Desktop, new order, filled.png`
- Review step: `originals/images/storefront view/new order dialog/Desktop, new order, review.png`

### Sell Flow Design

The order entry flow is deliberately sequenced:

1. **Due date first** - Allows immediate feasibility verification
   - "Due tomorrow and it's evening? Maybe no can do."
   - "Special order for day after tomorrow? Might need to double-check with bakery."

2. **Items and details** - Product selection with customizations

3. **Customer last** - Autofill speeds up entry and reduces errors

### Step 1: Order Details

#### Due Section (Required)

| Field | Type | Description |
|-------|------|-------------|
| Date | Date Picker | Pickup date (first field for feasibility check) |
| Time | Time Picker (Dropdown) | Pickup time (hourly slots: 08:00 a.m., 09:00 a.m., etc.) |
| Location | Dropdown | "Café" or "Bakery" |

#### Products Section

| Field | Type | Description |
|-------|------|-------------|
| Product | Combo Box (autocomplete) | Select from product catalog |
| Quantity | Number Stepper (+/-) | Number of items (default: 1) |
| Details | Text Input | Per-item notes (e.g., "White topping instead of pink") |
| Price | Display | Calculated price for line item |

- Multiple products can be added to a single order
- Each product line shows individual price
- Product autocomplete speeds up entry
- **Discount** field available for applying order discounts
- **Total** is calculated and displayed at bottom

**Note**: No separate product catalog is visible during order entry; staff and customers refer to an offline catalog.

#### Customer Section (Required)

| Field | Type | Description |
|-------|------|-------------|
| Customer | Combo Box (autocomplete) | Create new or select existing customer |
| Phone number | Phone Input | Customer contact (triggers autofill) |
| Additional details | Text Area | Special instructions (e.g., "Husband, Mark Torres, will pickup the order") |

**Customer Autofill**: Entering a phone number or name autocompletes existing customer information, speeding up the process and avoiding mistakes.

#### Actions
- **Cancel** - Close dialog without saving
- **Review order** - Proceed to review step (disabled until required fields complete)

### Step 2: Review Order

A read-only summary shown to verify with the customer before placement:

| Section | Content |
|---------|---------|
| Header | "New" status badge |
| Due | Date, day of week, time, location |
| Customer | Name, phone number |
| Additional details | Special instructions |
| Products | List with quantities, sizes, customizations, and prices |
| Discount | Applied discount (if any) |
| Total | Order total |

This summary can be shown to the customer (in person or read over the phone) to confirm accuracy.

#### Actions
- **Back** - Return to edit order details
- **Place order** - Submit the order (creates with "New" status)

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
