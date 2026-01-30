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

## NotificationService

Manages user-to-user notifications.

### Query Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| getRecent | userId: Long, limit: int | List&lt;Notification&gt; | Get recent notifications for user |
| getUnread | userId: Long | List&lt;Notification&gt; | Get unread notifications |
| countUnread | userId: Long | long | Count unread notifications (for badge) |

### Mutation Operations

| Method | Parameters | Returns | Description |
|--------|------------|---------|-------------|
| send | recipientId: Long, message: String | void | Send notification to a user |
| sendToRole | role: UserRole, message: String | void | Send notification to all users with role |
| markAsRead | notificationId: Long | void | Mark single notification as read |
| markAllAsRead | userId: Long | void | Mark all user's notifications as read |

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
| newOrders | int | Count of orders with NEW status |
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

### Notification

| Field | Type | Description |
|-------|------|-------------|
| id | Long | Notification ID |
| senderName | String | Sender's full name |
| message | String | Message content |
| sentAt | LocalDateTime | When sent |
| readAt | LocalDateTime | When read (null if unread) |

---

## UI Model Enums

Enums in `bakery-uimodel.type`:

### UserRole
`ADMIN`, `BAKER`, `BARISTA`

### OrderStatus
`NEW`, `VERIFIED`, `NOT_OK`, `CANCELLED`, `IN_PROGRESS`, `BAKED`, `PACKAGED`, `READY_FOR_PICK_UP`, `PICKED_UP`
