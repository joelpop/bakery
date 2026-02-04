# Implementation Checklist

This checklist covers all features and capabilities documented for the Café Sunshine Bakery Order Management System. Items are ordered for logical implementation sequence.

---

## Key Decisions

The following decisions were made during documentation review to resolve conflicts and gaps:

| Area | Decision |
|------|----------|
| **Application Name** | Café Sunshine |
| **Baker Role** | Read-only access to Products view (can view but not edit) |
| **Role Hierarchy** | No hierarchy - flat roles with explicit permissions |
| **Default Landing Page** | Admin → Dashboard; Baker/Barista → Storefront |
| **Order Direct Links** | `/storefront/{orderId}` (opens storefront with order selected) |
| **Order Editing** | Admin can edit after production starts; Baker/Admin can add notes until picked up/cancelled |
| **Customer Deletion** | Soft delete (mark inactive); blocked if in-progress orders; cancels pre-production orders on confirmation |
| **Passkey Authentication** | Implement now (WebAuthn support in initial release) |
| **Notifications** | Deferred to future enhancement |
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
  - [x] NEW - Order just received
  - [x] VERIFIED - Order reviewed and accepted
  - [x] NOT_OK - Problem requiring attention
  - [x] CANCELLED - Order cancelled
  - [x] IN_PROGRESS - Being manufactured
  - [x] BAKED - Baking completed
  - [x] PACKAGED - Packaged for transport
  - [x] READY_FOR_PICK_UP - Available for pickup
  - [x] PICKED_UP - Order complete

- [x] **OrderItemStatusCode** - Order item lifecycle states (first 6 of order statuses)
  - [x] NEW, VERIFIED, NOT_OK, CANCELLED, IN_PROGRESS, BAKED

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
  - [x] photo (byte[])
  - [x] photoContentType (String)

- [x] **LocationEntity** - Pickup locations (extends AbstractAuditableEntity)
  - [x] name (String, unique)
  - [x] address (String, optional)
  - [x] timezone (String) - IANA timezone ID (e.g., "America/New_York")
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

- [ ] **NotificationEntity** - User-to-user notifications *(Deferred)*
  - [ ] message (String)
  - [ ] sentAt (LocalDateTime)
  - [ ] readAt (LocalDateTime, optional)
  - [ ] Relationship: sender (Many-to-One → UserEntity)
  - [ ] Relationship: recipient (Many-to-One → UserEntity)

  > **Note**: Notification functionality is deferred to future enhancement

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
- [ ] **NotificationSummaryProjection** - Notification panel *(Deferred)*

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

- [ ] **NotificationRepository** *(Deferred)*
  - [ ] findByRecipientIdAndReadAtIsNullOrderBySentAtDesc
  - [ ] countByRecipientIdAndReadAtIsNull
  - [ ] findTop10ByRecipientIdOrderBySentAtDesc
  - [ ] Paginated projection queries

---

## Phase 3: UI Models ✅

### 3.1 UI Model Enums (bakery-uimodel)

- [x] **UserRole** - UI representation of user roles
- [x] **OrderStatus** - UI representation of order statuses

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
- [ ] **NotificationSummary** - Notification display *(Deferred)*

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

- [ ] **NotificationService** *(Deferred)*
  - [ ] getUnreadForUser(userId)
  - [ ] countUnreadForUser(userId)
  - [ ] getRecentForUser(userId, limit)
  - [ ] send(senderId, recipientId, message)
  - [ ] markAsRead(id) / markAllAsRead(userId)

  > **Note**: Notification functionality is deferred to future enhancement

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

### 4.2 Service Implementations (bakery-jpaservice)

- [x] **JpaUserService**
- [x] **JpaCustomerService**
- [x] **JpaProductService**
- [x] **JpaLocationService**
- [x] **JpaOrderService**
- [ ] **JpaNotificationService** *(Deferred)*
- [x] **JpaDashboardService**

### 4.3 MapStruct Mappers (bakery-jpaservice)

- [x] **UserMapper** - UserEntity ↔ UserSummary/UserDetail
- [x] **CustomerMapper** - CustomerEntity ↔ CustomerSummary
- [x] **ProductMapper** - ProductEntity ↔ ProductSummary/ProductSelect
- [x] **LocationMapper** - LocationEntity ↔ LocationSummary
- [x] **OrderMapper** - OrderEntity ↔ OrderList/OrderDetail/OrderDashboard
- [x] **OrderItemMapper** - OrderItemEntity ↔ OrderItemSummary/OrderItemDetail
- [ ] **NotificationMapper** - NotificationEntity ↔ NotificationSummary *(Deferred)*

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
- [ ] Admin-only operations in UserService *(to be added with admin views)*
- [ ] Self-edit restrictions (cannot delete own account, cannot demote last admin) *(to be added with admin views)*

### 5.3 Password Validation

- [x] Entropy-based password strength calculation
- [x] Minimum 50 bits entropy requirement
- [x] Common password blocklist check
- [x] Strength indicator for UI feedback

---

## Phase 6: Core UI Components ✅

### 6.1 Application Shell (bakery-app)

