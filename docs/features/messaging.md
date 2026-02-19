# Messaging

This document describes the order messaging system—a production and quality control tool that provides a unified activity timeline per order, combining system-generated events with staff-to-staff messages.

## Purpose

When a customer calls back to correct the message on a birthday cake, the barista who takes the call updates the order—but the baker fulfilling it needs to see that change before it's too late. The messaging system ensures that changes, notes, and status updates on an order are visible to everyone involved in fulfilling and handing off that order.

This is a **production/quality control tool**, not a social messaging platform. Every message is anchored to a specific order.

## Order Activity Timeline

Each order has a single, chronological activity timeline that interleaves two types of entries:

### System Events

Automatically generated when the order changes:

- Status changes — order-level (e.g., In Review → Verified, Produced → Packaged) and item-level (e.g., Pending Review → Accepted, In Progress → Produced)
- Field edits (e.g., due date changed, additional details updated)
- Item changes (e.g., item added, quantity changed, item removed)
- Payment changes (e.g., marked as paid)

### Staff Messages

Manually posted by staff, optionally referencing a specific order item:

- Corrections (e.g., "Customer called back—name on cake should be 'Lily', not 'Lilly'")
- Questions (e.g., "Is this the same Alice Johnson who ordered last week?")
- Notes (e.g., "Customer will arrive 30 minutes early")

### Example Timeline

> **10:02 AM — System**: Order created by Barista Ana
> **10:02 AM — System**: Order status: In Review
> **10:15 AM — System**: All items accepted — order status: Verified (by Admin Joel)
> **10:30 AM — Barista Ana**: "Customer called back—name on Birthday Cake should be 'Lily', not 'Lilly'. Already corrected the item details."
> **10:30 AM — System**: Item "Birthday Cake" details changed: "Happy 5th Brithday Lilly" → "Happy 5th Birthday Lily"
> **10:45 AM — Baker Tom**: "Got it, haven't started icing yet."
> **11:00 AM — System**: Status changed to In Progress by Baker Tom

## Notification Audience

The people who need to see activity on an order are determined by the order's **pickup location**:

- **Bakers** at the bakery location (they fulfill the order)
- **Baristas** at the pickup storefront location (they hand off the order to the customer)
- **Admins** see activity on all orders

Since the bakery itself is a location (not a café, but it can take orders), bakers who are associated with that location will see activity on orders picked up there as well.

## Unread Tracking

Only **human messages** trigger unread indicators and toast notifications. System events (status changes, field edits, etc.) are recorded in the timeline but do not mark an order as unread or pop up a toast—they are passive history, not actionable alerts.

Unread state is tracked **per message, globally** (not per user):

- Each staff message has a `read` flag (default `false`); system events are always considered read
- When a staff member posts a human message, it is created as **unread**
- Opening the order in **OrderDetailView** marks all unread messages on that order as **read** for all users
- This keeps the model simple—if anyone has looked at the messages, the team has seen them
- An order "has unread messages" if any of its staff messages have `read = false`

## Real-Time Behavior (Signals)

The messaging system uses Vaadin shared signals to push updates across sessions in real time.

### Storefront View — Unread Indicator

- Orders with unread human messages display a **blue dot** on their order card in the StorefrontView
- The dot appears/disappears reactively via a shared signal—no manual refresh needed
- Opening the order detail clears the dot across all sessions

### Order Detail View — Live Timeline

- On initial load, the timeline scrolls to the **first unread message**. If all messages are read, it scrolls to the bottom.
- Unread messages are visually distinguished (e.g., different background or a "New" divider before the first unread).
- If a user is viewing an order's detail and a new message arrives (human or system), it **appears in the timeline immediately** without requiring a page refresh.
- New messages arriving while viewing do **not** auto-scroll. Instead, a **"new messages" down-arrow button** appears below the last visible message (Slack-style). Clicking the button scrolls to the new messages.
- Exception: if the user is already scrolled to the bottom, new messages auto-scroll into view.

### Pop-Up Notifications

When a **human message** (not a system event) is posted on an order, a brief **toast notification** appears for users in the notification audience:

- **Bakers** associated with the bakery location
- **Baristas** associated with the order's pickup café location
- **Admins**

The toast appears regardless of which view the user is currently on. It includes the author name, order number, and a truncated preview of the message. Tapping the toast navigates to the order.

The message author does **not** receive a toast for their own message.

## UI Placement

The activity timeline is displayed within the **OrderDetailView**, not as a separate route. It appears as a panel alongside the existing order information.

## Relationship to Additional Details

Orders retain their **additional details** field separately from the messaging timeline. The two serve distinct purposes:

- **Additional details** captures **customer instructions** at order creation time (e.g., "No nuts—allergy", "Write 'Happy Birthday Lily' on cake"). It is a property of the order itself, visible at a glance on the order card and detail view.
- **Messages** are **staff-to-staff communication** about the order after it exists (e.g., "Customer called back—name should be 'Lily' not 'Lilly'").

Practical reasons for keeping them separate:

- Messages trigger unread indicators and toast notifications. Notes entered at order creation time would notify everyone unnecessarily—the order hasn't even been saved yet, so there's no audience to notify.
- Messages require an order ID to anchor to. At order creation time, the order doesn't exist in the database yet, so a message can't be posted until after the save.
- Customer instructions buried in a timeline of status changes and staff chatter lose their at-a-glance visibility.

## Message Properties

All messages carry the same urgency—there are no priority levels. Each message has:

| Property | Description |
|----------|-------------|
| Order | The order this message belongs to |
| Author | The user who posted the message |
| Timestamp | When the message was posted (stored as `Instant` in UTC; displayed as `LocalDateTime` in the user's browser timezone via `InstantMapper`) |
| Text | The message content |
| Referenced Item | (Optional) A specific order item the message pertains to |

## Future Considerations

These are **out of scope** for the initial implementation but worth noting:

- **Desktop/push notifications** — browser push notifications for urgent corrections
- **SMS/text notifications** — for offline staff
- **Ingredient inventory integration** — automatically post system events when items become unavailable due to ingredient stock
- **Customer-facing timeline** — a read-only view for customers to track their order
