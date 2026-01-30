# Authorization

This document describes role-based access control (RBAC) in the Bakery application.

## Role Hierarchy

```
ADMIN
  └── BAKER
        └── BARISTA (base permissions)
```

All roles have access to:
- Storefront (view and create orders)
- Dashboard (view analytics)
- User Menu (profile, notifications, logout)

Admin-only access:
- Users view (manage staff)
- Products view (manage catalog)
- Admin functions

## View-Level Security

### Vaadin Security Annotations

```java
// Allow all authenticated users
@Route(value = "storefront", layout = MainLayout.class)
@PermitAll
public class StorefrontView extends VerticalLayout { }

// Admin only
@Route(value = "users", layout = MainLayout.class)
@RolesAllowed("ADMIN")
public class UsersView extends VerticalLayout { }

// Anonymous access (login page)
@Route("login")
@AnonymousAllowed
public class LoginView extends VerticalLayout { }
```

### Available Annotations

| Annotation | Description |
|------------|-------------|
| `@AnonymousAllowed` | No authentication required |
| `@PermitAll` | Any authenticated user |
| `@RolesAllowed("ADMIN")` | Specific role(s) required |
| `@DenyAll` | No access (for testing) |

## Navigation Security

### Conditional Navigation Items

```java
public class MainLayout extends AppLayout {

    private final CurrentUserService currentUserService;

    public MainLayout(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
        createNavigation();
    }

    private void createNavigation() {
        var nav = new Tabs();

        // Always visible
        nav.add(createTab("Storefront", StorefrontView.class));
        nav.add(createTab("Dashboard", DashboardView.class));

        // Admin only
        if (currentUserService.isAdmin()) {
            nav.add(createTab("Users", UsersView.class));
            nav.add(createTab("Products", ProductsView.class));
            nav.add(createTab("Admin", AdminView.class));
        }

        addToNavbar(nav);
    }

    private Tab createTab(String label, Class<? extends Component> view) {
        var link = new RouterLink(label, view);
        return new Tab(link);
    }
}
```

### Navigation Access Check

```java
public class MainLayout extends AppLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Additional check for programmatic navigation
        var targetClass = event.getNavigationTarget();

        if (isAdminView(targetClass) && !currentUserService.isAdmin()) {
            event.rerouteTo(StorefrontView.class);
            Notification.show("Access denied", 3000, Notification.Position.TOP_CENTER);
        }
    }

    private boolean isAdminView(Class<?> viewClass) {
        return viewClass == UsersView.class ||
               viewClass == ProductsView.class ||
               viewClass == AdminView.class;
    }
}
```

## Method-Level Security

### Service Layer Security

```java
@Service
@Transactional
public class JpaUserService implements UserService {

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public User create(User user) {
        // Only admins can create users
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or #id == authentication.principal.id")
    public User update(Long id, User user) {
        // Admins can update anyone, users can update themselves
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        // Only admins can delete users
    }
}
```

### Enable Method Security

```java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {
    // Method security enabled
}
```

## Data-Level Security

### Row-Level Security Example

```java
@Service
public class JpaOrderService implements OrderService {

    private final CurrentUserService currentUserService;

    @Override
    public List<OrderSummary> list(OrderFilter filter) {
        var user = currentUserService.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("No user"));

        // Bakers/Baristas only see their location's orders
        if (!currentUserService.isAdmin()) {
            filter = filter.withLocation(user.getDefaultLocation());
        }

        return orderRepository.searchOrders(filter);
    }
}
```

## UI Component Security

### Conditional Component Rendering

```java
public class UserCard extends Div {

    public UserCard(User user, CurrentUserService currentUserService) {
        add(new Span(user.getFullName()));
        add(new Span(user.getEmail()));

        // Only admins see edit/delete buttons
        if (currentUserService.isAdmin()) {
            var editButton = new Button("Edit", e -> editUser(user));
            var deleteButton = new Button("Delete", e -> deleteUser(user));
            add(editButton, deleteButton);
        }
    }
}
```

### Disable vs Hide

```java
// Hide for non-admins (they don't know it exists)
if (currentUserService.isAdmin()) {
    add(deleteButton);
}

// Show but disable (they know it exists but can't use it)
deleteButton.setEnabled(currentUserService.isAdmin());
deleteButton.setTooltipText("Only admins can delete");
```

## API Security (if REST endpoints exist)

```java
@RestController
@RequestMapping("/api/users")
public class UserApiController {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> list() {
        return userService.list(null);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public User create(@RequestBody User user) {
        return userService.create(user);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public User getCurrentUser() {
        return currentUserService.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
```

## Security Testing

### Test with Roles

```java
@SpringBootTest
@WithMockUser(roles = "ADMIN")
class AdminAccessTest {

    @Test
    void adminCanAccessUsersView() {
        // Test admin access
    }
}

@SpringBootTest
@WithMockUser(roles = "BAKER")
class BakerAccessTest {

    @Test
    void bakerCannotAccessUsersView() {
        // Test access denied
    }
}
```

### Custom Test User

```java
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockBakeryUserSecurityContextFactory.class)
public @interface WithMockBakeryUser {
    String email() default "test@cafe-sunshine.com";
    UserRoleCode role() default UserRoleCode.BARISTA;
}
```

## Audit Trail (Future Enhancement)

Track who did what:

```java
@Entity
public class AuditLog {
    private Long id;
    private String action;  // CREATE, UPDATE, DELETE
    private String entityType;
    private Long entityId;
    private String userId;
    private LocalDateTime timestamp;
    private String details;  // JSON of changes
}
```
