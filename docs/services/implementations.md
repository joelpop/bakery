# Service Implementations

Service implementations reside in `bakery-jpaservice` and use JPA repositories with MapStruct mappers for entity/model conversion.

## Implementation Architecture

```
┌─────────────────────────────────────────┐
│        Service Implementation           │
│   (e.g., JpaUserService)                │
├─────────────────────────────────────────┤
│  Dependencies:                          │
│  • Repository (for data access)         │
│  • Mapper (for entity↔model conversion) │
│  • Other services (for cross-cutting)   │
└─────────────────┬───────────────────────┘
                  │
        ┌─────────┴─────────┐
        │                   │
        ▼                   ▼
┌───────────────┐   ┌───────────────┐
│  Repository   │   │    Mapper     │
│ (Spring Data) │   │  (MapStruct)  │
└───────────────┘   └───────────────┘
```

---

## Implementation Naming Convention

Service implementations follow the pattern `Jpa{ServiceName}`:

| Interface | Implementation |
|-----------|----------------|
| UserService | JpaUserService |
| CustomerService | JpaCustomerService |
| ProductService | JpaProductService |
| LocationService | JpaLocationService |
| OrderService | JpaOrderService |
| NotificationService | JpaNotificationService |
| DashboardService | JpaDashboardService |

---

## MapStruct Mappers

Mappers convert between JPA entities/projections and UI models. They are Spring components generated at compile time.

### Mapper Interfaces

| Mapper | Converts Between |
|--------|------------------|
| UserMapper | UserEntity ↔ User, UserSummaryProjection → User |
| CustomerMapper | CustomerEntity ↔ Customer, CustomerSummaryProjection → Customer |
| ProductMapper | ProductEntity ↔ Product, ProductSummaryProjection → Product |
| LocationMapper | LocationEntity ↔ Location, LocationSummaryProjection → Location |
| OrderMapper | OrderEntity ↔ Order, OrderListProjection → OrderSummary (includes paid, discount) |
| OrderItemMapper | OrderItemEntity ↔ OrderItem, OrderItemSummaryProjection → OrderItemSummary |
| NotificationMapper | NotificationEntity → Notification |

### Mapping Conventions

**Entity to UI Model:**
- All simple fields map by name
- Photo binary data maps to photoUrl (URL path to photo endpoint)
- Nested entities map to nested UI models
- Projections map directly by getter method names

**UI Model to Entity:**
- Password fields are ignored (handled separately with encoding)
- Photo data is ignored (handled via dedicated photo update method)
- Audit fields (createdAt, updatedAt, createdBy, updatedBy) are ignored
- ID is preserved for updates, ignored for creates

---

## Transaction Management

All service implementations use declarative transaction management:

| Operation Type | Transaction Setting |
|----------------|---------------------|
| Query methods (find, list, get, count) | Read-only transaction |
| Mutation methods (create, update, delete) | Read-write transaction |
| Complex operations | Read-write with explicit rollback rules |

### Transaction Propagation

- Service methods use `REQUIRED` propagation (default)
- Joins existing transaction if present, creates new if none
- Nested service calls share the same transaction

---

## JpaUserService

### Dependencies
- UserRepository
- UserMapper
- PasswordEncoder (BCrypt)

### Key Behaviors

**create(User)**
1. Maps UI model to entity (excluding password)
2. Encodes password using BCrypt
3. Saves entity via repository
4. Maps saved entity back to UI model

**update(User)**
1. Fetches existing entity by ID
2. Updates fields from UI model (excluding password)
3. Saves and returns updated entity as UI model

**changePassword(userId, currentPassword, newPassword)**
1. Fetches user entity
2. Verifies current password matches stored hash
3. Encodes new password
4. Updates password hash

**updatePhoto(userId, photo, contentType)**
1. Fetches user entity
2. Sets photo binary and content type
3. Saves entity

---

## JpaLocationService

### Dependencies
- LocationRepository
- OrderRepository
- LocationMapper

### Key Behaviors

**create(Location)**
1. Validates code and name are unique
2. Maps UI model to entity
3. Saves entity via repository
4. Returns saved entity as UI model

**update(Location)**
1. Fetches existing entity by ID
2. Validates code/name uniqueness (excluding current entity)
3. Updates fields from UI model
4. Saves and returns updated entity

**delete(id)**
1. Checks if any orders reference this location
2. If orders exist, throws ValidationException
3. Otherwise deletes the location

