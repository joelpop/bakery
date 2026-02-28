# Implemented Checklist

This document records all completed features for the Café Sunshine Bakery Order Management System. Items were moved here from `IMPLEMENTATION_CHECKLIST.md` upon completion.

---

## Key Decisions

The following decisions were made during documentation review to resolve conflicts and gaps:

| Area | Decision |
|------|----------|
| **Application Name** | Café Sunshine |
| **Baker Role** | Read-only access to Products view (can view but not edit) |
| **Role Hierarchy** | No hierarchy - flat roles with explicit permissions |
| **Default Landing Page** | Admin → Dashboard; Baker → Bakery; Barista → Storefront |
| **Order Direct Links** | `/storefront/{orderId}` (opens storefront with order selected) |
| **Order Editing** | Admin can edit after production starts; Baker/Admin can add notes until picked up/cancelled |
| **Customer Deletion** | Soft delete (mark inactive); blocked if in-progress orders; cancels pre-production orders on confirmation |
| **Passkey Authentication** | Implement now (WebAuthn support in initial release) |
| **Notifications** | Superseded by order messaging system |
| **Seed Data** | Full demo data (locations, admin, products, customers, orders) |
| **KPI Deltas** | Show both comparisons (vs previous period AND vs same period last year) |
| **Concurrent Sessions** | Allowed (users can be logged in on multiple devices) |
| **Preferences View** | Full settings (profile, password, passkeys, notification prefs, display settings) |
| **Admin View** | Removed (not needed - admin functions have separate views) |

---

## Phase 1: Core Domain Model ✅

### 1.1 Enums (bakery-jpamodel)

- [x] **UserRoleCode** - User authorization roles
  - [x] ADMIN - Full system access
  - [x] BAKER - Kitchen staff access
  - [x] BARISTA - Front-of-house access

- [x] **OrderStatusCode** - Order lifecycle states
  - [x] IN_REVIEW - Order needs review (was NEW; also absorbs NOT_OK)
  - [x] VERIFIED - Order reviewed and accepted
  - [x] IN_PROGRESS - Being manufactured
  - [x] PRODUCED - Production completed (was BAKED)
  - [x] PACKAGED - Packaged for transport
  - [x] IN_TRANSIT - Being transported to pickup location (new)
  - [x] READY_FOR_PICK_UP - Available for pickup
  - [x] PICKED_UP - Order complete
  - [x] CANCELED - Order will not be fulfilled (was CANCELLED; NOT_OK removed)

- [x] **OrderItemStatusCode** - Order item lifecycle states
  - [x] PENDING_REVIEW, ACCEPTED, REJECTED, CANCELED, IN_PROGRESS, PRODUCED

- [x] **OrderActivityTypeCode** - Activity timeline entry types
  - [x] SYSTEM_EVENT - Auto-generated order change records
  - [x] STAFF_MESSAGE - Human-posted staff messages

### 1.2 Abstract Base Entities (bakery-jpamodel)

- [x] **AbstractEntity** - Base class for all entities
  - [x] id (Long) - Primary key, auto-generated
  - [x] version (Integer) - Optimistic locking

- [x] **AbstractAuditableEntity** - Base class for auditable entities (extends AbstractEntity)
  - [x] createdAt (Instant) - UTC timestamp, set on persist
  - [x] updatedAt (Instant) - UTC timestamp, set on update
  - [x] createdBy (UserEntity) - User who created
  - [x] updatedBy (UserEntity) - User who last updated
  - [x] @PrePersist and @PreUpdate lifecycle callbacks

### 1.3 JPA Entities (bakery-jpamodel)

- [x] **UserEntity** - Staff members (extends AbstractAuditableEntity)
  - [x] email (String, unique) - Login identifier
  - [x] firstName (String)
  - [x] lastName (String)
  - [x] passwordHash (String) - BCrypt hashed
  - [x] role (UserRoleCode)
  - [x] photo (byte[]) - Profile photo
  - [x] photoContentType (String)
  - [x] primaryLocation (Many-to-One → LocationEntity) - User's default working location

- [x] **CustomerEntity** - Customers who place orders (extends AbstractAuditableEntity)
  - [x] name (String)
  - [x] phoneNumber (String)
  - [x] email (String, optional)
  - [x] active (boolean, default: true) - For soft delete
  - [x] Relationship: orders (One-to-Many → OrderEntity)

- [x] **ProductEntity** - Bakery products (extends AbstractAuditableEntity)
  - [x] name (String, unique)
  - [x] description (String, optional)
  - [x] size (String) - e.g., "12 ppl", "individual"
  - [x] price (BigDecimal)
  - [x] available (boolean)
  - [x] batchable (boolean) - Whether items can be grouped on bakery board
  - [x] photo (byte[])
  - [x] photoContentType (String)

- [x] **LocationEntity** - Pickup locations (extends AbstractAuditableEntity)
  - [x] name (String, unique)
  - [x] address (String, optional)
  - [x] timezone (String) - IANA timezone ID (e.g., "America/New_York")
  - [x] defaultCountryCode (String) - Default country code for phone formatting (e.g., "+1")
  - [x] defaultAreaCode (String) - Default area code for 7-digit phone numbers (e.g., "212")
  - [x] active (boolean)
  - [x] sortOrder (Integer)

