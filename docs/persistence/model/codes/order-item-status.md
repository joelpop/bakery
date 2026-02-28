# OrderItemStatusCode

Defines the workflow status of an individual order item as it progresses through the bakery production workflow.

**Package**: `bakery-jpamodel.code`

**Used by**: [OrderItemEntity](../entities/order-item.md)

---

## Values

| Value | Description |
|-------|-------------|
| PENDING_REVIEW | Item awaiting review (newly received or re-entered after correction) |
| ACCEPTED | Item reviewed and accepted, ready for production |
| IN_PROGRESS | Item currently being manufactured |
| PRODUCED | Item production completed |
| REJECTED | Item flagged with a problem requiring storefront attention |
| CANCELED | Item will not be fulfilled |

**Renames**: `PENDING_REVIEW` replaces `NEW`, `ACCEPTED` replaces `VERIFIED`, `PRODUCED` replaces `BAKED`, `REJECTED` replaces `NOT_OK`, `CANCELED` replaces `CANCELLED`. See also [OrderStatusCode](order-status.md) for corresponding order-level renames.

---

## Status Workflow

Item statuses are managed on the Bakery production board (via drag and drop) and from the Storefront (for rejected items).

```
PENDING_REVIEW → ACCEPTED → IN_PROGRESS → PRODUCED
      ↓              ↓
   REJECTED ←────────┘
      ↓  ↑
      ↓  └── PENDING_REVIEW (from Storefront)
      ↓
   CANCELED (from Storefront)
```

### Bakery Board Transitions (drag and drop)

| Transition | Constraint | Actor |
|------------|------------|-------|
| PENDING_REVIEW → ACCEPTED | | Baker, Admin |
| PENDING_REVIEW → REJECTED | Requires a message (rejection reason) | Baker, Admin |
| ACCEPTED → IN_PROGRESS | Only for items due **today** | Baker, Admin |
| ACCEPTED → REJECTED | Requires a message (rejection reason) | Baker, Admin |
| IN_PROGRESS → PRODUCED | | Baker, Admin |

### Storefront Transitions (order detail view)

| Transition | Description | Actor |
|------------|-------------|-------|
| REJECTED → PENDING_REVIEW | Issue corrected via Resolve button; requires a message (resolution explanation) | Admin, Barista |
| REJECTED → CANCELED | Item unfulfillable via Cancel Item button; requires a message (cancellation reason) | Admin, Barista |

---

## UI Representation

Each status is displayed with a distinctive visual treatment:

| Status | Badge Style | Purpose |
|--------|-------------|---------|
| PENDING_REVIEW | Accent/Blue | Draw attention to items needing review |
| ACCEPTED | Primary color | Item accepted, awaiting production |
| IN_PROGRESS | Primary color | Item being manufactured |
| PRODUCED | Success/Green | Production complete |
| REJECTED | Warning/Orange | Alert staff to issues requiring attention |
| CANCELED | Muted/Gray | Item terminated, no further action |

---

## Relationship to OrderStatusCode

Order items have their own lifecycle that **drives** order-level statuses through production. Pre-production order statuses (IN_REVIEW, VERIFIED, IN_PROGRESS, PRODUCED, CANCELED) are derived from the aggregate of item statuses via [roll-up rules](../../features/bakery-workflow.md#order-status-roll-up). Post-production order statuses (PACKAGED, IN_TRANSIT, READY_FOR_PICK_UP, PICKED_UP) are manual actions independent of item statuses.

| Item Status | Drives Order Status |
|---|---|
| Any PENDING_REVIEW or REJECTED | → IN_REVIEW |
| All ACCEPTED or CANCELED | → VERIFIED |
| Any IN_PROGRESS | → IN_PROGRESS |
| All non-canceled PRODUCED | → PRODUCED |
| All CANCELED | → CANCELED |

---

## Related Documentation

- [OrderItemEntity](../entities/order-item.md) - Entity that uses this code
- [Bakery Workflow](../../features/bakery-workflow.md) - Production board workflow details
- [OrderStatusCode](order-status.md) - Order-level status enum (separate lifecycle)