- [x] **Application.java** updates
  - [x] @StyleSheet(Lumo.STYLESHEET) and @StyleSheet(Lumo.UTILITY_STYLESHEET) - Lumo theme with utility classes
  - [x] @EnableVaadin for route scanning

### 6.2 Main Layout (bakery-ui)

- [x] **MainLayout** - Application shell with navigation
  - [x] App branding (Café Sunshine logo/name)
  - [x] Desktop: Top horizontal navigation tabs
  - [x] Mobile: Bottom tab bar with overflow menu
  - [x] User menu trigger (avatar with notification badge)
  - [x] Role-based navigation item visibility
  - [x] Active tab highlighting

### 6.3 Login View (bakery-ui)

- [x] **LoginView** - Authentication screen
  - [x] Centered layout with Café Sunshine logo/branding
  - [x] Email and password fields
  - [x] Login button
  - [x] Passkey login button (placeholder - Coming Soon)
  - [x] Error display for invalid credentials
  - [ ] Redirect based on role: *(deferred until Dashboard/Storefront views exist)*
    - [ ] Admin → Dashboard
    - [ ] Baker/Barista → Storefront
  - [x] @AnonymousAllowed, autoLayout=false

### 6.4 Passkey Authentication (WebAuthn) *(Deferred)*

> **Note**: Full WebAuthn implementation deferred due to webauthn4j dependency issues with current Maven repository configuration. Passkey button added as placeholder.

- [ ] WebAuthn integration for passwordless login
- [ ] Passkey login flow on LoginView
- [ ] Passkey registration in PreferencesView
- [ ] Support for:
  - [ ] Platform authenticators (TouchID, FaceID, Windows Hello)
  - [ ] Roaming authenticators (YubiKey, FIDO2 keys)

---

## Phase 7: Admin Views ✅

### 7.1 Users View (bakery-ui)

- [x] **UsersView** - User management (Admin only)
  - [x] Searchable data grid
  - [x] Columns: Avatar, Email, Name, Role
  - [x] "+ New user" button
  - [x] Row click opens edit dialog
  - [x] @RolesAllowed("ADMIN")

- [x] **UserDialog** - Create/Edit user dialog
  - [x] Photo upload
  - [x] Email, First name, Last name fields
  - [x] Password field with show/hide toggle
  - [x] Role dropdown (Admin, Baker, Barista)
  - [x] Save, Cancel, Delete buttons
  - [x] Validation: unique email, password requirements
  - [x] Self-edit restrictions

### 7.2 Products View (bakery-ui)

- [x] **ProductsView** - Product catalog management
  - [x] Searchable data grid
  - [x] Columns: Image, Name, Size, Price, Available
  - [x] "+ New product" button (Admin only)
  - [x] Edit/Delete actions (Admin only)
  - [x] Read-only mode for Baker role
  - [x] @RolesAllowed({"ADMIN", "BAKER"}) with conditional editing

- [x] **ProductDialog** - Create/Edit product dialog
  - [x] Photo upload
  - [x] Name, Description, Size, Price fields
  - [x] Available toggle
  - [x] Save, Cancel, Delete buttons
  - [x] Validation: unique name, positive price

### 7.3 Locations View (bakery-ui)

- [x] **LocationsView** - Location management (Admin only)
  - [x] Data grid with Name, Address, Timezone, Active, Sort Order
  - [x] "+ New location" button
  - [x] @RolesAllowed("ADMIN")

- [x] **LocationDialog** - Create/Edit location dialog
  - [x] Name, Address, Timezone fields
  - [x] Active checkbox
  - [x] Sort order number field
  - [x] Save, Cancel, Delete buttons
  - [x] Validation: unique name, at least one active location
  - [ ] Deletion protection (cannot delete with orders) *(service layer handles this)*

---

## Phase 8: Storefront View ✅

### 8.1 Order List (bakery-ui)

- [x] **StorefrontView** - Order management
  - [x] Card-based order list
  - [x] Grouped by: Today, Tomorrow, This Week, Upcoming
  - [x] @RolesAllowed({"ADMIN", "BARISTA"})

- [x] **OrderCard** - Individual order display
  - [x] Status badge with color coding
  - [x] Paid indicator (checkmark)
  - [x] Time and location
  - [x] Customer name
  - [x] Order items summary
  - [x] Click to open detail view

### 8.2 Order Filtering

- [x] **FilterBar** - Order filtering component
  - [x] Date range picker (from/to)
  - [x] Status filter (multi-select)
  - [x] Location filter (dropdown)
  - [ ] Customer filter (searchable dropdown) *(deferred)*
  - [ ] Paid/Unpaid filter *(deferred)*
  - [ ] "Show past orders" checkbox *(deferred)*
  - [ ] "Clear filters" link *(deferred)*
  - [ ] Filter chips for applied filters *(deferred)*

### 8.3 Edit Order Dialog

