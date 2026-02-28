# Implementation Checklist

Remaining tasks for the Café Sunshine Bakery Order Management System. Completed items have been moved to `IMPLEMENTED_CHECKLIST.md`.

---

## Phase 5: Security

### 5.2 Method Security (bakery-app)

- [ ] Admin-only operations in UserService *(to be added with admin views)*
- [ ] Self-edit restrictions (cannot delete own account, cannot demote last admin) *(to be added with admin views)*

---

## Phase 6: Core UI Components

### 6.3 Login View (bakery-ui)

- [ ] Redirect based on role after login *(currently all roles land on Storefront, the default route)*:
  - [ ] Admin → Dashboard
  - [ ] Baker → Bakery
  - [ ] Barista → Storefront

### 6.4 Passkey Authentication (WebAuthn) *(Deferred)*

> **Note**: Full WebAuthn implementation deferred due to webauthn4j dependency issues with current Maven repository configuration. Passkey button added as placeholder.

- [ ] WebAuthn integration for passwordless login
- [ ] Passkey login flow on LoginView
- [ ] Passkey registration in PreferencesView
- [ ] Support for:
  - [ ] Platform authenticators (TouchID, FaceID, Windows Hello)
  - [ ] Roaming authenticators (YubiKey, FIDO2 keys)

---

## Phase 7: Admin Views

### 7.3 Locations View (bakery-ui)

- [ ] LocationDialog: Deletion protection (cannot delete with orders) *(service layer handles this)*

---

## Phase 8: Storefront View

### 8.2 Order Filtering

- [ ] Customer filter (searchable dropdown) *(deferred)*
- [ ] Paid/Unpaid filter *(deferred)*
- [ ] "Show past orders" checkbox *(deferred)*
- [ ] "Clear filters" link *(deferred)*
- [ ] Filter chips for applied filters *(deferred)*

---

## Phase 9: Dashboard View

### 9.2 Alerts Section

- [ ] **AlertsPanel** - Bulletin board *(deferred to future enhancement)*
  - [ ] Ingredient alerts
  - [ ] Problem orders (IN_REVIEW status with rejected items)
  - [ ] Staff messages

### 9.3 Charts

- [ ] **PickupCharts** (second row) *(placeholder added, charts deferred)*
  - [ ] Pickups in [Current Month] - Daily bar chart
  - [ ] Pickups in [Current Year] - Monthly bar chart

- [ ] **SalesTrendChart** (third row) *(deferred)*
  - [ ] Sales Last Years - Multi-line year-over-year comparison

- [ ] **ProductsBreakdownChart** (bottom row) *(placeholder added, chart deferred)*
  - [ ] Products Delivered in [Current Month] - Donut/pie chart

---

## Phase 10: User Menu & Preferences

### 10.4 Preferences View

- [ ] Password strength indicator *(placeholder exists, visual feedback deferred)*
- [ ] Passkey management *(deferred - requires WebAuthn)*
  - [ ] List of registered passkeys
  - [ ] Add passkey button
  - [ ] Remove passkey button
- [ ] **Display Settings Section** *(Deferred)*
  - [ ] Theme selection (Light/Dark/System)

---

## Phase 11: Exception Handling

### 11.2 Logging

- [ ] User ID and request details in logs *(deferred)*

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

## Implementation Gaps (Bakery Board)

The following features are documented in the spec but not yet implemented, or differ from the spec.

- [ ] **Tile hover/tap action buttons** — Spec calls for status transition buttons, undo button, and reorder arrows (Top/Up/Down/Bottom) appearing on hover (desktop) or tap (mobile). Not implemented; status transitions are performed exclusively via drag-and-drop. Undo is accessible from the detail overlay. Click currently opens the detail overlay directly.
- [ ] **Drag-to-undo** — Spec says dragging a tile to the swimlane it came from performs an undo. Not implemented; undo is accessible from the detail overlay until tile buttons are added.
- [ ] **Message viewing/posting in detail overlay** — TileDetailOverlay shows an "unread messages" indicator and "View Order" links, but does not embed the activity timeline or a message input. Spec calls for inline message posting from the overlay.
- [ ] **Selectable items in detail overlay** — Spec says items should be selectable to filter the detail display. Not implemented; all items always shown.
- [ ] **Unread message read-marking** — Opening the detail overlay should mark unread messages as read. Not implemented.
- [ ] **Custom date range picker** — Only preset buttons implemented; no custom start/end date selector.
- [ ] **Default date range** — Spec says Today+Tomorrow; implementation defaults to Today.
- [ ] **Incomplete past orders** — Past-due items not in terminal status should always appear regardless of date range. Currently filtered by selected date range.

---

## Implementation Notes

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
