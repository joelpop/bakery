# Navigation

The application features responsive navigation that adapts between desktop and mobile layouts.

## Screenshots
- Desktop: `legacy/images/storefront view/Desktop, order list.png` (top navigation visible)
- Phone menu: `legacy/images/storefront view/Phone, menu overflow.png`
- Phone admin menu: `legacy/images/admin view/Phone, selected menu item from overflow, menu open.png`

---

## Desktop Navigation

### Layout
A horizontal navigation bar at the top of the screen containing:

| Element | Position | Description |
|---------|----------|-------------|
| App Logo/Name | Left | "Café Sunshine" branding |
| Navigation Tabs | Center | Main view links |
| User Menu | Right | User avatar and dropdown |

### Navigation Tabs

| Tab | Icon | View | Access |
|-----|------|------|--------|
| Storefront | Clipboard/Edit icon | Order management | All users |
| Dashboard | Clock/Chart icon | Business analytics | All users |
| Users | Person icon | User management | Admin only |
| Products | Grid icon | Product catalog | Admin (edit), Baker (read-only) |
| Locations | Pin icon | Location management | Admin only |

### Active State
- Active tab is highlighted (typically with color and/or underline)
- Icon and label both visible

### Technical Note: Route Configuration
```
Routes:
  /storefront      -> StorefrontView (default for Baker/Barista)
  /storefront/{id} -> StorefrontView with order selected
  /dashboard       -> DashboardView (default for Admin)
  /users           -> UsersView (Admin only)
  /products        -> ProductsView (Admin edit, Baker read-only)
  /locations       -> LocationsView (Admin only)
  /preferences     -> PreferencesView (All authenticated users)
```

---

## Mobile Navigation

### Layout
A bottom tab bar with overflow menu for additional items.

### Primary Tabs (Bottom Bar)

| Tab | Icon | Description |
|-----|------|-------------|
| Storefront | Clipboard icon | Always visible |
| Dashboard | Clock icon | Always visible |
| Users | Person icon | Visible for Admin |
| Products | Grid icon | May be in overflow |
| (Overflow) | Hamburger menu | Additional items |

### Overflow Menu
When tapped, slides up to reveal additional navigation options:
- **Admin** section header
- Products link
- Other admin functions

### Active State
- Active tab highlighted with color
- Overflow menu items show selection state

---

## Role-Based Navigation

Navigation items are conditionally displayed based on user role:

| Role | Visible Navigation Items |
|------|-------------------------|
| Admin | Storefront, Dashboard, Users, Products, Locations |
| Baker | Storefront, Dashboard, Products (read-only) |
| Barista | Storefront, Dashboard |

### Security Note

Access control is enforced by Spring Security via `@RolesAllowed` annotations on view classes. See [Authorization](../security/authorization.md) for details. Navigation items are conditionally rendered based on the current user's role, but security is not enforced at the navigation level—unauthorized access attempts result in an access denied error page.

---

## Header Elements

### App Branding (Left)
- Application name: "Café Sunshine"
- May include logo
- Clicking returns to default view (Storefront)

### User Menu (Right)
- User avatar with notification badge
- Dropdown with profile options
- See [User Menu](user-menu.md) for details

---

## Responsive Breakpoints

| Breakpoint | Navigation Style |
|------------|------------------|
| Desktop (> 768px) | Top horizontal tabs |
| Tablet (768px) | Top tabs, possibly condensed |
| Phone (< 768px) | Bottom tab bar with overflow |

### Transition Behavior
- Navigation smoothly transitions between layouts
- Active view is preserved during resize
- Overflow menu auto-closes on view change
