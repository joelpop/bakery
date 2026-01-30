# Repositories

This document defines the Spring Data JPA repository interfaces for the Bakery application. All repositories use derived query methods and interface projections for efficient data retrieval.

## Repository Pattern

All repositories extend `JpaRepository<Entity, Long>`, providing standard CRUD operations, pagination, and sorting. Custom queries are derived from method names following Spring Data JPA conventions.

---

## UserRepository

Repository for managing user entities.

### Standard Operations
Inherited from `JpaRepository`: `save`, `findById`, `findAll`, `deleteById`, `count`, etc.

### Custom Query Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `findByEmail(email)` | Find user by their login email address | `Optional<UserEntity>` |
| `findByEmailIgnoreCase(email)` | Case-insensitive email lookup | `Optional<UserEntity>` |
| `existsByEmail(email)` | Check if email is already registered | `boolean` |
| `existsByEmailAndIdNot(email, id)` | Check email availability excluding a user | `boolean` |
| `findByRole(role)` | Find all users with a specific role | `List<UserEntity>` |
| `findByRoleOrderByLastNameAscFirstNameAsc(role)` | Find users by role, sorted by name | `List<UserEntity>` |
| `countByRole(role)` | Count users with a specific role | `long` |

### Projection Queries

| Method | Description | Returns |
|--------|-------------|---------|
| `findAllByOrderByLastNameAscFirstNameAsc()` | List all users for grid display | `List<UserSummaryProjection>` |
| `findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByLastNameAsc(search, search, search)` | Search users by name or email | `List<UserSummaryProjection>` |

---

## CustomerRepository

Repository for managing customer entities.

### Custom Query Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `findByPhoneNumber(phone)` | Find customer by phone number | `Optional<CustomerEntity>` |
| `findByPhoneNumberAndActiveTrue(phone)` | Find active customer by phone | `Optional<CustomerEntity>` |
| `findByNameContainingIgnoreCaseAndActiveTrueOrderByName(name)` | Search active customers by name | `List<CustomerEntity>` |
| `existsByPhoneNumber(phone)` | Check if phone number exists | `boolean` |

### Projection Queries

| Method | Description | Returns |
|--------|-------------|---------|
| `findTop20ByActiveTrueOrderByNameAsc()` | Get recent active customers for autocomplete | `List<CustomerSummaryProjection>` |
| `findByNameContainingIgnoreCaseAndActiveTrueOrderByNameAsc(name)` | Search active customers for combo box | `List<CustomerSummaryProjection>` |

---

## ProductRepository

Repository for managing product entities.

### Custom Query Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `findByName(name)` | Find product by exact name | `Optional<ProductEntity>` |
| `existsByName(name)` | Check if product name exists | `boolean` |
| `existsByNameAndIdNot(name, id)` | Check name availability excluding a product | `boolean` |
| `findByAvailableTrueOrderByNameAsc()` | Find all available products | `List<ProductEntity>` |
| `countByAvailableFalse()` | Count unavailable products (for dashboard KPI) | `long` |

### Projection Queries

| Method | Description | Returns |
|--------|-------------|---------|
| `findAllByOrderByNameAsc()` | List all products for admin grid | `List<ProductSummaryProjection>` |
| `findByAvailableTrueOrderByNameAsc()` | List available products for order form | `List<ProductSelectProjection>` |

---

## LocationRepository

Repository for managing location entities.

### Custom Query Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `findByCode(code)` | Find location by code | `Optional<LocationEntity>` |
| `existsByCode(code)` | Check if code exists | `boolean` |
| `existsByCodeAndIdNot(code, id)` | Check code availability excluding a location | `boolean` |
| `existsByName(name)` | Check if name exists | `boolean` |
| `existsByNameAndIdNot(name, id)` | Check name availability excluding a location | `boolean` |
| `findByActiveTrueOrderBySortOrderAsc()` | Find all active locations | `List<LocationEntity>` |
| `countByActiveTrue()` | Count active locations | `long` |

### Projection Queries

| Method | Description | Returns |
|--------|-------------|---------|
| `findAllByOrderBySortOrderAsc()` | List all locations for admin grid | `List<LocationEntity>` |
| `findByActiveTrueOrderBySortOrderAsc()` | Active locations for dropdowns | `List<LocationSummaryProjection>` |

---

## OrderRepository

Repository for managing order entities.

