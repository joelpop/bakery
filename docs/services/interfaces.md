# Service Interfaces

Service interfaces define the business operations available to the UI layer. All methods work with UI model objects from the `bakery-uimodel` module, never JPA entities.

---

## UserService

Manages user accounts and authentication.

### Query Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| findById | id: Long | Optional&lt;User&gt; | Find user by ID |
| findByEmail | email: String | Optional&lt;User&gt; | Find user by login email |
| list | search: String | List&lt;User&gt; | List users, optionally filtered by search term |
| list | search: String, pageable: Pageable | Page&lt;User&gt; | Paginated user list with search |
| isEmailAvailable | email: String, excludeUserId: Long | boolean | Check if email is available for use |

### Mutation Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| create | user: User | User | Create a new user account |
| update | user: User | User | Update existing user |
| delete | id: Long | void | Delete user by ID |
| updatePhoto | userId: Long, photo: byte[], contentType: String | void | Update user's profile photo |
| changePassword | userId: Long, currentPassword: String, newPassword: String | void | Change user's password |

---

## CustomerService

Manages customer records.

### Query Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| findById | id: Long | Optional&lt;Customer&gt; | Find customer by ID |
| search | name: String | List&lt;Customer&gt; | Search customers by name for autocomplete |
| getRecentCustomers | limit: int | List&lt;Customer&gt; | Get recently active customers |

### Mutation Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| findOrCreate | name: String, phoneNumber: String | Customer | Find existing customer by phone or create new |

---

## ProductService

Manages the product catalog.

### Query Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| findById | id: Long | Optional&lt;Product&gt; | Find product by ID |
| list | - | List&lt;Product&gt; | List all products |
| listAvailable | - | List&lt;Product&gt; | List only available products (for order form) |
| search | name: String | List&lt;Product&gt; | Search products by name |
| isNameAvailable | name: String, excludeProductId: Long | boolean | Check if product name is available |
| countUnavailable | - | long | Count unavailable products (for dashboard KPI) |

### Mutation Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| create | product: Product | Product | Create a new product |
| update | product: Product | Product | Update existing product |
| delete | id: Long | void | Delete product by ID |
| setAvailability | productId: Long, available: boolean | void | Set product availability status |

---

## LocationService

Manages pickup locations.

### Query Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| findById | id: Long | Optional&lt;Location&gt; | Find location by ID |
| findByCode | code: String | Optional&lt;Location&gt; | Find location by code |
| list | - | List&lt;Location&gt; | List all locations |
| listActive | - | List&lt;Location&gt; | List only active locations (for dropdowns) |
| isCodeAvailable | code: String, excludeLocationId: Long | boolean | Check if code is available |
| isNameAvailable | name: String, excludeLocationId: Long | boolean | Check if name is available |

### Mutation Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| create | location: Location | Location | Create a new location |
| update | location: Location | Location | Update existing location |
| delete | id: Long | void | Delete location by ID (fails if orders exist) |
| setActive | locationId: Long, active: boolean | void | Set location active status |
| reorder | locationIds: List&lt;Long&gt; | void | Update sort order for locations |

---

## OrderService

Manages customer orders.

### Query Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| findById | id: Long | Optional&lt;Order&gt; | Find order by ID |
| findByIdWithItems | id: Long | Optional&lt;Order&gt; | Find order with line items loaded |
| list | filter: OrderFilter | List&lt;OrderSummary&gt; | List orders with optional filters |
| list | filter: OrderFilter, pageable: Pageable | Page&lt;OrderSummary&gt; | Paginated order list |
| getOrdersByDate | filter: OrderFilter | Map&lt;LocalDate, List&lt;OrderSummary&gt;&gt; | Orders grouped by due date (for storefront) |

### Mutation Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| create | order: Order | Order | Create a new order |
| update | order: Order | Order | Update existing order |
| updateStatus | orderId: Long, newStatus: OrderStatus | void | Change order status |
| markAsPaid | orderId: Long | void | Mark order as paid |
| markAsUnpaid | orderId: Long | void | Mark order as unpaid (for corrections) |
| delete | id: Long | void | Delete order by ID |

