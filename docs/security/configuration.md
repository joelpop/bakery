# Security Configuration

This document describes the Spring Security configuration for the Bakery application.

## Security Filter Chain

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends VaadinWebSecurity {

    private final UserDetailsServiceImpl userDetailsService;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // Vaadin security configuration
        super.configure(http);

        // Custom login view
        setLoginView(http, LoginView.class);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

## Vaadin Security Integration

Vaadin provides `VaadinWebSecurity` base class that:
- Handles CSRF protection automatically
- Secures Vaadin internal endpoints
- Integrates with Spring Security context

### Login View Configuration

```java
@Route("login")
@PageTitle("Login | Café Sunshine")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        addClassName("login-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);

        add(
            new H1("Café Sunshine"),
            loginForm
        );
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters()
                .getParameters().containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
```

## UserDetailsService Implementation

```java
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .map(this::createUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private UserDetails createUserDetails(UserEntity user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(getAuthorities(user.getRole()))
                .build();
    }

    private Collection<? extends GrantedAuthority> getAuthorities(UserRoleCode role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
```

## Session Configuration

```properties
# application.properties
server.servlet.session.timeout=30m
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=true  # Enable in production (HTTPS)
```

## CORS Configuration (if needed for API)

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of("https://cafe-sunshine.com"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
}
```

## Password Requirements

Passwords are validated based on entropy (randomness) rather than arbitrary character class requirements. This approach allows users to create memorable passphrases while ensuring sufficient security.

### Entropy-Based Validation

| Requirement | Description |
|-------------|-------------|
| Minimum entropy | 50 bits (approximately equivalent to a random 10-character password) |
| Minimum length | 8 characters |
| Maximum length | 128 characters |
| Blocklist check | Rejects common passwords and breached passwords |

### Entropy Calculation

Password entropy is estimated based on:

| Factor | Description |
|--------|-------------|
| Character pool size | Number of unique character types used (lowercase, uppercase, digits, symbols) |
| Password length | Longer passwords have exponentially more entropy |
| Pattern detection | Repeated characters, sequences, and keyboard patterns reduce entropy |
| Dictionary words | Common words contribute less entropy than random characters |

### User Feedback

The password field displays a strength indicator:

| Strength | Entropy | Indicator |
|----------|---------|-----------|
| Weak | < 30 bits | Red bar, password rejected |
| Fair | 30-49 bits | Orange bar, password rejected |
| Good | 50-69 bits | Yellow bar, password accepted |
| Strong | 70-89 bits | Green bar, password accepted |
| Very Strong | 90+ bits | Full green bar, password accepted |

### Examples

| Password | Entropy | Accepted |
|----------|---------|----------|
| `password` | ~3 bits | No (blocklisted) |
| `Tr0ub4dor&3` | ~28 bits | No (too low) |
| `correct horse battery staple` | ~44 bits | No (too low) |
| `purple-elephant-jumping-rope` | ~56 bits | Yes |
| `Xk9#mP2$vL7@nQ4!` | ~85 bits | Yes |

## Security Headers

```java
@Override
protected void configure(HttpSecurity http) throws Exception {
    super.configure(http);

    http.headers(headers -> headers
        .contentSecurityPolicy(csp -> csp
            .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline'"))
        .frameOptions(frame -> frame.sameOrigin())
        .xssProtection(xss -> xss.disable()) // Vaadin handles XSS
    );
}
```