- [x] **EditOrderDialog** - Single-page order creation/edit dialog
  - [x] Customer section
    - [x] Customer name field
    - [x] Phone number field
    - [ ] Customer autofill from phone number (lookup existing customer)
  - [x] Pickup section
    - [x] Location dropdown (auto-selects if only one active)
    - [x] Due date picker (defaults to today, min: today)
    - [ ] Due time picker (hourly slots: 08:00, 09:00, etc.)
    - [x] Additional details text area
  - [x] Order items section
    - [x] Product combo box with autocomplete
    - [x] Quantity field with stepper (min: 1)
    - [x] Item notes field
    - [x] Items grid with remove button
  - [ ] Totals section
    - [ ] Discount field (currency input)
    - [x] Total calculation (items minus discount)
  - [x] Cancel and Save buttons
  - [x] Listener pattern for dismiss events (`SaveClickEvent`, `CancelClickEvent`)
  - [x] `SaveClickEvent` returns created order and new customer flag

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
  - [ ] "Mark as Not OK" button (with problem description) *(deferred)*
  - [ ] Edit order details (role-based) *(deferred)*
  - [ ] Order history/audit trail *(deferred)*

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

### 9.2 Alerts Section

- [ ] **AlertsPanel** - Bulletin board *(deferred to future enhancement)*
  - [ ] Ingredient alerts
  - [ ] Problem orders (NOT_OK status)
  - [ ] Staff messages

### 9.3 Charts

- [ ] **PickupCharts** (second row) *(placeholder added, charts deferred)*
  - [ ] Pickups in [Current Month] - Daily bar chart
  - [ ] Pickups in [Current Year] - Monthly bar chart

- [ ] **SalesTrendChart** (third row) *(deferred)*
  - [ ] Sales Last Years - Multi-line year-over-year comparison

- [ ] **ProductsBreakdownChart** (bottom row) *(placeholder added, chart deferred)*
  - [ ] Products Delivered in [Current Month] - Donut/pie chart

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
  - [x] "Log out" button

### 10.3 Notifications *(Deferred)*

> Notification UI is deferred to future enhancement

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
      - [ ] Strength indicator *(deferred)*
    - [ ] Passkey management *(deferred - requires WebAuthn)*
      - [ ] List of registered passkeys
      - [ ] Add passkey button
      - [ ] Remove passkey button

  - [ ] **Notification Preferences Section** *(Deferred)*
    - [ ] Email notifications toggle
    - [ ] In-app notifications toggle

  - [ ] **Display Settings Section** *(Deferred)*
    - [ ] Theme selection (Light/Dark/System)

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
  - [ ] ~~Lock icon~~ *(intentionally omitted for security)*
  - [ ] ~~"Access Denied" heading~~ *(intentionally omitted for security)*

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
- [ ] User ID and request details in logs *(deferred)*
- [x] Stack traces for 500 errors (logged server-side)

---

## Phase 12: Responsive Design ✅

### 12.1 Desktop Layout (> 768px)

- [x] Top horizontal navigation bar (MainLayout with desktop-navigation class)
- [x] Multi-column layouts (FormLayout responsive steps, CSS Grid with auto-fit)
- [ ] Hover states and tooltips *(handled by Vaadin components)*

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

### 13.5 Demo Orders (12 orders)

- [x] Orders across all statuses: NEW, VERIFIED, IN_PROGRESS, BAKED, READY_FOR_PICK_UP, PICKED_UP, CANCELLED
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

## Phase 14: Testing

### 14.1 Unit Tests (bakery-jpaservice)

- [ ] Repository tests
- [ ] Service tests with actual repositories
- [ ] MapStruct mapper tests

### 14.2 UI Unit Tests (bakery-ui)

- [ ] TestBench UI Unit tests for views
- [ ] Component behavior tests

### 14.3 Integration Tests (bakery-app)

- [ ] Playwright end-to-end tests
- [ ] Authentication flow tests
- [ ] Order creation flow tests
- [ ] Admin CRUD tests

### 14.4 Security Tests

- [ ] Role-based access tests
- [ ] Method security tests
- [ ] Session management tests

---

## Implementation Notes

### Priority Order

1. **Foundation** (Phases 1-4): Domain model, persistence, services
2. **Security** (Phase 5): Must work before UI
3. **Core UI** (Phase 6): Login and navigation framework
4. **Admin Views** (Phase 7): Simpler CRUD patterns first
5. **Storefront** (Phase 8): Main business functionality
6. **Dashboard** (Phase 9): Analytics and KPIs
7. **User Menu** (Phase 10): Notifications and preferences
8. **Error Handling** (Phase 11): Polished error experience
9. **Responsive** (Phase 12): Mobile optimization
10. **Data & Testing** (Phases 13-14): Seed data and quality assurance

### Dependencies

- Entities must be created before projections
- Projections must be created before repositories use them
- Repositories must be created before services
- Services must be created before UI views
- Login must work before protected views can be tested
- Admin views (Users, Products, Locations) can be developed in parallel
- Storefront depends on Products, Locations, Customers
- Dashboard depends on Orders and Products

### Images

Screenshots from `docs/legacy/images/` are available for:
- Dashboard view
- Storefront view (order list, filters, new order dialog)
- User menu (notifications)
- Users view (CRUD, new/edit dialog)
- Admin view (phone overflow menu)

Missing screenshots will require creative interpretation based on documentation.