### OrderFilter

Filter criteria for order queries:

| Field | Type | Description |
|-------|------|-------------|
| status | OrderStatus | Filter by status (optional) |
| customerId | Long | Filter by customer (optional) |
| locationId | Long | Filter by location (optional) |
| includePastOrders | boolean | Include orders before today |
| fromDate | LocalDate | Start of date range (optional) |
| toDate | LocalDate | End of date range (optional) |

---

> **Note**: The originally planned `NotificationService` has been superseded by `OrderActivityService`. Staff communication happens in the context of orders via the activity timeline and `MessageBroadcaster`. See [Messaging](../features/messaging.md) for details.

---

## BakeryService

Manages the bakery production board: tile listing, status transitions, position persistence, and undo.

### Query Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| listTiles | startDate: LocalDate, endDate: LocalDate | List&lt;BakeryTile&gt; | Fetch all tiles for date range (includes overdue non-terminal items) |
| getTileDetails | groupingKey: String | List&lt;BakeryTileDetail&gt; | Get contributing order items for a tile |
| getUndoStack | groupingKey: String | List&lt;OrderItemStatus&gt; | Get previous statuses for undo |

### Mutation Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| saveTileOrder | swimlane: OrderItemStatus, dueDate: LocalDate, orderedGroupingKeys: List&lt;String&gt;, movedGroupingKey: String | void | Bulk persist tile positions for a swimlane/date group |
| transitionTile | tile: BakeryTile, newStatus: OrderItemStatus, position: int, rejectionMessage: String | void | Atomic tile transition (items, positions, events, roll-up, undo) |
| undoTileTransition | groupingKey: String | OrderItemStatus | Undo most recent transition; returns previous status |

---

## OrderActivityService

Manages the order activity timeline for staff messaging and system event recording.

### Query Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| listActivities | orderId: Long | List&lt;OrderActivity&gt; | Get all activities for an order |
| listActivitiesSince | orderId: Long, since: Instant | List&lt;OrderActivity&gt; | Get activities since timestamp (incremental load) |
| hasUnreadMessages | orderId: Long | boolean | Check if order has unread messages |

### Mutation Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| postMessage | orderId: Long, text: String | OrderActivity | Post a staff message to the activity timeline |
| recordSystemEvent | orderId: Long, text: String, referencedItemId: Long | OrderActivity | Record an automated system event |
| markRead | orderId: Long | void | Mark all messages as read for an order |

---

## CurrentUserService

Provides access to the currently authenticated user.

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| getCurrentUserEmail | — | String | Email from security context |
| getCurrentUser | — | Optional&lt;UserSummary&gt; | Current user as UI model |
| isAdmin | — | boolean | Check if user has ADMIN role |
| isBaker | — | boolean | Check if user has BAKER role |
| isBarista | — | boolean | Check if user has BARISTA role |

---

## UserLocationService

Session-scoped working location management. Persists the user's selected working location for the session.

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| getCurrentLocation | — | Optional&lt;LocationSummary&gt; | Get the user's current working location |
| setCurrentLocation | location: LocationSummary | void | Set the working location for this session |
| isCurrentLocationSet | — | boolean | Whether a location has been selected |
| initializeFromUserPrimaryLocation | — | void | Auto-initialize from user's primary location |

---

## ClientDetailsService

Lazily detects and caches browser client details per session.

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| getBrowserTimezone | — | ZoneId | Browser timezone (falls back to system default) |

---

## DataChangeNotifier

Bridge interface for service implementations to notify the UI layer of data changes. Implemented by `DataChangeSignalUpdater` in `bakery-ui`.

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| notifyChange | entityType: EntityType, entityId: long | void | Broadcast entity-level change |
| notifyTileChange | groupingKey: String | void | Broadcast tile-level change (default no-op) |
| notifyMessage | notification: MessageNotification | void | Broadcast message notification (default no-op) |