### Custom Query Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `findByStatus(status)` | Find orders by status | `List<OrderEntity>` |
| `findByDueDateOrderByDueTimeAsc(date)` | Find orders due on a specific date | `List<OrderEntity>` |
| `findByDueDateBetweenOrderByDueDateAscDueTimeAsc(start, end)` | Find orders within date range | `List<OrderEntity>` |
| `findByCustomerIdOrderByDueDateDescDueTimeDesc(customerId)` | Find orders for a customer | `List<OrderEntity>` |
| `countByStatus(status)` | Count orders by status | `long` |
| `countByDueDate(date)` | Count orders for a date | `long` |
| `countByDueDateAndStatusNot(date, status)` | Count remaining orders for today | `long` |
| `existsByCustomerIdAndStatusIn(customerId, statuses)` | Check if customer has orders in given statuses | `boolean` |
| `findByCustomerIdAndStatusIn(customerId, statuses)` | Find customer orders in given statuses | `List<OrderEntity>` |

### Projection Queries

| Method | Description | Returns |
|--------|-------------|---------|
| `findByDueDateGreaterThanEqualOrderByDueDateAscDueTimeAsc(date)` | Upcoming orders for storefront list | `List<OrderListProjection>` |
| `findByDueDateBetweenOrderByDueDateAscDueTimeAsc(start, end)` | Orders in date range for storefront | `List<OrderListProjection>` |
| `findByStatusOrderByDueDateAscDueTimeAsc(status)` | Orders by status for filtered view | `List<OrderListProjection>` |
| `findByCustomerIdAndDueDateGreaterThanEqualOrderByDueDateAsc(customerId, date)` | Customer's upcoming orders | `List<OrderListProjection>` |
| `findTop5ByDueDateGreaterThanEqualOrderByDueDateAscDueTimeAsc(date)` | Dashboard upcoming orders widget | `List<OrderDashboardProjection>` |

### Time-Based Queries

| Method | Description | Returns |
|--------|-------------|---------|
| `findFirstByDueDateAndStatusNotOrderByDueTimeAsc(date, status)` | Next pickup time today | `Optional<OrderTimeProjection>` |
| `findFirstByDueDateOrderByDueTimeAsc(date)` | First pickup time for a date | `Optional<OrderTimeProjection>` |
| `findFirstByStatusOrderByCreatedAtDesc(status)` | Most recent order with status | `Optional<OrderTimeProjection>` |

---

## OrderItemRepository

Repository for managing order line items.

### Custom Query Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `findByOrderIdOrderByIdAsc(orderId)` | Get items for an order | `List<OrderItemEntity>` |
| `deleteByOrderId(orderId)` | Delete all items for an order | `void` |

### Projection Queries

| Method | Description | Returns |
|--------|-------------|---------|
| `findByOrderId(orderId)` | Order items for display | `List<OrderItemSummaryProjection>` |

---

## NotificationRepository

Repository for managing user notifications.

### Custom Query Methods

| Method | Description | Returns |
|--------|-------------|---------|
| `findByRecipientIdAndReadAtIsNullOrderBySentAtDesc(userId)` | Unread notifications | `List<NotificationEntity>` |
| `countByRecipientIdAndReadAtIsNull(userId)` | Count unread (for badge) | `long` |
| `findTop10ByRecipientIdOrderBySentAtDesc(userId)` | Recent notifications | `List<NotificationEntity>` |

### Projection Queries

| Method | Description | Returns |
|--------|-------------|---------|
| `findByRecipientIdOrderBySentAtDesc(userId, pageable)` | Paginated notifications | `Page<NotificationSummaryProjection>` |

---

## Pagination Support

All list methods can accept a `Pageable` parameter for pagination:

| Method Pattern | Description |
|----------------|-------------|
| `findAllBy...(Pageable pageable)` | Returns `Page<T>` with pagination metadata |
| `findTop10By...()` | Returns first 10 results |
| `findFirst5By...()` | Returns first 5 results |

---

## Projection Interfaces

Interface projections are defined in `bakery-jpamodel.projection` package. Each projection is documented with its associated entity:

- [UserSummaryProjection](model/entities/user.md#projections)
- [CustomerSummaryProjection](model/entities/customer.md#projections)
- [ProductSummaryProjection, ProductSelectProjection](model/entities/product.md#projections)
- [LocationSummaryProjection](model/entities/location.md#projections)
- [OrderListProjection, OrderDashboardProjection, OrderTimeProjection](model/entities/order.md#projections)
- [OrderItemSummaryProjection](model/entities/order-item.md#projections)
- [NotificationSummaryProjection](model/entities/notification.md#projections)
