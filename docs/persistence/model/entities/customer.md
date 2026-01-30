# CustomerEntity

Represents customers who place orders.

**Table**: `customers`

**Package**: `bakery-jpamodel.entity`

---

## Fields

| Field | Type | Nullable | Unique | Description |
|-------|------|----------|--------|-------------|
| id | Long | No | Yes | Primary key (inherited from AbstractEntity) |
| version | Integer | No | No | Optimistic locking version (inherited) |
| name | String | No | No | Customer's full name |
| phoneNumber | String | No | No | Contact phone number |
| email | String | Yes | No | Contact email address |

---

## Relationships

| Relationship | Target | Type | Cascade | Description |
|--------------|--------|------|---------|-------------|
| orders | OrderEntity | One-to-Many | - | Orders placed by this customer |

---

## Fields (continued)

| Field | Type | Nullable | Unique | Description |
|-------|------|----------|--------|-------------|
| active | boolean | No | No | Whether customer is active (default: true) |

---

## Deletion Behavior (Soft Delete)

Customer deletion is a **soft delete** (marking as inactive), with rules based on order status:

| Scenario | Behavior |
|----------|----------|
| Customer has in-progress orders | **Block deletion** - Notify user that customer cannot be deleted while orders are being fulfilled |
| Customer has open pre-production orders | **Prompt confirmation** - Ask user to confirm cancellation of NEW/VERIFIED/NOT_OK orders before soft-deleting |
| Customer has no open orders | **Prompt confirmation** - Simple confirmation before soft-deleting |

### In-Progress Order Statuses (Block Deletion)
- IN_PROGRESS
- BAKED
- PACKAGED
- READY_FOR_PICK_UP

### Pre-Production Order Statuses (Cancel on Confirmation)
- NEW
- VERIFIED
- NOT_OK

### Soft Delete Effect
- `active` field is set to `false`
- Customer no longer appears in autocomplete/search
- Historical orders retain customer reference
- Customer can be reactivated if needed

---

## Projections

### CustomerSummaryProjection

**Package**: `bakery-jpamodel.projection`

Customer data for combo boxes and autocomplete fields.

| Method | Return Type | Description |
|--------|-------------|-------------|
| getId() | Long | Customer ID |
| getName() | String | Customer name |
| getPhoneNumber() | String | Phone number |

**Used by**: Customer combo box in order form, customer search autocomplete

---

## Indexes

| Columns | Purpose |
|---------|---------|
| (phone_number) | Customer lookup by phone |
| (name) | Customer search |