- [x] **OrderEntity** - Customer orders (extends AbstractAuditableEntity)
  - [x] status (OrderStatusCode)
  - [x] dueDate (LocalDate)
  - [x] dueTime (LocalTime)
  - [x] additionalDetails (String, optional)
  - [x] total (BigDecimal)
  - [x] discount (BigDecimal, optional)
  - [x] paid (Boolean)
  - [x] Relationship: customer (Many-to-One → CustomerEntity)
  - [x] Relationship: location (Many-to-One → LocationEntity)
  - [x] Relationship: items (One-to-Many → OrderItemEntity, cascade ALL)
  - [x] Inherits from AbstractAuditableEntity: createdAt, updatedAt (Instant), createdBy, updatedBy

- [x] **OrderItemEntity** - Order line items
  - [x] status (OrderItemStatusCode) - Item-level status tracking
  - [x] quantity (Integer)
  - [x] details (String, optional) - Per-item customizations
  - [x] unitPrice (BigDecimal) - Price snapshot at order time
  - [x] lineTotal (BigDecimal) - Calculated
  - [x] Relationship: order (Many-to-One → OrderEntity)
  - [x] Relationship: product (Many-to-One → ProductEntity)

- [x] **TilePositionEntity** - Persisted bakery board tile ordering within swimlanes
  - [x] swimlane (OrderItemStatusCode)
  - [x] dueDate (LocalDate)
  - [x] groupingKey (String)
  - [x] position (Integer)
  - [x] Unique constraint on (swimlane, dueDate, groupingKey)

- [x] **TileUndoEntryEntity** - Undo stack for bakery board tile transitions
  - [x] groupingKey (String)
  - [x] previousStatus (OrderItemStatusCode)
  - [x] sequenceNumber (Integer)
  - [x] activityIds (String) - Comma-separated activity IDs

- [x] **OrderStatusHistoryEntity** - Audit trail for order status changes
  - [x] order (Many-to-One → OrderEntity)
  - [x] status (OrderStatusCode)
  - [x] changedBy (Many-to-One → UserEntity)
  - [x] changedAt (Instant) - UTC timestamp

- [x] **OrderItemStatusHistoryEntity** - Audit trail for order item status changes
  - [x] orderItem (Many-to-One → OrderItemEntity)
  - [x] status (OrderItemStatusCode)
  - [x] changedBy (Many-to-One → UserEntity)
  - [x] changedAt (Instant) - UTC timestamp

- [x] **OrderActivityEntity** - Order activity timeline entries (extends AbstractEntity)
  - [x] order (Many-to-One → OrderEntity)
  - [x] type (OrderActivityTypeCode) - SYSTEM_EVENT or STAFF_MESSAGE
  - [x] text (String) - Activity description or message content
  - [x] author (Many-to-One → UserEntity, optional) - Staff member for messages
  - [x] referencedItem (Many-to-One → OrderItemEntity, optional) - Related item
  - [x] postedAt (Instant) - UTC timestamp
  - [x] read (boolean) - Read tracking for unread indicators

- ~~**NotificationEntity**~~ - Superseded by OrderActivityEntity and the order messaging system

### 1.4 Interface Projections (bakery-jpamodel)

- [x] **UserSummaryProjection** - User list grid display
- [x] **CustomerSummaryProjection** - Customer combo box
- [x] **ProductSummaryProjection** - Product admin grid
- [x] **ProductSelectProjection** - Order form product dropdown
- [x] **LocationSummaryProjection** - Location dropdown
- [x] **OrderListProjection** - Storefront order list (with items)
- [x] **OrderDashboardProjection** - Dashboard upcoming orders
- [x] **OrderTimeProjection** - Dashboard KPI queries
- [x] **OrderItemSummaryProjection** - Order item display
- ~~**NotificationSummaryProjection**~~ - Superseded by order messaging system

---

## Phase 2: Persistence Layer ✅

### 2.1 JPA Configuration (bakery-jpaclient)

- [x] **JpaConfig** - Spring Data JPA configuration
  - [x] @EntityScan for bakery-jpamodel
  - [x] @EnableJpaRepositories for bakery-jpaclient

### 2.2 Repositories (bakery-jpaclient)

- [x] **UserRepository**
  - [x] findByEmail / findByEmailIgnoreCase
  - [x] existsByEmail / existsByEmailAndIdNot
  - [x] findByRole / findByRoleOrderByLastNameAscFirstNameAsc
  - [x] countByRole
  - [x] Projection queries for UserSummaryProjection

- [x] **CustomerRepository**
  - [x] findByPhoneNumber
  - [x] findByPhoneNumberAndActiveTrue
  - [x] findByNameContainingIgnoreCaseAndActiveTrueOrderByName
  - [x] existsByPhoneNumber
  - [x] Projection queries for CustomerSummaryProjection (active only)

- [x] **ProductRepository**
  - [x] findByName / existsByName / existsByNameAndIdNot
  - [x] findByAvailableTrueOrderByNameAsc
  - [x] countByAvailableFalse (dashboard KPI)
  - [x] Projection queries for ProductSummaryProjection, ProductSelectProjection

- [x] **LocationRepository**
  - [x] existsByName / existsByNameAndIdNot
  - [x] findByActiveTrueOrderBySortOrderAsc
  - [x] countByActiveTrue
  - [x] Projection queries for LocationSummaryProjection

- [x] **OrderRepository**
  - [x] findByStatus
  - [x] findByDueDateOrderByDueTimeAsc
  - [x] findByDueDateBetweenOrderByDueDateAscDueTimeAsc
  - [x] findByCustomerIdOrderByDueDateDescDueTimeDesc
  - [x] countByStatus / countByDueDate / countByDueDateAndStatusNot
  - [x] Projection queries for OrderListProjection, OrderDashboardProjection
  - [x] Time-based queries for OrderTimeProjection

