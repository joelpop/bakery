# Bakery Workflow

This document describes the bakery production workflow — how order items flow through status swimlanes in the Bakery view, how items are grouped for efficient batch production, and how groupings and their ordering are persisted.

## Purpose

Bakers work with **items, not orders**. A single baker doesn't care that ten different customers each ordered a chocolate cake — they care that there are 75 chocolate cakes to bake *today*. The bakery workflow groups identical items into aggregate tiles so bakers can plan production efficiently, then drag tiles through swimlanes as items progress.

---

## Item Statuses

Order items have their own lifecycle, separate from order-level statuses. The bakery workflow uses these item statuses:

| Status | Description |
|--------|-------------|
| **Pending Review** | Item awaiting review (newly received or re-entered after correction) |
| **Accepted** | Item reviewed and accepted, ready for production |
| **In Progress** | Item currently being manufactured |
| **Produced** | Item production completed |
| **Canceled** | Item will not be fulfilled |
| **Rejected** | Item flagged with a problem requiring storefront attention |

**Status renames:** `PENDING_REVIEW` replaces the previous `NEW` — not all items in this state are new (e.g., corrected rejections re-entering the workflow). `ACCEPTED` replaces the previous `VERIFIED` — for parallelism with `REJECTED`. `PRODUCED` replaces the previous `BAKED` — generic enough for non-baked products (e.g., no-bake cookies). `REJECTED` replaces the previous `NOT_OK`.

### Status Transitions

```
                    ┌───────────────────────────────────────────────┐
                    │              Bakery Board                     │
┌─────────┐     ┌──────────┐     ┌─────────────┐     ┌──────────┐ │
│ Pending │────>│ Accepted │────>│ In Progress │────>│ Produced │ │
│ Review  │     └──────────┘     └─────────────┘     └──────────┘ │
└─────────┘        │                                               │
 │  ^              │                                               │
 │  │              v                                               │
 │  │         ┌──────────┐                                         │
 │  └─────────│ Rejected │──────> Canceled                         │
 │ (from      └──────────┘       (from Storefront)                 │
 │ Storefront)     ^                                               │
 │                 │                                               │
 └─────────────────┘                                               │
                    └───────────────────────────────────────────────┘
```

#### Bakery Board Transitions (drag and drop)

| From | Valid Targets | Constraint |
|------|---------------|------------|
| Pending Review | Accepted, Rejected | Rejected requires a message |
| Accepted | In Progress, Rejected | In Progress only for items due **today**; Rejected requires a message |
| In Progress | Produced | |

**Today-only rule**: Only items due today can enter In Progress. Tomorrow's items can be accepted in advance but cannot start production until their due date. The In Progress drop target does not appear when dragging a future-dated tile.

**Rejection requires a message**: When a baker rejects an item (via drag or button), a prompt appears requiring a message explaining the reason (e.g., "Out of pink sugarcoating", "Instructions unclear — which tier size?"). The message is posted to the order's activity timeline referencing the specific item. The transition does not complete until the message is submitted.

#### Storefront Transitions (order detail view)

| From | Valid Targets | Description | Message Required |
|------|---------------|-------------|------------------|
| Rejected | Pending Review | Issue corrected, item re-enters the bakery workflow | Yes — resolution explanation |
| Rejected | Canceled | Item is unfulfillable | Yes — cancellation reason |

**Rejected** is not a terminal status — it signals that an item needs attention from the storefront. The baker's rejection message explains what's wrong; the storefront user's resolution or cancellation message explains what was done about it. Both messages are posted to the order's activity timeline referencing the specific item.

**Resolution flow**: The storefront user may first edit the order (fix item details, swap products, adjust quantities), then click **Resolve** next to the rejected item to return it to Pending Review. The Resolve action prompts for a required message. If the item cannot be fulfilled, the user clicks **Cancel Item**, which also prompts for a required message before moving the item to Canceled.

#### Undo via Drag

