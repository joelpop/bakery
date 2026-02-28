# OrderStatusCode

Defines the workflow status of an order as it progresses through fulfillment.

**Package**: `bakery-jpamodel.code`

**Used by**: [OrderEntity](../entities/order.md)

---

## Values

| Value | Description |
|-------|-------------|
| IN_REVIEW | Order needs review — newly received or item(s) flagged with a problem |
| VERIFIED | Order reviewed and accepted — all items accepted, ready for production |
| IN_PROGRESS | Order being manufactured — at least one item in production |
| PRODUCED | Production completed — all non-canceled items produced |
| PACKAGED | Packaged for transport |
| IN_TRANSIT | Being transported to pickup location |
| READY_FOR_PICK_UP | Available for customer pickup |
| PICKED_UP | Customer collected order |
| CANCELED | Order will not be fulfilled |

**Renames**: `IN_REVIEW` replaces `NEW`, `PRODUCED` replaces `BAKED`, `CANCELED` replaces `CANCELLED`. `NOT_OK` has been removed — its function is absorbed by `IN_REVIEW`.

**New**: `IN_TRANSIT` represents the transport phase between bakery and pickup location.

---

## Status Workflow

Order status has two distinct phases: **derived** (pre-production, driven by item statuses) and **manual** (post-production, driven by staff actions).

### Pre-Production (Derived from Item Statuses)

```
IN_REVIEW → VERIFIED → IN_PROGRESS → PRODUCED
     ↑           │
     └───────────┘ (item rejected on bakery board)
     │
     ↓
  CANCELED
```

Pre-production order statuses are **not set directly** — they are computed from the aggregate of the order's item statuses. See [Bakery Workflow — Order Status Roll-Up](../../features/bakery-workflow.md#order-status-roll-up) for the full priority cascade.

| Priority | Condition | Order Status |
|----------|-----------|--------------|
| 1 | All items CANCELED | CANCELED |
| 2 | Any item PENDING_REVIEW or REJECTED | IN_REVIEW |
| 3 | All items ACCEPTED or CANCELED | VERIFIED |
| 4 | Any item IN_PROGRESS | IN_PROGRESS |
| 5 | All non-canceled items PRODUCED | PRODUCED |

### Post-Production (Manual)

```
PRODUCED → PACKAGED → IN_TRANSIT → READY_FOR_PICK_UP → PICKED_UP
```

| Transition | Description | Actor |
|------------|-------------|-------|
| PRODUCED → PACKAGED | Order packaged for transport | Baker |
| PACKAGED → IN_TRANSIT | Order dispatched to pickup location | Baker |
| IN_TRANSIT → READY_FOR_PICK_UP | Order received at pickup location | Barista |
| READY_FOR_PICK_UP → PICKED_UP | Customer collects order | Barista, Admin |

Details of post-production transitions (e.g., whether IN_TRANSIT is skippable for bakery-pickup orders) are to be specified.

### Order Cancellation

| Action | Effect |
|--------|--------|
| Cancel entire order (storefront) | All non-terminal items cascade to CANCELED; order becomes CANCELED |
| Cancel individual item (storefront) | Item moves to CANCELED; order status recalculates per roll-up rules |

---

## UI Representation

Each status is displayed with a distinctive visual treatment:

| Status | Badge Style | Purpose |
|--------|-------------|---------|
| IN_REVIEW | Accent/Blue | Draw attention to orders needing review |
| VERIFIED | Primary color | Order accepted, awaiting production |
| IN_PROGRESS | Primary color | Order being manufactured |
| PRODUCED | Success/Light green | Production complete |
| PACKAGED | Success/Green | Ready for transport |
| IN_TRANSIT | Success/Green | Being transported |
| READY_FOR_PICK_UP | Success/Bright green | Customer can pick up |
| PICKED_UP | Muted/Gray | Order complete |
| CANCELED | Muted/Gray | Order terminated, no further action |

---

## Business Rules

| Rule | Description |
|------|-------------|
| Just-in-time production | Orders are typically manufactured the morning of the due date |
| Paid is separate | Payment status is tracked independently from order status |
| No delivery | All orders are customer pickup; no shipping/delivery workflow |
| Hold behavior | ACCEPTED items cannot advance while sibling items are PENDING_REVIEW or REJECTED |

---

## Related Documentation

- [OrderEntity](../entities/order.md) - Entity that uses this code
- [OrderItemStatusCode](order-item-status.md) - Item-level status enum (items drive pre-production order status)
- [Bakery Workflow](../../features/bakery-workflow.md) - Production workflow and roll-up rules
- [Orders Feature](../../features/orders.md) - Order workflow details
