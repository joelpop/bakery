# NotificationEntity

Represents user-to-user notifications.

**Table**: `notifications`

**Package**: `bakery-jpamodel.entity`

---

## Fields

| Field | Type | Nullable | Unique | Description |
|-------|------|----------|--------|-------------|
| id | Long | No | Yes | Primary key (inherited from AbstractEntity) |
| version | Integer | No | No | Optimistic locking version (inherited) |
| message | String(1000) | No | No | Notification message content |
| sentAt | LocalDateTime | No | No | Timestamp when notification was sent |
| readAt | LocalDateTime | Yes | No | Timestamp when read (null = unread) |

---

## Relationships

| Relationship | Target | Type | Cascade | Description |
|--------------|--------|------|---------|-------------|
| sender | UserEntity | Many-to-One | - | User who sent the notification |
| recipient | UserEntity | Many-to-One | - | User who receives the notification |

---

## Projections

### NotificationSummaryProjection

**Package**: `bakery-jpamodel.projection`

Notification data for user menu display.

| Method | Return Type | Description |
|--------|-------------|-------------|
| getId() | Long | Notification ID |
| getMessage() | String | Message content |
| getSentAt() | LocalDateTime | When sent |
| getReadAt() | LocalDateTime | When read (null if unread) |
| getSenderFirstName() | String | Sender's first name |
| getSenderLastName() | String | Sender's last name |

**Used by**: User menu notifications panel, notifications page

---

## Indexes

| Columns | Purpose |
|---------|---------|
| (recipient_id, read_at) | Unread notification queries |
| (recipient_id, sent_at) | Recent notifications queries |