- [x] **OrderItemRepository**
  - [x] findByOrderIdOrderByIdAsc
  - [x] deleteByOrderId
  - [x] Projection queries for OrderItemSummaryProjection

- [x] **OrderActivityRepository** - Order messaging and activity timeline

- [x] **TilePositionRepository** - Bakery board tile position persistence
- [x] **TileUndoEntryRepository** - Bakery board undo stack management

- ~~**NotificationRepository**~~ - Superseded by OrderActivityRepository

---

## Phase 3: UI Models ✅

### 3.1 UI Model Enums (bakery-uimodel)

- [x] **UserRole** - UI representation of user roles
- [x] **OrderStatus** - UI representation of order statuses
- [x] **OrderItemStatus** - UI representation of order item statuses (PENDING_REVIEW, ACCEPTED, IN_PROGRESS, PRODUCED, REJECTED, CANCELED)

### 3.2 UI Model POJOs (bakery-uimodel)

- [x] **UserSummary** - User list display
- [x] **UserDetail** - User create/edit form
- [x] **CustomerSummary** - Customer combo box/autocomplete
- [x] **ProductSummary** - Product admin grid
- [x] **ProductSelect** - Order form product dropdown
- [x] **LocationSummary** - Location dropdown
- [x] **OrderList** - Storefront order list
- [x] **OrderDetail** - Order detail/edit
- [x] **OrderDashboard** - Dashboard upcoming orders
- [x] **OrderItemSummary** - Order item display
- [x] **OrderItemDetail** - Order item create/edit
- [x] **OrderActivity** - Order activity timeline entry
- [x] **BakeryTile** - Bakery board tile (grouped order items by product/status)
- [x] **BakeryTileDetail** - Individual order item within a bakery tile
- ~~**NotificationSummary**~~ - Superseded by OrderActivity

---

## Phase 4: Service Layer ✅

### 4.1 Service Interfaces (bakery-service)

- [x] **UserService**
  - [x] list() / search(query)
  - [x] get(id) / getByEmail(email)
  - [x] create(user) / update(id, user) / delete(id)
  - [x] changePassword(id, password)

- [x] **CustomerService**
  - [x] search(query) - Active customers only
  - [x] getByPhoneNumber(phone)
  - [x] create(customer) / update(id, customer)
  - [x] delete(id) - Soft delete with order status checks:
    - [x] Block if in-progress orders exist
    - [x] Cancel pre-production orders on confirmation
    - [x] Set active=false
  - [x] canDelete(id) - Returns deletion eligibility and affected orders

- [x] **ProductService**
  - [x] list() / listAvailable()
  - [x] get(id)
  - [x] create(product) / update(id, product) / delete(id)
  - [x] countUnavailable() (dashboard KPI)

- [x] **LocationService**
  - [x] list() / listActive()
  - [x] get(id)
  - [x] create(location) / update(id, location) / delete(id)

- [x] **OrderService**
  - [x] listUpcoming() / listByDateRange(start, end)
  - [x] listByStatus(status) / listByCustomer(customerId)
  - [x] get(id)
  - [x] create(order) / update(id, order)
  - [x] updateStatus(id, status)
  - [x] markAsPaid(id)
  - [x] Dashboard KPI methods

- [x] **OrderService** (additions for bakery workflow)
  - [x] updateItemStatus(orderId, itemId, newStatus, expectedItemVersion)
  - [x] updateGroupItemStatuses(productId, dueDate, currentStatus, newStatus)
  - [x] resolveItem(orderId, itemId, message, expectedItemVersion) - REJECTED → PENDING_REVIEW
  - [x] cancelItem(orderId, itemId, message, expectedItemVersion) - REJECTED → CANCELED
  - [x] togglePaid(id, expectedVersion) - Flip paid boolean
  - [x] togglePickedUp(id, expectedVersion) - READY_FOR_PICK_UP ↔ PICKED_UP

- [x] **BakeryService** - Bakery board data service
  - [x] listTiles(startDate, endDate) - Grouped order items as board tiles
  - [x] getTileDetails(groupingKey) - Contributing items for a tile
  - [x] saveTilePosition(groupingKey, swimlane, dueDate, position)
  - [x] getUndoStack(groupingKey)
  - [x] undoTileTransition(groupingKey)

- [x] **OrderActivityService** - Order messaging and activity timeline
  - [x] List activities for an order
  - [x] Post staff messages
  - [x] Record system events (status changes, edits)
  - [x] Unread message tracking per order

- ~~**NotificationService**~~ - Superseded by OrderActivityService

- [x] **CurrentUserService**
  - [x] getCurrentUserEmail()
  - [x] getCurrentUser()
  - [x] hasRole(role)
  - [x] isAdmin()

- [x] **DashboardService**
  - [x] getRemainingTodayCount()
  - [x] getNextPickupTime()
  - [x] getNewOrdersCount()
  - [x] getLastNewOrderTime()
  - [x] getTomorrowCount()
  - [x] getFirstPickupTimeTomorrow()
  - [x] getUnavailableProductsCount()
  - [x] getUpcomingOrders(limit)
  - [x] getMonthlyPickupData()
  - [x] getYearlyPickupData()
  - [x] getProductBreakdown()
  - [x] getYearOverYearSales()

