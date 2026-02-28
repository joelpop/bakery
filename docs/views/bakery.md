# Bakery View

The Bakery view is a Kanban-style production board where bakers manage order items through status swimlanes. Items are grouped by product for efficient batch production and transitioned via drag and drop.

**Route**: `/bakery`

**Access**: Admin, Baker (not visible to Barista)

**Default view for**: Baker role

---

## Layout

The view is organized as four equal-width vertical swimlanes spanning the full viewport width. Each swimlane scrolls independently.

```
┌──────────────────────────────────────────────────────────────────────┐
│  [Date Range Filter]                                                │
├────────────────┬────────────────┬────────────────┬──────────────────┤
│    PENDING     │    REVIEWED    │  IN PROGRESS   │    COMPLETED     │
│    REVIEW      │                │                │                  │
│                │ ┌────────────┐ │                │ ┌────────────┐   │
│  ┌──────────┐  │ │ REJECTED   │ │  ┌──────────┐  │ │  PRODUCED  │   │
│  │Today     │  │ │  tile      │ │  │Today     │  │ │            │   │
│  ├──────────┤  │ ├────────────┤ │  ├──────────┤  │ │            │   │
│  │ tile     │  │ │ ACCEPTED   │ │  │ tile     │  │ ├────────────┤   │
│  │ tile     │  │ │  tile      │ │  │ tile     │  │ │  CANCELED  │   │
│  ├──────────┤  │ │  tile      │ │  │          │  │ │            │   │
│  │Tomorrow  │  │ │            │ │  │          │  │ └────────────┘   │
│  ├──────────┤  │ │            │ │  │          │  │                  │
│  │ tile     │  │ └────────────┘ │  │          │  │                  │
└────────────────┴────────────────┴────────────────┴──────────────────┘
```

### Toolbar