---

## DashboardService

Provides analytics and KPI data.

### Query Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| getKpis | - | DashboardKpis | Get current dashboard KPI values |
| getPickupsByDay | month: YearMonth | List&lt;DailyPickupCount&gt; | Pickups per day for a month |
| getPickupsByMonth | year: int | List&lt;MonthlyPickupCount&gt; | Pickups per month for a year |
| getSalesComparison | years: int... | List&lt;YearlySalesData&gt; | Sales data for year-over-year comparison |
| getProductBreakdown | month: YearMonth | List&lt;ProductPickupCount&gt; | Product pickup counts for pie chart |
| getUpcomingOrders | limit: int | List&lt;OrderSummary&gt; | Upcoming orders for dashboard widget |

### DashboardKpis

| Field | Type | Description |
|-------|------|-------------|
| remainingToday | int | Orders not yet ready for today |
| nextPickupTime | LocalTime | Next pickup time today |
| notAvailable | int | Count of unavailable products |
| newOrders | int | Count of orders with IN_REVIEW status |
| sinceLastNewOrder | Duration | Time since last new order |
| tomorrowOrders | int | Order count for tomorrow |
| firstTomorrowPickup | LocalTime | First pickup time tomorrow |

---

## UI Model Classes

UI models are plain POJOs in `bakery-uimodel.data`:

### User

| Field | Type | Description |
|-------|------|-------------|
| id | Long | User ID |
| email | String | Login email |
| firstName | String | First name |
| lastName | String | Last name |
| role | UserRole | User role |
| photoUrl | String | URL to profile photo (or null) |

**Note**: Password is never exposed in UI model. Password operations use dedicated service methods.

### Customer

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Customer ID |
| name | String | Full name |
| phoneNumber | String | Phone number |
| email | String | Email (optional) |

### Product

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Product ID |
| name | String | Product name |
| description | String | Description (optional) |
| size | String | Serving size |
| price | BigDecimal | Unit price |
| available | boolean | Availability status |
| photoUrl | String | URL to product photo (or null) |

### Location

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Location ID |
| name | String | Display name |
| code | String | Short identifier code |
| address | String | Physical address (optional) |
| active | boolean | Whether location is available |
| sortOrder | int | Display order in lists |

### Order

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Order ID |
| status | OrderStatus | Current status |
| dueDate | LocalDate | Pickup date |
| dueTime | LocalTime | Pickup time |
| location | Location | Pickup location |
| customer | Customer | Customer details |
| additionalDetails | String | Special instructions |
| items | List&lt;OrderItem&gt; | Line items |
| total | BigDecimal | Calculated total |
| discount | BigDecimal | Discount amount (optional) |
| paid | Boolean | Whether payment has been received |

### OrderSummary

Lightweight order representation for lists:

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Order ID |
| status | OrderStatus | Current status |
| dueDate | LocalDate | Pickup date |
| dueTime | LocalTime | Pickup time |
| location | Location | Pickup location |
| customerName | String | Customer name |
| paid | Boolean | Payment status |
| items | List&lt;OrderItemSummary&gt; | Item summaries |

### OrderItem

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Item ID |
| product | Product | Product reference |
| quantity | int | Quantity ordered |
| details | String | Per-item notes (optional) |
| unitPrice | BigDecimal | Price at order time |
| lineTotal | BigDecimal | Quantity × unit price |

### ~~Notification~~ (Superseded)

> Superseded by `OrderActivity`. See [Messaging](../features/messaging.md) for details.

---

## UI Model Enums

Enums in `bakery-uimodel.type`:

### UserRole
`ADMIN`, `BAKER`, `BARISTA`

### OrderStatus
`IN_REVIEW`, `VERIFIED`, `IN_PROGRESS`, `PRODUCED`, `PACKAGED`, `IN_TRANSIT`, `READY_FOR_PICK_UP`, `PICKED_UP`, `CANCELED`