- [x] **UserLocationService** - Session-scoped working location management
  - [x] setCurrentLocation(location)
  - [x] getCurrentLocation() → Optional
  - [x] isCurrentLocationSet()
  - [x] initializeFromUserPrimaryLocation()

- [x] **ClientDetailsService** - Lazy browser client details with timezone accessor
  - [x] getBrowserTimezone()
  - [x] isBrowserTimezoneSet()

- [x] **DataChangeNotifier** - Bridge interface for cross-session stale data signals
  - [x] Notifies UI layer of data changes from service layer

- [x] **StaleDataException** - Optimistic locking conflict detection

### 4.2 Service Implementations (bakery-jpaservice)

- [x] **JpaUserService**
- [x] **JpaCustomerService**
- [x] **JpaProductService**
- [x] **JpaLocationService**
- [x] **JpaOrderService**
- [x] **JpaOrderActivityService**
- ~~**JpaNotificationService**~~ - Superseded by JpaOrderActivityService
- [x] **JpaBakeryService** - Bakery board tile listing, grouping, undo stack
- [x] **OrderStatusRollUpHelper** - Derives order status from item statuses (package-private utility)
- [x] **JpaDashboardService**
- [x] **SessionUserLocationService** - @SessionScope implementation
- ~~**SessionUserTimezoneService**~~ - Removed; superseded by `VaadinClientDetailsService` in bakery-ui

### 4.3 MapStruct Mappers (bakery-jpaservice)

- [x] **UserMapper** - UserEntity ↔ UserSummary/UserDetail
- [x] **CustomerMapper** - CustomerEntity ↔ CustomerSummary
- [x] **ProductMapper** - ProductEntity ↔ ProductSummary/ProductSelect
- [x] **LocationMapper** - LocationEntity ↔ LocationSummary
- [x] **OrderMapper** - OrderEntity ↔ OrderList/OrderDetail/OrderDashboard
- [x] **OrderItemMapper** - OrderItemEntity ↔ OrderItemSummary/OrderItemDetail
- [x] **OrderActivityMapper** - OrderActivityEntity ↔ OrderActivity
- ~~**NotificationMapper**~~ - Superseded by OrderActivityMapper

---

## Phase 5: Security ✅

### 5.1 Security Configuration (bakery-app)

- [x] **SecurityConfig** - Spring Security + Vaadin integration
  - [x] VaadinSecurityConfigurer setup
  - [x] Custom login view configuration
  - [x] BCryptPasswordEncoder bean
  - [x] Session configuration (timeout, fixation protection)

- [x] **UserDetailsServiceImpl** - Load user by email
  - [x] Map UserEntity to Spring Security UserDetails
  - [x] Convert UserRoleCode to GrantedAuthority

- [x] **CurrentUserServiceImpl** - Access authenticated user
  - [x] Get current user from SecurityContext
  - [x] Role checking methods

### 5.2 Method Security (bakery-app)

- [x] Enable @PreAuthorize annotations

### 5.3 Password Validation

- [x] Entropy-based password strength calculation
- [x] Minimum 50 bits entropy requirement
- [x] Common password blocklist check
- [x] Strength indicator for UI feedback

---

## Phase 6: Core UI Components ✅

### 6.1 Application Shell (bakery-app)

- [x] **Application.java** configuration
  - [x] @StyleSheet(Lumo.STYLESHEET) and @StyleSheet(Lumo.UTILITY_STYLESHEET) - Lumo theme with utility classes
  - [x] @Push - Server push for real-time updates (stale data, messaging)
  - [x] @PWA - Progressive web app support
  - [x] Route scanning via root package placement (no @EnableVaadin needed)

### 6.2 Main Layout (bakery-ui)

- [x] **MainLayout** - Application shell with navigation
  - [x] App branding (Café Sunshine logo/name)
  - [x] Desktop: Top horizontal navigation tabs
  - [x] Mobile: Bottom tab bar with overflow menu
  - [x] User menu trigger (avatar)
  - [x] Role-based navigation item visibility
  - [x] Active tab highlighting
  - [x] Location selector ComboBox (navbar, session-scoped)
  - [x] Browser timezone detection (onAttach)
  - [x] Message broadcast registration/unregistration

### 6.3 Login View (bakery-ui)

- [x] **LoginView** - Authentication screen
  - [x] Centered layout with Café Sunshine logo/branding
  - [x] Email and password fields
  - [x] Login button
  - [x] Passkey login button (placeholder - Coming Soon)
  - [x] Error display for invalid credentials
  - [x] @AnonymousAllowed, autoLayout=false

---

## Phase 7: Admin Views ✅

### 7.1 Users View (bakery-ui)

- [x] **UsersView** - User management (Admin only)
  - [x] Searchable data grid
  - [x] Columns: Avatar, Email, Name, Role
  - [x] "+ New user" button
  - [x] Row click opens edit dialog
  - [x] @RolesAllowed(UserRole.ROLE_ADMIN)

- [x] **UserDialog** - Create/Edit user dialog
  - [x] Photo upload
  - [x] Email, First name, Last name fields
  - [x] Password field with show/hide toggle
  - [x] Role dropdown (Admin, Baker, Barista)
  - [x] Primary location dropdown
  - [x] Save, Cancel, Delete buttons
  - [x] Validation: unique email, password requirements
  - [x] Self-edit restrictions (cannot delete own account, cannot change own role)
  - [x] Stale data detection with banner

### 7.2 Products View (bakery-ui)