A toolbar above the swimlanes contains the date range filter for scoping which items are visible. See [Date Scope](../features/bakery-workflow.md#date-scope) for filter options.

**Current implementation**: Three preset buttons (Today, Today+Tomorrow, This Week) with Today as the default. Custom date range picker not yet implemented.

### Pending Review Swimlane (1)

Items awaiting review — both newly received orders and items re-entered after a rejection was corrected.

### Reviewed Swimlane (2)

Contains two vertically stacked sections. The **Rejected** section at the top gives rejected items immediate visibility so they can be addressed with urgency. The **Accepted** section below holds items that have been reviewed and are ready for production. Each section has its own header and scrollable tile area. During drag operations, these sections become distinct drop targets (see [Drag and Drop](#drag-and-drop) below).

Accepted tiles whose order still has sibling items in PENDING_REVIEW or REJECTED status display a **hold indicator** — these tiles cannot advance to In Progress until the flagged items are resolved. See [Hold Behavior](../features/bakery-workflow.md#hold-behavior).

### In Progress Swimlane (3)

Items currently being manufactured. Only today's items can enter this swimlane.

### Completed Swimlane (4)

Contains two vertically stacked sections for terminal states: **Produced** and **Canceled**. Each section has its own header and scrollable tile area.

All four swimlanes are the **same width**.

---

## Tiles

Tiles are the primary UI element within swimlanes. Their content depends on the product's `batchable` flag.

### Grouped Tile (Batchable Product)

A single tile aggregating all items of the same product, due date, and status:

```
┌─────────────────────────────┐
│ Chocolate Chip Cookie 📝 🔵 │
│ 80 items across 13 orders   │
└─────────────────────────────┘
```

- **Product name** and size
- **Total quantity** across all contributing orders
- **Order count**
- **Notes indicator** (icon) — visible if any item in the group has special instructions or additional details
- **Message indicator** (blue dot) — visible if any contributing order has unread messages

### Individual Tile (Non-Batchable Product)

Each order item appears as its own tile, but tiles for the same product are visually grouped under a shared product header:

```
  Birthday Cake
┌─────────────────────────────┐
│ Large — Order #234    📝 🔵 │
│ 1 item                      │
│ "Happy 5th Birthday Lily"   │
└─────────────────────────────┘
┌─────────────────────────────┐
│ Large — Order #267          │
│ 1 item                      │
│ "Congratulations Class..."  │
└─────────────────────────────┘
```

- **Product name** and size
- **Order reference** (order number or customer name)
- **Quantity**
- **Item details** (special instructions)
- **Notes indicator** (icon) — visible if the item has special instructions or additional details
- **Message indicator** (blue dot) — visible if the order has unread messages

### Tile Buttons

To maximize tile space, action buttons are hidden by default and appear contextually:

- **Desktop**: Buttons appear on **hover** as an overlay on the tile. **Clicking** the tile opens the detail overlay.
- **Mobile/touch**: Buttons appear after **tapping** the tile as an overlay. A **Details** button in the overlay opens the detail overlay (since tap is used to reveal the buttons).

Buttons include:

| Button | When Available |
|--------|---------------|
| Status transition buttons | One per valid target status (e.g., Accept / Reject for a Pending Review tile). Reject prompts for a required message before completing the transition. |
| Undo | When the tile has an undo stack entry |
| Top / Up / Down / Bottom | Reorder arrows for changing tile priority within the swimlane |
| Details | Mobile/touch only — opens the detail overlay |

**Current implementation**: Tile buttons are not yet implemented. Status transitions are performed exclusively via drag-and-drop. Undo is currently accessible only through the detail overlay. Click opens the detail overlay directly on both desktop and mobile.

### Detail Overlay

Because tiles combine items across multiple orders, it is not practical to display all additional details, notes, and messages directly on the tile. The tile shows summary information and indicators; the detail overlay provides the full picture.

The overlay lists the individual order items from each contributing order, showing:

- **Order reference** and item details
- **Additional details** (customer instructions from the order)
- **Notes / special instructions** per item
- **Messages** — the order's activity timeline (staff messages and system events), with a **message input** for posting replies directly from the overlay
- **Link** to the Order Detail view for each order item

Items in the overlay are **selectable** — selecting specific items filters the display to show only details for those items.

Opening the overlay marks unread messages as read, consistent with the [Messaging](../features/messaging.md#unread-tracking) feature's read model.

**Current implementation**: The overlay shows contributing order cards with customer name, quantity, item details, additional details, unread indicator, and "View Order" link. Not yet implemented: inline activity timeline / message posting, selectable item filtering, unread read-marking on open.

### Date Group Headers

Within each swimlane, tiles are organized under date group headers (e.g., "Today — Tue, Jun 6"). Today's items appear first, then tomorrow, then subsequent dates.

### Tile Ordering

Within a date group, tiles are ordered by their persisted position. Bakers can reorder tiles within a swimlane using drag or the **Top / Up / Down / Bottom** arrow buttons (both today's and future-dated tiles), setting production priority. Tile reordering is not undoable. See [Persistence](../features/bakery-workflow.md#persistence).

**Current implementation**: Reorder via drag only (arrow buttons not yet implemented). The full reordered list is persisted atomically via `saveTileOrder()`.

### Change Highlighting

When a tile's status, position, or contributing order/item data changes (from any source), all users see a **temporary highlight** on the affected tile, following the same highlight pattern used in the Storefront and other list views.

**Current implementation**: Signal-based auto-refresh is working (board refreshes on data changes), but per-tile change highlighting animations are not yet implemented.

---

## Drag and Drop

### Transitioning Status

Bakers drag a tile from one swimlane to another to transition all items represented by that tile to the new status. The dropped tile appears at the bottom of the target date group (or target section). The swimlane auto-scrolls to reveal the newly placed tile.

### Drop Target Overlays

When a tile is picked up, **all swimlanes** simultaneously enter drag mode, displaying two complementary drop mechanisms:

1. **Overlay panel** (left ~30% of each swimlane): Translucent status drop zones for quick First/Last positioning
2. **Reorder insertion lines** (between tiles on the right ~70%): Precise position targeting

Tiles remain in the DOM throughout the drag (never removed), preventing layout shift and drag cancellation.

#### Overlay Panel

Each swimlane displays a translucent overlay panel on its left ~30%, absolutely positioned over the tile content. The panel contains one zone per status in that swimlane, sized proportionally by flex weight.

```
┌───────────────────────────────────────┐
│ To Review  2  │ Reviewed  5  │ ...    │
├───┬───────────┼───┬──────────┤        │
│   │           │   │          │        │
│ ⇈ │ tile      │ ⇈ │ tile     │        │
│   │           │   │          │        │
│ P │ tile      │ R │ tile     │        │
│ E │           │ E │          │        │
│ N │           │ J │          │        │
│ D │           │ E │          │        │
│ I │           │ C │          │        │
│ N │           │ T │          │        │
│ G │           │ E │          │        │
│   │           │ D │          │        │
│ R │           │   │          │        │
│ E │           │   │          │        │
│ V │           ├───┤          │        │
│ I │           │ ⇈ │          │        │
│ E │           │   │          │        │
│ W │           │ A │          │        │
│   │           │ C │          │        │
│ ⇊ │           │ C │          │        │
│   │           │ E │          │        │
│   │           │ P │          │        │
│   │           │ T │          │        │
│   │           │ E │          │        │
│   │           │ D │          │        │
│   │           │   │          │        │
│   │           │ ⇊ │          │        │
└───┴───────────┴───┴──────────┘        │
 Panel (~30%)    Panel (~30%)           │
```

Each status zone contains:
- **Top sub-zone** (⇈ icon): Drop here inserts at position 0 (first)
- **Vertical status label**: Big uppercase text in status color, centered between icons
- **Bottom sub-zone** (⇊ icon): Drop here inserts at last position

**Active zones** (valid transition targets + source status for reorder): Tinted with the status color (10% wash), 80% opacity icons and 50% opacity label. On hover/drag-over: 30% color fill background, icons and label turn white.

**Disabled zones** (invalid targets): Grey wash (5%), desaturated icons and label, no interactivity.

**Per-status color theming** via CSS custom property `--_panel-zone-color`:

| Status | Color |
|--------|-------|
| PENDING_REVIEW | Primary (blue) |
| REJECTED | Error (red) |
| ACCEPTED | Success (green) |
| IN_PROGRESS | Yellow |
| PRODUCED | Orange |
| CANCELED | Contrast (grey) |

#### Reorder Insertion Lines

Between tiles of active statuses (valid targets + source status), thin reorder drop zones appear as horizontal blue lines (3px). These use negative margins to overlay the gap between tiles without shifting layout. Dropping on a line inserts the tile at that specific position.

For the source status, no-op positions (adjacent to the dragged tile) are skipped. For target statuses, all positions are available since the tile is new to that group.

#### Drop Actions

| Drop Location | Action |
|--------------|--------|
| Panel zone top (⇈) — same status | Reorder to first position |
| Panel zone bottom (⇊) — same status | Reorder to last position |
| Panel zone top (⇈) — different status | Transition + insert at position 0 |
| Panel zone bottom (⇊) — different status | Transition + insert at last position |
| Reorder line — same status | Reorder to that position |
| Reorder line — different status | Transition + insert at that position |

#### Active Target Computation

When a tile is picked up, valid drop targets are computed:

1. Start with static rules from `OrderItemStatus.getValidBakeryTargets()`
2. Remove `IN_PROGRESS` if tile's due date is after today (today-only rule)
3. Remove `IN_PROGRESS` if tile is on hold (hold constraint)
4. Source status is always active (for reorder within same swimlane)

#### Auto-Scroll

During drag, all swimlanes enable auto-scroll via client-side JavaScript. When the cursor is within 50px of the top or bottom edge of a swimlane's scroller, it scrolls incrementally at 6px per `requestAnimationFrame`. Scrolling stops when the edge is reached or the cursor moves away.

### Reordering

Tiles can be dragged within the same swimlane to change their priority order. This applies to both today's and future-dated tiles. The new position is persisted immediately. Reordering is not undoable.

**Current implementation**: The full reordered list is persisted atomically via `saveTileOrder()` (not single-tile `saveTilePosition()`).

### Undo

Dragging a tile to the swimlane it came from performs an **undo** rather than a new forward transition. The tile's button overlay also includes an undo button. Undo:

1. Reverts all affected items to their previous status
2. Removes the activity timeline entries created by the undone transition
3. Places the tile at the **end of its date group** in the target swimlane (not the original position, since other tiles may have shifted)

After undoing, the tile's previous undo becomes available (multi-level undo stack). For example, a tile transitioned Pending Review → Accepted → In Progress can undo back to Accepted, then undo again back to Pending Review.

**Current implementation**: Drag-to-undo is not yet implemented. Undo is currently triggered only from the detail overlay (which temporarily has the undo button until tile buttons are implemented).

### Concurrent Operations

When two users drag tiles simultaneously, the system handles conflicts via optimistic locking. If a conflict is detected, the operation is rolled back, the user is notified, and the board refreshes to reflect the current state.

---

## Responsive Behavior

### Desktop
- Four equal-width swimlanes side by side
- Hover on tiles reveals action buttons
- Click on tiles opens detail overlay
- Drag and drop for status transitions and reordering

### Tablet (≤ 1024px)
- Swimlanes get `overflow-x: auto` with `min-width: 220px`
- Tap on tiles reveals button overlay (with Details button)
- Touch drag for tile transitions

### Phone (≤ 480px, coarse pointer)
- Horizontal scroll-snap: one swimlane visible at a time (`min-width: 85vw`, `scroll-snap-type: x mandatory`)
- Tap on tiles reveals button overlay (with Details button)
- Touch drag for tile transitions

---

## Related Documentation

- [Bakery Workflow](../features/bakery-workflow.md) — Full workflow specification, item grouping rules, status transitions, data model changes, and persistence
- [Orders Feature](../features/orders.md) — Order workflow and data model
- [Storefront View](storefront.md) — Order management (complementary view of the same data)
