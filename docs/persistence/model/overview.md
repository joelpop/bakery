# JPA Model Overview

This document provides an overview of the JPA model for the Bakery application. Model classes are located in the `bakery-jpamodel` module.

## Entity Index

| Entity | Description | Document |
|--------|-------------|----------|
| UserEntity | Application users (staff members) | [user.md](entities/user.md) |
| CustomerEntity | Customers who place orders | [customer.md](entities/customer.md) |
| ProductEntity | Bakery products for sale | [product.md](entities/product.md) |
| LocationEntity | Pickup locations (Café, Bakery) | [location.md](entities/location.md) |
| OrderEntity | Customer orders | [order.md](entities/order.md) |
| OrderItemEntity | Line items within orders | [order-item.md](entities/order-item.md) |
| NotificationEntity | User-to-user notifications | [notification.md](entities/notification.md) |

---

## Code Index

| Code | Description | Document |
|------|-------------|----------|
| UserRoleCode | User access levels | [user-role.md](codes/user-role.md) |
| OrderStatusCode | Order lifecycle states | [order-status.md](codes/order-status.md) |

---

## Entity Relationships

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│    User     │       │   Product   │       │  Location   │
└─────────────┘       └──────┬──────┘       └──────┬──────┘
                             │                     │
                             │ *                   │ 1
┌─────────────┐       ┌──────▼─────────────────────▼──────┐
│  Customer   │◄──1───┤               Order               │
└──────┬──────┘       └──────────────────┬────────────────┘
       │                                 │
       │ *                               │ 1
       ▼                                 ▼
 ┌───────────┐                    ┌─────────────┐
 │   Order   │                    │  OrderItem  │ *
 └───────────┘                    └──────┬──────┘
                                         │ *
                                         ▼
                                  ┌─────────────┐
                                  │   Product   │
                                  └─────────────┘
```

---

## AbstractEntity

Base class providing common fields for all entities.

**Package**: `bakery-jpamodel.entity`

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Primary key, auto-generated identity |
| version | Integer | Optimistic locking version for concurrency control |

All entities extend AbstractEntity and inherit these fields.

---

## Projections Summary

Interface projections provide optimized read-only views of entities. Each projection is documented with its associated entity.

| Projection | Entity | Purpose |
|------------|--------|---------|
| UserSummaryProjection | [UserEntity](entities/user.md) | User list grid display |
| CustomerSummaryProjection | [CustomerEntity](entities/customer.md) | Customer combo box |
| ProductSummaryProjection | [ProductEntity](entities/product.md) | Product admin grid |
| ProductSelectProjection | [ProductEntity](entities/product.md) | Order form product dropdown |
| LocationSummaryProjection | [LocationEntity](entities/location.md) | Location dropdown |
| OrderListProjection | [OrderEntity](entities/order.md) | Storefront order list |
| OrderDashboardProjection | [OrderEntity](entities/order.md) | Dashboard upcoming orders |
| OrderTimeProjection | [OrderEntity](entities/order.md) | Dashboard KPI queries |
| OrderItemSummaryProjection | [OrderItemEntity](entities/order-item.md) | Order item display |
| NotificationSummaryProjection | [NotificationEntity](entities/notification.md) | Notification panel |

---

## Database Indexes

Recommended indexes for query performance:

| Table | Columns | Purpose |
|-------|---------|---------|
| users | (email) | Login queries |
| customers | (phone_number) | Customer lookup |
| products | (available) | Available product queries |
| locations | (code) | Location lookup, unique constraint |
| locations | (active, sort_order) | Active location list queries |
| orders | (due_date, due_time) | Storefront list queries |
| orders | (status) | Status filter queries |
| orders | (customer_id) | Customer order history |
| orders | (location_id) | Location order queries |
| orders | (paid) | Payment status filtering |
| order_items | (order_id) | Fetch items for order |
| notifications | (recipient_id, read_at) | Unread notification queries |