- [x] **ProductsView** - Product catalog management
  - [x] Searchable data grid
  - [x] Columns: Image, Name, Size, Price, Available
  - [x] "+ New product" button (Admin only)
  - [x] Edit/Delete actions (Admin only)
  - [x] Read-only mode for Baker role
  - [x] @RolesAllowed({UserRole.ROLE_ADMIN, UserRole.ROLE_BAKER}) with conditional editing

- [x] **ProductDialog** - Create/Edit product dialog
  - [x] Photo upload
  - [x] Name, Description, Size, Price fields
  - [x] Available toggle
  - [x] Save, Cancel, Delete buttons
  - [x] Validation: unique name, positive price
  - [x] Stale data detection with banner

### 7.3 Locations View (bakery-ui)

- [x] **LocationsView** - Location management (Admin only)
  - [x] Data grid with Name, Address, Timezone, Active, Sort Order
  - [x] "+ New location" button
  - [x] @RolesAllowed(UserRole.ROLE_ADMIN)

- [x] **LocationDialog** - Create/Edit location dialog
  - [x] Name, Address, Timezone fields
  - [x] Default country code, Default area code fields
  - [x] Active checkbox
  - [x] Sort order number field
  - [x] Save, Cancel, Delete buttons
  - [x] Validation: unique name, at least one active location
  - [x] Stale data detection with banner

---

## Phase 8: Storefront View ✅

### 8.1 Order List (bakery-ui)

- [x] **StorefrontView** - Order management
  - [x] Card-based order list
  - [x] Grouped by: Today, Tomorrow, This Week, Upcoming
  - [x] @RolesAllowed({UserRole.ROLE_ADMIN, UserRole.ROLE_BAKER, UserRole.ROLE_BARISTA})
  - [x] Auto-refresh via shared signals (cross-session stale data detection)
  - [x] Change highlighting for new/modified orders (CSS animations)

- [x] **OrderCard** - Individual order display
  - [x] Status badge with color coding
  - [x] Paid indicator (checkmark)
  - [x] Time and location
  - [x] Customer name
  - [x] Order items summary
  - [x] Unread message indicator
  - [x] Click to open detail view

### 8.2 Order Filtering

- [x] **FilterBar** - Order filtering component
  - [x] Date range picker (from/to)
  - [x] Status filter (multi-select)
  - [x] Location filter (dropdown with "Current Location" option)

### 8.3 Edit Order Dialog