Dragging a tile to the swimlane it came from performs an **undo** rather than a new forward transition. See [Undo](#undo) for details.

#### Storefront Button Toggles

The **Picked Up** and **Paid** buttons on the Storefront are toggleable — clicking again reverts the action, allowing storefront users to correct accidental clicks.

---

## Order Status Roll-Up

Order status through production is **derived from the aggregate of its item statuses** — bakers change item statuses on the bakery board, and the order status follows automatically. Post-production statuses are manual actions.

### Derived Statuses (Pre-Production)

The order status is determined by evaluating these rules in priority order — the first matching rule wins:

| Priority | Condition | Order Status |
|----------|-----------|--------------|
| 1 | All items CANCELED | **CANCELED** |
| 2 | Any item PENDING_REVIEW or REJECTED | **IN_REVIEW** |
| 3 | All items ACCEPTED or CANCELED | **VERIFIED** |
| 4 | Any item IN_PROGRESS (implies none PENDING_REVIEW/REJECTED) | **IN_PROGRESS** |
| 5 | All non-canceled items PRODUCED | **PRODUCED** |

The order status recalculates each time an item status changes. For example, if a baker rejects an item on a VERIFIED order, the order drops back to IN_REVIEW automatically.

### Hold Behavior

When an order is IN_REVIEW (some items PENDING_REVIEW or REJECTED while others are ACCEPTED), the ACCEPTED items are **held** and cannot advance to IN_PROGRESS. This prevents:

- **Wasted production** — if the order is ultimately canceled because the flagged items can't be fulfilled
- **Indeterminate order status** — an order cannot be both IN_REVIEW and IN_PROGRESS

On the bakery board, held tiles display:
- A **hold indicator** (e.g., lock icon or "Held — order has items needing review") explaining why the tile cannot advance
- No In Progress drop target when dragged
- No "In Progress" status transition button

The hold lifts automatically when all sibling items reach ACCEPTED or CANCELED — at that point the order becomes VERIFIED and the tiles can proceed normally.

### Initial Item Status

When a new order is created, all its items enter **PENDING_REVIEW** and the order status is **IN_REVIEW**.

### Order Cancellation

| Action | Scope | Effect |
|--------|-------|--------|
| Cancel entire order | Storefront action | All non-terminal items cascade to CANCELED; order becomes CANCELED |
| Cancel individual item | Storefront action on REJECTED items | Item moves to CANCELED; order status recalculates per roll-up rules |

### Manual Statuses (Post-Production)

Once all non-canceled items reach PRODUCED, the order status becomes PRODUCED. The remaining statuses are **manual actions** not driven by item statuses:

| Transition | Description | Actor |
|------------|-------------|-------|
| PRODUCED → PACKAGED | Order packaged for transport | Baker |
| PACKAGED → IN_TRANSIT | Order dispatched to pickup location | Baker |
| IN_TRANSIT → READY_FOR_PICK_UP | Order received at pickup location | Barista |
| READY_FOR_PICK_UP → PICKED_UP | Customer collects order | Barista, Admin |

**IN_TRANSIT** is a new status representing transport from bakery to café. Details of post-production transitions (e.g., whether IN_TRANSIT is skippable for bakery-pickup orders) are to be specified.

### Relationship Between Status Names

| Order Status | Driven By | Item Status Counterpart |
|---|---|---|
| IN_REVIEW | Derived | PENDING_REVIEW, REJECTED |
| VERIFIED | Derived | ACCEPTED |
| IN_PROGRESS | Derived | IN_PROGRESS |
| PRODUCED | Derived | PRODUCED |
| CANCELED | Derived | CANCELED (all items) |
| PACKAGED | Manual | — |
| IN_TRANSIT | Manual | — |
| READY_FOR_PICK_UP | Manual | — |
| PICKED_UP | Manual | — |

**Status renames**: IN_REVIEW replaces the previous NEW (orders in this state need review, whether new or flagged). PRODUCED replaces the previous BAKED (generic). CANCELED replaces the previous CANCELLED (US English). NOT_OK has been removed — its function is absorbed by IN_REVIEW.

---

## Swimlanes

The Bakery view is organized as four equal-width vertical swimlanes. Each swimlane scrolls independently.

```
┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   PENDING    │  │   REVIEWED   │  │ IN PROGRESS  │  │  COMPLETED   │
│   REVIEW     │  │              │  │              │  │              │
│              │  │ ┌──────────┐ │  │              │  │ ┌──────────┐ │
│  (awaiting   │  │ │ REJECTED │ │  │  (currently  │  │ │ PRODUCED │ │
│   review)    │  │ ├──────────┤ │  │   in         │  │ │          │ │
│              │  │ │ ACCEPTED │ │  │  production) │  │ ├──────────┤ │
│              │  │ │          │ │  │              │  │ │ CANCELED │ │
│              │  │ └──────────┘ │  │              │  │ └──────────┘ │
└──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘
```

### Pending Review Swimlane (1)

| Swimlane | Status | Description |
|----------|--------|-------------|
| **Pending Review** | PENDING_REVIEW | Items awaiting review — both newly received and re-entered after rejection correction |

### Reviewed Swimlane (2)

The second lane contains two vertically stacked sections:

| Section | Status | Description |
|---------|--------|-------------|
| **Rejected** | REJECTED | Items flagged with problems requiring storefront attention |
| **Accepted** | ACCEPTED | Items reviewed and accepted, ready for production |

Rejected items appear at the **top** of this swimlane so they are immediately visible and can be addressed with urgency. Items in the Rejected section remain until someone on the storefront corrects the issue (returning the item to Pending Review) or cancels the item. See [Status Transitions](#status-transitions).

Accepted tiles whose order still has sibling items in PENDING_REVIEW or REJECTED status display a **hold indicator** — these tiles cannot advance to In Progress until the flagged items are resolved. See [Hold Behavior](#hold-behavior).

### In Progress Swimlane (3)

| Swimlane | Status | Description |
|----------|--------|-------------|
| **In Progress** | IN_PROGRESS | Items currently being manufactured (today's items only) |

### Completed Swimlane (4)

The fourth lane contains two vertically stacked sections for terminal states:

| Section | Status | Description |
|---------|--------|-------------|
| **Produced** | PRODUCED | Items whose production is completed |
| **Canceled** | CANCELED | Items that will not be fulfilled |

All four swimlanes are the **same width**.

---

## Item Grouping

Items are grouped **per day** — items for different due dates are never combined, since all baking is fresh. Within a single day and status, grouping depends on the product's `batchable` flag.

### Batchable Products

Products with `batchable = true` (the default) have their items aggregated into a single tile per product per day per status.

**Example:** For today, ten orders each containing "Chocolate Chip Cookie x5" and three orders containing "Chocolate Chip Cookie x10" produce a single tile:

> **Chocolate Chip Cookie**
> 80 items across 13 orders

### Non-Batchable Products

Products with `batchable = false` appear as individual tiles. However, tiles for the same non-batchable product are **visually grouped together** within the swimlane (adjacent, under a shared product header) so the baker can see related items at a glance.

**Example:** Three birthday cake orders appear as three individual tiles, grouped under "Birthday Cake":

> **Birthday Cake (Large)** — Order #234
> 1 item — "Happy 5th Birthday Lily"
>
> **Birthday Cake (Large)** — Order #267
> 1 item — "Congratulations Class of 2026"
>
> **Birthday Cake (Medium)** — Order #289
> 1 item — "Happy Anniversary Mom & Dad"

### Grouping Rules Summary

| Product Flag | Grouping | Result |
|--------------|----------|--------|
| `batchable = true` | Merge by product + day + status | One aggregate tile |
| `batchable = false` | Individual tiles, visually grouped by product | Adjacent tiles under product header |

### Product Batchable Flag

The `batchable` field is managed alongside other product properties in the Products view:

| Product | Batchable | Rationale |
|---------|-----------|-----------|
| Chocolate Chip Cookie | Yes | Standard product, produced in large batches |
| Croissant | Yes | Standard product, produced in large batches |
| Birthday Cake | No | Custom decoration, dedicated oven space |
| Wedding Cake | No | Highly custom, unique per order |

---

## Item Tiles

### Grouped Tile (Batchable Product)

Displays:
- **Product name** and size
- **Total quantity** across all contributing orders
- **Order count** (e.g., "across 13 orders")
- **Notes indicator** — if any contributing item has special instructions or additional details
- **Message indicator** — if any contributing order has unread staff messages

### Individual Tile (Non-Batchable Product)

Displays:
- **Product name** and size
- **Quantity**
- **Order reference** (order number or customer name)
- **Item details** (special instructions / customization notes)
- **Notes indicator** — if the item has special instructions or additional details
- **Message indicator** — if the order has unread staff messages

### Tile Buttons

To maximize tile space, action buttons are hidden by default and appear contextually:

- **Desktop**: Buttons appear on **hover** as an overlay on the tile
- **Mobile/touch**: Buttons appear after **tapping** the tile as an overlay

Buttons include:

| Button | When Available |
|--------|---------------|
| **Status transition buttons** | One button per valid target status for the tile's current status (e.g., Accept, Reject for a Pending Review tile) |
| **Undo** | When the tile has an undo stack entry |
| **Top / Up / Down / Bottom** | Reorder arrows for changing tile priority within the swimlane |
| **Details** | Mobile/touch only — opens the detail overlay (see below) |

On desktop, the detail overlay opens on **click** (since hover shows the buttons). On mobile, the tap shows buttons first, and the **Details** button opens the detail overlay.

### Detail Overlay

Because tiles combine items across multiple orders, it is not practical to display all additional details, notes, and messages directly on the tile. The tile shows summary information and indicators; the detail overlay provides the full picture.

The overlay lists the individual order items from each contributing order, showing:

- **Order reference** and item details
- **Additional details** (customer instructions from the order)
- **Notes / special instructions** per item
- **Messages** — the order's activity timeline (staff messages and system events), with a **message input** for posting replies directly from the overlay
- **Link** to the Order Detail view for each order item

Items in the overlay are **selectable** — selecting specific items filters the display to show only details for those items.

The message input allows bakers to respond without leaving the bakery board (e.g., "Got it, haven't started icing yet"). Messages posted from the overlay follow the same behavior as messages posted from the Order Detail view — they appear in the activity timeline, trigger unread indicators and toast notifications for the [notification audience](messaging.md#notification-audience), and are anchored to the order.

Opening the overlay marks unread messages as read, consistent with the messaging feature's [read model](messaging.md#unread-tracking).

### Tile Ordering Within Swimlanes

Tiles are organized within each swimlane as follows:

1. **Date groups** — Today's items first, then tomorrow, then subsequent dates. Date group headers are visually distinct (e.g., "Today — Tue, Jun 6").
2. **Within a date group** — Tiles are ordered by their persisted position (see [Persistence](#persistence) below). New tiles appear at the bottom of their date group.

### Reordering Tiles

Bakers can reorder tiles **within** a swimlane using drag or the **Top / Up / Down / Bottom** arrow buttons, setting production priority. This applies to both today's and future-dated tiles. The new position is persisted so it survives page refreshes and is visible to other users. **Tile reordering is not undoable.**

---

## Drag and Drop

### Interaction

Bakers drag tiles from one swimlane to another to transition item status. When a grouped tile is dragged, **all items in the group** are updated at once.

### Drop Targets

When a tile is picked up for dragging:

- **Valid swimlanes** display a drop target overlay covering the lane, providing a clear visual indicator of where the tile can be dropped
- **Invalid swimlanes** do not show a drop target (no visual affordance)
- **Reviewed swimlane**: Two stacked drop targets appear — **Rejected** (top) and **Accepted** (bottom) — so the baker can choose the specific status
- **In Progress swimlane**: The drop target only appears for tiles due **today** — future-dated tiles cannot enter production
- **Completed swimlane**: Two stacked drop targets appear — **Produced** (twice the height of the other) and **Canceled**
- **Undo target**: If the tile has an undo available, its previous swimlane (or section) displays a drop target. Dropping the tile there performs an undo rather than a new transition (see [Undo](#undo))

### Status Update

On drop:
1. All order items represented by the tile are updated to the new status
2. Each item update uses optimistic locking (version check) to prevent conflicts
3. If any item fails the version check, the operation is rolled back and the user is notified
4. A system event is recorded in each affected order's activity timeline
5. Cross-session updates are pushed via shared signals so other users see the change in real time
6. The dropped tile appears at the bottom of the target date group (or target section); the swimlane auto-scrolls to reveal it

### Undo

Each tile maintains an **undo stack** of its previous statuses, allowing mistakes to be corrected regardless of how much time has passed or how many other tiles have been transitioned since.

**Triggering undo:**
- **Drag-to-undo**: Drag the tile to the swimlane (or section) it came from. The previous location displays a drop target when an undo is available.
- **Tile button**: The tile's hover/tap button overlay includes an undo button when available.
- **Detail overlay**: The detail overlay also includes an undo button when available.

**What undo does:**
1. Reverts all affected items to their previous status
2. Removes the activity timeline entries created by the undone transition
3. Places the tile at the **end of its date group** in the target swimlane (not the original position, since intervening changes to other tiles may have shifted the layout)
4. Pushes the reversal to other sessions via shared signals

**Undo stack**: After undoing a transition, the tile's previous undo becomes available. For example, a tile transitioned Pending Review → Accepted → In Progress can undo back to Accepted, and then undo again back to Pending Review.

**Not undoable**: Tile reordering within a swimlane is not tracked in the undo stack.

**Undo stack invalidation**: Undo stack entries are invalidated when an item's status changes from **outside the bakery board** (i.e., storefront transitions). Edits to order/item data (details, quantity, etc.) do not affect the undo stack since they don't change status. Specifically:

| External Event | Effect on Undo Stack |
|---|---|
| Item resolved (REJECTED → PENDING_REVIEW from storefront) | Clear undo stack for that item |
| Item canceled (REJECTED → CANCELED from storefront) | Clear undo stack for that item |
| Order canceled (all items → CANCELED from storefront) | Clear undo stacks for all items in that order |
| Order edited (details, quantities, items added/removed) | No effect — undo stack remains valid |

### Concurrent Operations

When two users drag tiles simultaneously, the system must handle conflicts gracefully:

- Each status update uses optimistic locking (version check) per item
- If a conflict is detected, the operation is rolled back and the user is notified
- The board refreshes to reflect the current state

### Change Highlighting

When a tile's status, position, or contributing order/item data changes (from any source — drag-and-drop, storefront edits, new orders), all users see a **temporary highlight** on the affected tile, following the same highlight pattern used in the Storefront and other list views.

---

## Date Scope

### Default

The view defaults to showing items due **today and tomorrow** — the baker's immediate workload.

### Configurable Range

A date range filter allows the baker to expand or narrow the scope:
- **Today only** — focus on the current day
- **Today + Tomorrow** — default, the immediate horizon
- **This week** — broader planning view
- **Custom range** — specific start and end dates

The date range selector only allows current and future dates.

### Incomplete Past Orders

Items not yet in a terminal status (Produced or Canceled) continue to appear on the board **regardless of the selected date range**, even if their due date has passed. They remain visible until they are moved to a terminal status or their due date is changed to a current or future date. This prevents items from silently disappearing when their due date lapses.

### No Cross-Day Grouping

Items are **never grouped across different due dates**. All baking is fresh — cookies due today and cookies due tomorrow are separate tiles even if they're the same product.

---

## Persistence

Tile groupings and their ordering within swimlanes must be persisted so that:

- A baker's tile arrangement survives page refreshes
- Tile ordering is shared across sessions (all bakers see the same priority order)
- When items are added or removed (new orders, cancellations), the board incorporates changes without losing existing arrangement

### What Is Persisted

- **Tile identity**: The grouping key (product ID + due date + status for batchable; item ID + status for non-batchable)
- **Position**: The ordering of tiles within each swimlane, per date group
- **Undo stack**: The previous statuses for each tile, enabling multi-level undo
- **Scope**: Shared across all users (not per-user) — the production board reflects the team's agreed priority

### When Positions Update

- **Drag reorder within swimlane**: Position is saved immediately
- **Arrow button reorder**: Position is saved immediately
- **Drag to new swimlane (status change)**: Tile appears at the bottom of the target date group (or target section); the swimlane auto-scrolls to reveal it
- **Undo**: Tile appears at the end of its date group in the target swimlane
- **New items**: Appear at the bottom of their swimlane's date group
- **Items removed** (all items in a group reach a terminal status): Tile disappears; positions of remaining tiles compact

---

## Relationship to Storefront

The Storefront shows **orders** (grouped by date, with customer and payment info). The Bakery view shows **items** (grouped by product within status swimlanes). They are complementary views of the same underlying data.

### Status Changes from Storefront

Order-level status transitions that don't apply to the bakery workflow (e.g., marking an order as Picked Up) are handled from the Storefront via a **button** action, not a status combo box. By the time an order reaches the storefront for pickup, its items have already been through the bakery workflow (Produced status).

The **Picked Up** and **Paid** buttons are toggleable — clicking again reverts the action, allowing storefront users to correct accidental clicks.

### Order Detail Navigation

Tapping an order item in a tile's detail overlay navigates to the Order Detail view for that order.

---

## Data Model Changes

### New: Product `batchable` Field

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| batchable | boolean | true | Whether items of this product can be grouped in the Bakery view |

Added to `ProductEntity`, product projections, and product UI models. Editable in the Products view by admins.

### Order Status Renames

| Old | New | Reason |
|-----|-----|--------|
| `NEW` | `IN_REVIEW` | Orders in this state need review, whether new or flagged with a problem |
| `BAKED` | `PRODUCED` | Generic enough for non-baked products |
| `CANCELLED` | `CANCELED` | US English spelling |
| `NOT_OK` | *(removed)* | Absorbed by `IN_REVIEW` — a flagged order returns to review |

These affect `OrderStatusCode` (JPA enum), `OrderStatus` (UI enum), and any existing data (database migration).

### New: `IN_TRANSIT` Order Status

A new order-level status representing transport from bakery to pickup location. Inserted between PACKAGED and READY_FOR_PICK_UP. Details of post-production transitions to be specified.

### Order Item Status Renames

| Old | New | Reason |
|-----|-----|--------|
| `NEW` | `PENDING_REVIEW` | Items in this state are awaiting review — not necessarily new (e.g., corrected rejections re-entering the workflow) |
| `VERIFIED` | `ACCEPTED` | Parallelism with `REJECTED` |
| `BAKED` | `PRODUCED` | Generic enough for non-baked products |
| `NOT_OK` | `REJECTED` | Clarity |
| `CANCELLED` | `CANCELED` | US English spelling |

These affect `OrderItemStatusCode` (JPA enum), `OrderItemStatus` (UI enum, to be created), and any existing data (database migration).

### New: `OrderItemStatus` UI Enum

A new UI-layer enum mirroring `OrderItemStatusCode` with display names, colors, and helper methods (following the pattern of the existing `OrderStatus` enum).

### New: Tile Position Persistence

A new entity to store tile ordering within swimlanes. Details TBD during implementation, but conceptually:

| Field | Type | Description |
|-------|------|-------------|
| swimlane | OrderItemStatus | Which swimlane the tile is in |
| dueDate | LocalDate | The date group within the swimlane |
| groupingKey | String | Product ID (batchable) or item ID (non-batchable) |
| position | Integer | Sort order within the date group |

### New: Tile Undo Stack

Per-tile undo history to support multi-level undo. Conceptually:

| Field | Type | Description |
|-------|------|-------------|
| tileKey | String | The tile's grouping key |
| previousStatus | OrderItemStatus | The status before the transition |
| activityIds | List\<Long\> | Activity timeline entries to remove on undo |

---

## Real-Time Updates

The view uses **shared signals and component effects** to stay in sync across sessions, following the same reactive pattern used in the Storefront and other views:

- When a baker drags a tile to a new status, other users viewing the board see the tile move immediately
- When new orders are created (items appear in the Pending Review lane), the board updates without requiring a page refresh
- When items are modified from the Storefront or Order Detail views, the board reflects the changes
- Tile reordering within a swimlane is reflected across sessions
- Changed tiles receive a **temporary highlight** visible to all users

---

## Future Considerations

These are **out of scope** for the initial implementation but worth noting:

- **Partial status updates** — allow moving a subset of a grouped tile's items (e.g., 50 of 75 cookies produced so far)
- **Capacity indicators** — show oven capacity or time estimates per swimlane
- **Ingredient availability alerts** — flag items whose ingredients are running low
- **Print production sheet** — generate a printable summary of items to produce for a given date
