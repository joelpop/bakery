# OrderStatusCode

Defines the workflow status of an order as it progresses through fulfillment.

**Package**: `bakery-jpamodel.code`

**Used by**: [OrderEntity](../entities/order.md)

---

## Values

| Value | Description |
|-------|-------------|
| NEW | Order just received, not yet reviewed by bakery |
| VERIFIED | Order reviewed and accepted by bakery |
| NOT_OK | Problem requiring attention (customer contact needed) |
| CANCELLED | Cancelled on purpose, no further action needed |
| IN_PROGRESS | Being manufactured at bakery |
| BAKED | Baking completed at bakery |
| PACKAGED | Packaged and ready for transport to café |
| READY_FOR_PICK_UP | Available for customer pickup at café |
| PICKED_UP | Picked up by customer |

---

## Status Workflow

Orders progress through statuses in the following sequence:

```
NEW → VERIFIED → IN_PROGRESS → BAKED → PACKAGED → READY_FOR_PICK_UP → PICKED_UP
        ↓
     NOT_OK ←→ VERIFIED
        ↓
    CANCELLED
```

### Normal Flow

| Transition | Trigger | Actor |
|------------|---------|-------|
| NEW → VERIFIED | Bakery reviews and accepts the order | Baker, Admin |
| VERIFIED → IN_PROGRESS | Production begins | Baker |
| IN_PROGRESS → BAKED | Baking completed | Baker |
| BAKED → PACKAGED | Order packaged for transport | Baker |
| PACKAGED → READY_FOR_PICK_UP | Order delivered to café | Barista, Baker |
| READY_FOR_PICK_UP → PICKED_UP | Customer picks up order | Barista, Admin |

### Problem Flow

| Transition | Trigger | Actor |
|------------|---------|-------|
| NEW → NOT_OK | Problem identified before verification | Baker, Admin |
| VERIFIED → NOT_OK | Problem identified during production | Baker, Admin |
| NOT_OK → VERIFIED | Issue resolved, order back on track | Baker, Admin |
| NOT_OK → CANCELLED | Order cannot be fulfilled | Admin |
| Any (pre-pickup) → CANCELLED | Order cancelled at request | Admin |

---

## UI Representation

Each status is displayed with a distinctive visual treatment:

| Status | Badge Style | Purpose |
|--------|-------------|---------|
| NEW | Accent/Blue | Draw attention to orders needing review |
| VERIFIED | Primary color | Order accepted, awaiting production |
| NOT_OK | Warning/Orange | Alert staff to issues requiring action |
| CANCELLED | Muted/Gray | Order terminated, no further action |
| IN_PROGRESS | Primary color | Order being manufactured |
| BAKED | Success/Light green | Baking complete |
| PACKAGED | Success/Green | Ready for transport |
| READY_FOR_PICK_UP | Success/Bright green | Customer can pick up |
| PICKED_UP | Muted/Gray | Order complete |

---

## Business Rules

| Rule | Description |
|------|-------------|
| Just-in-time production | Orders are typically manufactured the morning of the due date |
| Problem notification | NOT_OK status triggers alert; customer may need to be contacted |
| Paid is separate | Payment status is tracked independently from order status |
| No delivery | All orders are customer pickup; no shipping/delivery workflow |

---

## Related Documentation

- [OrderEntity](../entities/order.md) - Entity that uses this code
- [Orders Feature](../../features/orders.md) - Order workflow details