- [x] **EditOrderDialog** - Single-page order creation/edit dialog (Spring prototype bean)
  - [x] Customer section (phone-first for quick lookup)
    - [x] Phone number field (first - triggers autofill popup)
      - [x] Autofill popup showing partial matches (ignoring punctuation)
      - [x] Popup displays phone number and customer name
      - [x] Selection populates both phone and name fields
      - [x] Phone formatting on blur (uses location's default country/area codes)
    - [x] Customer name field (second - conditionally editable)
      - [x] Read-only when existing customer selected
      - [x] Read-write when new phone number entered
      - [x] Skipped in tab order when existing customer selected
  - [x] Pickup section
    - [x] Location dropdown (auto-selects from user's current location or if only one active)
    - [x] Due date picker (defaults to today, min: today)
    - [x] Due time picker (time intervals)
    - [x] Additional details text area
  - [x] Order items section
    - [x] Product combo box with autocomplete
    - [x] Quantity field with stepper (min: 1)
    - [x] Item notes field
    - [x] Items grid with remove button
  - [x] Totals section
    - [x] Discount type (percent or currency) with amount field
    - [x] Total calculation (items minus discount)
  - [x] Cancel and Save buttons
  - [x] Listener pattern for dismiss events (`SaveEvent`, `CancelEvent`)
  - [x] `SaveEvent` returns created order and new customer flag

### 8.3.1 Global New Order Button

- [x] **Navigation bar action button** - Global access to create new orders
  - [x] Positioned at right end of nav tabs (before user menu)
  - [x] Desktop: Primary-styled nav item with "+ New order" text
  - [x] Mobile: Collapsed to plus icon with primary background
  - [x] Opens `EditOrderDialog` without navigation
  - [x] On save: refreshes current view if StorefrontView or DashboardView
  - [x] On save with new customer: also refreshes CustomerView if current

### 8.4 Order Detail View

- [x] **OrderDetailView** - Full order information and actions
  - [x] Complete order information display
  - [x] Status change dialog
  - [x] "Mark as Paid" button
  - [x] "Cancel Order" button (for pre-production orders)
  - [x] "Edit Order" button (opens EditOrderDialog, disabled for terminal orders)
  - [x] Order activity timeline (OrderActivityTimeline component)
    - [x] System events (status changes, edits)
    - [x] Staff messages with author and timestamp
    - [x] Post new message input
    - [x] Unread message tracking
  - [x] Auto-refresh via shared signals (cross-session stale data detection)
  - ~~"Mark as Not OK" button~~ *(removed — problems handled at item level via Bakery board REJECTED status)*

### 8.5 Direct Order Links

- [x] Route: `/orders/{orderId}`
- [x] Deep linking support for sharing orders

---

## Phase 9: Dashboard View ✅

### 9.1 KPI Cards (bakery-ui)

- [x] **DashboardView** - Business analytics
  - [x] @PermitAll

- [x] **KPI Cards** (top row)
  - [x] Remaining Today (count + next pickup time)
  - [x] Not Available (count + products unavailable)
  - [x] New (count + "last X ago" timestamp)
  - [x] Tomorrow (count + first pickup time)
  - [x] Month Total (count + dual delta: vs prev month AND vs same month last year)
  - [x] Year Total (count + dual delta: vs prev year AND vs same period last year)

### 9.4 Upcoming Orders Widget

- [x] **UpcomingOrdersPanel** - Condensed order list
  - [x] Status badge
  - [x] Paid indicator
  - [x] Day, time, location
  - [x] Customer name
  - [x] Items summary

---

## Phase 10: User Menu & Preferences ✅

### 10.1 Menu Trigger

- [x] User avatar in header
- [x] Click/tap to open dropdown

### 10.2 Menu Contents

- [x] **UserMenuPopup** - Dropdown menu (MenuBar in MainLayout)
  - [x] User profile section (avatar, name, email, role)
  - [x] "Preferences" link
  - [x] "About" link (Admin only)
  - [x] "Log out" button

### 10.3 Notifications

> Superseded by the order messaging system (see Phase 16.2). Staff communication happens in the context of orders via the activity timeline, with unread tracking and cross-session push notifications.

### 10.4 Preferences View

- [x] **PreferencesView** - Full user settings
  - [x] Route: `/preferences`
  - [x] @PermitAll

  - [x] **Profile Settings Section**
    - [x] Profile photo upload
    - [x] Display name (read-only)
    - [x] Email (read-only)
    - [x] Role badge (read-only)

  - [x] **Security Settings Section**
    - [x] Change password form
      - [x] Current password field
      - [x] New password with minimum length validation
      - [x] Confirm password

---

## Phase 11: Exception Handling ✅

### 11.1 Error Views (bakery-ui)

- [x] **NotFoundView** (404)
  - [x] Error icon (search icon)
  - [x] "Page Not Found" heading
  - [x] Helpful message
  - [x] Home link

- [x] **AccessDeniedView** (403)
  - [x] Displays as 404 for security (search icon, "Page Not Found")
  - [x] Returns SC_NOT_FOUND to avoid information disclosure
  - [x] Home link

- [x] **SystemErrorView** (500)
  - [x] Error icon (warning)
  - [x] "Something Went Wrong" heading
  - [x] Message
  - [x] Error reference ID (UUID-based)
  - [x] Home link
  - [x] Retry option (page reload button)

- [x] **InvalidParametersView** (400)
  - [x] Warning icon (exclamation circle)
  - [x] "Invalid Request" heading
  - [x] Validation error details (when safe to display)
  - [x] Home link

### 11.2 Logging

- [x] Error logging with correlation IDs (SystemErrorView)
- [x] Stack traces for 500 errors (logged server-side)

---

## Phase 12: Responsive Design ✅

### 12.1 Desktop Layout (> 768px)

- [x] Top horizontal navigation bar (MainLayout with desktop-navigation class)
- [x] Multi-column layouts (FormLayout responsive steps, CSS Grid with auto-fit)

### 12.2 Tablet Layout (768px)

- [x] Condensed top navigation (same as desktop, tabs adapt)
- [x] Adapted layouts (CSS Grid auto-fit handles transition)

### 12.3 Phone Layout (< 768px)

- [x] Bottom tab bar with overflow menu (MainLayout with bottom-navigation class)
- [x] Single-column layouts (FormLayout responsive steps, CSS Grid minmax)
- [x] Full-screen dialogs (responsive-dialog theme variant on all dialogs)
- [x] Native mobile controls (Vaadin uses native pickers on mobile)
- [x] Touch-optimized targets (44px minimum on buttons/inputs via CSS)

### 12.4 Transitions

- [x] Smooth layout transitions on resize (CSS Grid/Flexbox handles this)
- [x] Active view preserved during resize (SPA architecture maintains state)
- [x] Auto-close overflow menu on navigation (not applicable - bottom tabs navigate directly)

---

## Phase 13: Data Seeding ✅

### 13.1 Default Locations

- [x] Downtown Store (active, sortOrder: 1, timezone: America/New_York)
- [x] Central Bakery (active, sortOrder: 2, timezone: America/New_York)

### 13.2 Default Users

- [x] Admin: admin@cafe-sunshine.com (password: admin123, role: ADMIN)
- [x] Baker: baker@cafe-sunshine.com (password: baker123, role: BAKER)
- [x] Barista: barista@cafe-sunshine.com (password: barista123, role: BARISTA)
- [x] Passwords stored as BCrypt hashes

### 13.3 Demo Products (18 items)

**Pastries:**
- [x] Croissant (Individual, $3.50)
- [x] Chocolate Croissant (Individual, $4.00)
- [x] Almond Croissant (Individual, $4.50)
- [x] Cinnamon Roll (Individual, $4.50)
- [x] Blueberry Scone (Individual, $3.25)
- [x] Chocolate Muffin (Individual, $3.00)
- [x] Banana Nut Muffin (Individual, $3.25)
- [x] Danish Pastry (Individual, $3.75)

**Breads:**
- [x] Sourdough Loaf (Large, $7.00)
- [x] Baguette (Regular, $4.00)
- [x] Ciabatta (Regular, $4.50)
- [x] Whole Wheat Loaf (Large, $6.00)
- [x] Focaccia (Half Sheet, $8.00)

**Cakes and Tarts:**
- [x] Birthday Cake (12 people, $45.00)
- [x] Chocolate Cake (12 people, $48.00)
- [x] Fruit Tart (8 people, $28.00)
- [x] Cheesecake (10 people, $35.00)

**Specialty:**
- [x] Quiche Lorraine (6 people, $22.00)

### 13.4 Demo Customers (9 customers)

- [x] Alice Johnson, Bob Smith, Carol White, David Brown
- [x] Emma Davis, Frank Miller, Grace Lee, Henry Wilson, Iris Martinez
- [x] Each with phone number and email

### 13.5 Demo Orders (15 orders)

- [x] Orders across all statuses: IN_REVIEW, VERIFIED, IN_PROGRESS, PRODUCED, READY_FOR_PICK_UP, PICKED_UP, CANCELED
- [x] Orders due today, tomorrow, and in the past
- [x] Mix of paid and unpaid orders
- [x] Orders with single and multiple items
- [x] Orders split across both locations
- [x] No same-day multi-location orders per customer

### 13.6 Implementation

- [x] SQL-based seed data in `bakery-app/src/main/resources/data.sql`
- [x] Auto-loaded by Spring Boot with `spring.jpa.defer-datasource-initialization=true`
- [x] Audit fields (createdAt) populated via CURRENT_TIMESTAMP

---

## Phase 15: Bakery Workflow ✅

### 15.1 Enum Renames (Foundation)

- [x] **OrderStatusCode** renames: NEW→IN_REVIEW, BAKED→PRODUCED, CANCELLED→CANCELED, NOT_OK removed, IN_TRANSIT added
- [x] **OrderItemStatusCode** renames: NEW→PENDING_REVIEW, VERIFIED→ACCEPTED, NOT_OK→REJECTED, BAKED→PRODUCED, CANCELLED→CANCELED
- [x] **OrderStatus** (UI) renames + helpers: isTerminal(), isPreProduction(), isInProduction(), isDerived(), isManual()
- [x] **EnumMapper** updated with OrderItemStatus ↔ OrderItemStatusCode mappings
- [x] Entity defaults updated (IN_REVIEW, PENDING_REVIEW)
- [x] All cascade fixes across all modules

### 15.2 Product Batchable Field

- [x] `batchable` field on ProductEntity, ProductSummaryProjection, ProductSelectProjection
- [x] UI models: ProductSummary, ProductSelect
- [x] ProductDialog: batchable checkbox
- [x] Seed data: cakes/tarts non-batchable, everything else batchable

### 15.3 Item Status + Roll-Up Logic

- [x] **OrderItemStatus** UI enum with display names, descriptions, badge themes
- [x] Status field on OrderItemDetail, OrderItemSummary, OrderItemSummaryProjection
- [x] **OrderStatusRollUpHelper** — derives order status from item statuses (priority cascade)
- [x] Hold detection: ACCEPTED items on hold when siblings are PENDING_REVIEW or REJECTED
- [x] Today-only rule: cannot start production for future-dated items
- [x] Item-level service methods: updateItemStatus, updateGroupItemStatuses, resolveItem, cancelItem
- [x] Toggle methods: togglePaid, togglePickedUp

### 15.4 Bakery Board Data Service

- [x] **BakeryService** interface with tile listing, details, positioning, undo
- [x] **JpaBakeryService** implementation with batchable/non-batchable grouping
- [x] **TilePositionEntity** for persisted tile ordering
- [x] **TileUndoEntryEntity** for undo stack
- [x] Undo stack invalidation on storefront resolve/cancel/order cancel

### 15.5 Storefront Updates

- [x] Item status badges in OrderDetailView
- [x] Resolve and Cancel Item buttons for REJECTED items (with required message)
- [x] "Needs Attention" group for orders with rejected items (pink background)
- [x] Toggleable Paid and Picked Up buttons in OrderDetailView

### 15.6 Bakery Board View

- [x] **BakeryView** — Kanban board with toolbar and 4 swimlane columns
  - [x] @Route("bakery"), @RolesAllowed({ROLE_ADMIN, ROLE_BAKER})
  - [x] Date range preset buttons (Today, Today+Tomorrow, This Week)
  - [x] Default date range: Today (spec says Today+Tomorrow — see gaps)
  - [x] Auto-refresh via DataChangeSignals.orderVersion() + local ValueSignal trigger
  - [x] Unified drop handler dispatching reorder vs transition based on same/different status
  - [x] Active target computation with today-only rule and hold constraint
  - [x] Batch transition for batchable tiles (all items updated individually with version check)
  - [x] Reorder persistence via saveTileOrder() (full ordered list, not single position)
  - [x] Position save after transition via transitionTilePosition() (removes from source, inserts in target, resequences both)
  - [x] Undo via TileDetailOverlay UndoEvent → bakeryService.undoTileTransition()
  - [x] StaleDataException handling with warning notification
  - [x] Change highlighting on tiles (content hash tracking, gold fade for modified, scale-in for new)
- [x] **BakerySwimlane** — Status column with date-grouped tiles and overlay panel drag system
  - [x] Four swimlanes: To Review, Reviewed (Rejected+Accepted), In Progress, Done (Produced+Canceled)
  - [x] Overlay panel (left ~30%): Absolutely positioned status drop zones with First/Last sub-zones
  - [x] Per-status color theming via CSS custom property (--_panel-zone-color)
  - [x] Active zones: Tinted background, large chevron icons (ANGLE_DOUBLE_UP/DOWN), vertical status label
  - [x] Disabled zones: Grey wash, desaturated, no interactivity
  - [x] Hover/drag-over: Colored background fill, white icons and label
  - [x] Reorder insertion lines (3px blue) between tiles of active statuses
  - [x] No-op zone skipping for source status (adjacent to dragged tile)
  - [x] Auto-scroll on drag: Client-side JS, 50px edge detection, 6px/frame via requestAnimationFrame
  - [x] Tiles remain in DOM during drag (no layout shift)
  - [x] Clean drag mode exit: Remove panel, remove reorder zones, remove CSS classes
- [x] **BakeryTileComponent** — Draggable tile card with product info and indicators
  - [x] DragSource for drag-and-drop transitions
  - [x] Hold indicator (lock icon with tooltip) when on hold
  - [x] Unread message indicator (blue dot)
  - [x] Notes indicator (comment icon)
  - [x] Hover elevation (shadow increase, translateY -1px)
  - [x] Dragged state: 50% opacity, grabbing cursor
- [x] **TileDetailOverlay** — Dialog showing contributing order items (delegation pattern)
  - [x] Tile summary badges (quantity, size, order count)
  - [x] Contributing order cards with customer name, details, unread indicator
  - [x] "View Order" navigation links to OrderDetailView
  - [x] Undo button (temporary location until tile hover buttons are implemented)
- [x] **RejectMessageDialog** — Required reason dialog for rejections (delegation pattern)
  - [x] TextArea with "Rejection Reason" label, required validation
  - [x] Reject button (primary + error), Cancel button
  - [x] ConfirmEvent carries message text, CancelEvent
- [x] CSS: swimlane layout, tile styling, overlay panels, reorder zones, responsive breakpoints
  - [x] Tablet (≤1024px): overflow-x auto, min-width 220px per swimlane
  - [x] Phone (≤480px): scroll-snap one swimlane at a time (85vw)

---

## Phase 16: Cross-Cutting Features ✅

### 16.1 Stale Data Prevention

- [x] **Version tracking** - AbstractModel base class with version field
- [x] **Shared signals** - DataChangeSignals with SharedNumberSignal for cross-session notification
  - [x] orderVersion, userVersion, productVersion, locationVersion, messageVersion signals
- [x] **DataChangeNotifier** - Bridge interface (bakery-service) for service→UI layer communication
- [x] **DataChangeSignalUpdater** - UI-side listener that increments shared signals on data changes
- [x] **List view auto-refresh** - All list views (Storefront, Users, Products, Locations, Dashboard) react to signal changes
  - [x] Change detection with highlight animations for new/modified items
- [x] **Detail view live updates** - OrderDetailView refreshes on cross-session changes
- [x] **Edit dialog conflict prevention** - StaleDataBanner in UserDialog, ProductDialog, LocationDialog
  - [x] Pre-save version checks with StaleDataException
  - [x] Visual banner warning when external changes detected
- [x] **StaleDataHelper** - Reusable utility for stale data UI patterns

### 16.2 Order Messaging

- [x] **OrderActivityEntity** - Persistent storage for timeline entries
- [x] **OrderActivityService** - Service for posting messages and recording system events
- [x] **OrderActivityTimeline** - UI component for displaying and posting messages
- [x] **MessageBroadcaster** - Cross-session push notification for new messages
- [x] **VaadinUnreadMessageTracker** - Session-scoped unread message tracking
- [x] **Unread indicators** - OrderCard shows unread badge in storefront

### 16.3 Session-Scoped Services

- [x] **UserLocationService** - Working location persisted per session
  - [x] Location selector in MainLayout navbar
  - [x] Auto-initialized from user's primary location
  - [x] Used for pre-populating new order location and "Current Location" filter
- [x] **ClientDetailsService** - Browser client details lazily detected and cached per session
  - [x] Caches full ExtendedClientDetails in VaadinSession on first access
  - [x] Used by InstantMapper for UTC→local time conversion

---

## Documentation Gaps (Bakery Workflow) — All Resolved ✅

### Critical

- [x] **Order-level ↔ item-level status bridge** — Roll-up rules defined: item statuses drive pre-production order statuses via priority cascade. Hold behavior prevents indeterminate states. Order status renames: NEW→IN_REVIEW, BAKED→PRODUCED, CANCELLED→CANCELED, NOT_OK removed. New: IN_TRANSIT. Documented in bakery-workflow.md, order-status.md, orders.md.
- [x] **orders.md missing item status field** — Order Item data model updated with `status` field. Status progression rewritten to show derived pre-production and manual post-production. Cross-references to bakery-workflow.md added throughout.

### Important

- [x] **Storefront UI for rejected items** — Documented: rejection requires baker message; "Needs Attention" group with pink background at top of storefront; item statuses shown in order detail; Resolve and Cancel Item buttons with required messages. Updated bakery-workflow.md, storefront.md, orders.md, bakery.md, order-item-status.md.
- [x] **Posting messages from bakery detail overlay** — Yes, the overlay includes a message input for posting replies directly. Same behavior as Order Detail timeline. Updated bakery-workflow.md and bakery.md.
- [x] **Storefront toggleable Picked Up / Paid buttons** — Updated in storefront.md and orders.md to document toggleable behavior.
- [x] **Undo stack invalidation** — Undo entries invalidated when item status changes from outside the bakery board (storefront resolve/cancel). Order data edits do not affect undo stack. Updated bakery-workflow.md.

### Minor

- [x] **ProductSelectProjection missing `batchable`** — Resolved: `batchable` added to ProductSelectProjection, ProductSummaryProjection, and corresponding UI models.
- [x] **Order status spelling inconsistency** — Resolved. Both levels now use US English: CANCELED (single L). NOT_OK removed from order level; REJECTED is item-level only.