**setActive(locationId, active)**
1. If deactivating, verifies at least one other location remains active
2. Updates active status
3. Saves entity

**reorder(locationIds)**
1. Fetches all location entities
2. Updates sortOrder based on position in list
3. Batch saves all locations

---

## JpaOrderService

### Dependencies
- OrderRepository
- CustomerRepository
- ProductRepository
- LocationRepository
- OrderMapper
- OrderItemMapper
- CurrentUserService

### Key Behaviors

**create(Order)**
1. Maps order to entity
2. Looks up customer entity by ID (or creates new customer)
3. Looks up location entity by ID
4. For each item:
   - Looks up product entity
   - Creates item entity with current product price snapshot
   - Calculates line total
5. Applies discount if provided
6. Calculates order total from line totals minus discount
7. Sets initial status to NEW
8. Sets paid to false
9. Sets createdBy from current user
10. Saves and returns as UI model

**update(Order)**
1. Fetches existing order entity
2. Clears existing items (orphan removal)
3. Creates new items from UI model
4. Recalculates total
5. Sets updatedBy from current user
6. Saves and returns as UI model

**updateStatus(orderId, newStatus)**
1. Fetches order entity
2. Validates status transition is allowed
3. Sets new status
4. Sets updatedBy from current user
5. Saves entity

**markAsPaid(orderId)**
1. Fetches order entity
2. Sets paid to true
3. Sets updatedBy from current user
4. Saves entity

**markAsUnpaid(orderId)**
1. Fetches order entity
2. Sets paid to false
3. Sets updatedBy from current user
4. Saves entity

**getOrdersByDate(filter)**
1. Queries orders using filter criteria
2. Maps to OrderSummary list
3. Groups by dueDate using LinkedHashMap (preserves order)
4. Returns map of date → order list

---

## JpaDashboardService

### Dependencies
- OrderRepository
- ProductRepository
- OrderItemRepository
- OrderMapper

### Key Behaviors

**getKpis()**
Aggregates multiple repository queries:
1. Count remaining orders today (where status ≠ READY)
2. Find next pickup time today
3. Count unavailable products
4. Count orders with NEW status
5. Calculate duration since last new order
6. Count orders for tomorrow
7. Find first pickup time tomorrow

**getDeliveriesByDay(month)**
1. Queries orders for the given month
2. Groups by day
3. Returns list of day → count pairs

**getProductBreakdown(month)**
1. Queries order items for the given month
2. Aggregates quantities by product name
3. Returns list for pie chart

---

## JpaNotificationService

### Dependencies
- NotificationRepository
- UserRepository
- NotificationMapper
- CurrentUserService

### Key Behaviors

**send(recipientId, message)**
1. Creates notification entity
2. Sets sender from current user
3. Sets recipient from recipientId
4. Sets sentAt to current time
5. Saves entity

**sendToRole(role, message)**
1. Finds all users with the specified role
2. Creates notification for each user
3. Batch saves all notifications

**markAllAsRead(userId)**
1. Updates all unread notifications for user
2. Sets readAt to current time
3. Uses bulk update for efficiency

---

## CurrentUserService

Helper service for accessing the currently authenticated user.

### Methods

| Method | Returns | Description |
|--------|---------|-------------|
| getCurrentUserEmail() | Optional&lt;String&gt; | Email from security context |
| getCurrentUserEntity() | UserEntity | Entity for audit fields |
| getCurrentUser() | Optional&lt;User&gt; | Current user as UI model |
| hasRole(role) | boolean | Check if user has specific role |
| isAdmin() | boolean | Check if user has ADMIN role |

### Usage
Injected into service implementations for:
- Setting audit fields (createdBy, updatedBy)
- Determining notification sender
- Authorization checks

---

## Error Handling

### Exception Types

| Exception | When Thrown |
|-----------|-------------|
| EntityNotFoundException | Entity with given ID not found |
| ValidationException | Business rule violation |
| DuplicateEmailException | Email already in use |
| InvalidPasswordException | Current password doesn't match |

### Service Layer Validation

Services perform validation before persistence:
- Required fields are present
- Unique constraints are satisfied
- Business rules are met (e.g., can't delete admin's own account)

Validation errors result in ValidationException with list of error messages.

---

## Caching Considerations

For frequently accessed, rarely changing data:
- Product list (for order form dropdown)
- Active locations list (for order form dropdown)
- User roles
- Dashboard KPIs (with short TTL)

Caching can be implemented using Spring Cache abstraction with method-level annotations.
