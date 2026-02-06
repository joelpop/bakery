# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is **Bakery**, a Vaadin 25 + Spring Boot 4 application using Java 25. It follows a **multi-module Maven architecture** with **layered separation** where code is organized into separate Maven modules by technical concern. The application uses **MapStruct** to map between JPA interface projections, UI models, and JPA entities, ensuring complete decoupling between persistence and presentation layers.

## Tech Stack

- Maven
- Java 25
- Vaadin 25
- Spring Boot 4
- Spring Data JPA
- MapStruct (for mapping from JPA interface projections to UI model objects and from UI model objects to JPA entities)
- Vaadin TestBench (for unit tests)
- Playwright (for integration tests)
- Git

## MCP Servers

This project was preconfigured with Model Context Protocol (MCP) servers in `.mcp.json` to provide specialized tooling assistance to Claude Code. These servers were selected based on the project's toolchain and the broad, cross-platform availability of the tooling needed to run the MCP servers (http and node/npm/npx).

### Sample MPC Server File

```json
{
  "mcpServers": {
    "fetch": {
      "command": "npm",
      "args": ["exec", "--silent", "--", "fetch-mcp"]
    },
    "java": {
      "type": "http",
      "url": "https://www.javadocs.dev/mcp"
    },
    "playwright": {
      "command": "npx",
      "args": ["-y", "@playwright/mcp"]
    },
    "spring-docs": {
      "command": "npx",
      "args": ["-y", "@enokdev/springdocs-mcp@latest"]
    },
    "vaadin": {
      "type": "http",
      "url": "https://mcp.vaadin.com/docs"
    }
  }
}
```

**Note:** Node.js must be installed and `npm`/`npx` must be available on your PATH for the command-based MCP servers to work. The HTTP-based servers (java, vaadin) require no local installation.

### Fetch MCP Server (`mcp__fetch__`)

Provides URL fetching capabilities for retrieving web content.

**Available Tools:**
- `fetch_url` - Fetch content from a URL (HTML, text, or images). Returns Markdown by default.
- `fetch_youtube_transcript` - Fetch transcript from a YouTube video URL.

### IDE MCP Server (`mcp__ide__`)

Provides IDE integration for diagnostics.

**Available Tools:**
- `getDiagnostics` - Get diagnostic information, optionally filtered by URI.

### Java MCP Server (`mcp__java__`)

Provides Java/Maven utilities for dependency management and Javadoc lookup.

**Available Tools:**
- `get_latest_version` - Get the latest version of a Maven artifact from Maven Central.
- `get_javadoc_content_list` - List contents of a Javadoc JAR for a specific artifact.
- `get_javadoc_symbol_contents` - Get Javadoc content for a specific symbol/class.
- `symbol_to_artifact` - Find the Maven groupId and artifactId for a given class/package name.

### Playwright MCP Server (`mcp__playwright__`)

Provides browser automation for UI testing and interaction. Useful for testing the Vaadin application.

**Available Tools:**
- `browser_navigate` - Navigate to a URL.
- `browser_snapshot` - Capture accessibility snapshot (preferred over screenshot for actions).
- `browser_take_screenshot` - Take a screenshot of the current page.
- `browser_click` - Click on an element.
- `browser_type` - Type text into an element.
- `browser_fill_form` - Fill multiple form fields at once.
- `browser_press_key` - Press a keyboard key.
- `browser_select_option` - Select an option in a dropdown.
- `browser_hover` - Hover over an element.
- `browser_drag` - Drag and drop between elements.
- `browser_wait_for` - Wait for text, text disappearance, or a timeout.
- `browser_tabs` - List, create, close, or select browser tabs.
- `browser_evaluate` - Execute JavaScript in the browser.
- `browser_console_messages` - Get console messages.
- `browser_network_requests` - Get network requests.
- `browser_close` - Close the browser page.
- `browser_install` - Install the browser if not available.

### Spring Docs MCP Server (`mcp__spring-docs__`)

Provides access to Spring Boot documentation, guides, tutorials, and best practices.

**Available Tools:**
- `search_spring_docs` - Search Spring Boot documentation by keywords. Filter by `docType`: "guides", "reference", "api", or "all".
- `search_spring_projects` - Search all Spring projects on spring.io/projects.
- `get_spring_project` - Get details of a specific Spring project (e.g., "spring-boot", "spring-security").
- `get_all_spring_guides` - List all Spring guides, optionally filtered by category.
- `get_spring_guide` - Get content of a specific guide with configurable detail level.
- `get_spring_reference` - Get a section of the Spring Boot reference documentation.
- `search_spring_concepts` - Search Spring Boot concepts by category (core, web, data, security, testing, production).
- `search_spring_ecosystem` - Search across the entire Spring ecosystem.
- `get_spring_tutorial` - Get step-by-step tutorials for specific features.
- `compare_spring_versions` - Compare features between Spring Boot versions.
- `get_spring_best_practices` - Get best practices by category (architecture, performance, security, testing, configuration, deployment).
- `diagnose_spring_issues` - Diagnose common Spring Boot issues and get solutions.

### Vaadin MCP Server (`mcp__vaadin__`)

Provides access to Vaadin documentation, component APIs, and best practices. **Always call `get_vaadin_primer` first** before working with Vaadin to get current information about modern Vaadin development.

**Available Tools:**
- `get_vaadin_primer` - Returns comprehensive primer about modern Vaadin development (2025+). Essential to avoid outdated assumptions.
- `search_vaadin_docs` - Search Vaadin documentation with hybrid semantic + keyword search. Specify `ui_language` as "java", "react", or "common".
- `get_full_document` - Retrieve complete documentation pages by file path.
- `get_vaadin_version` - Get the latest stable Vaadin version.
- `get_components_by_version` - List all components available in a specific Vaadin version.
- `get_component_java_api` - Get Java API documentation for a component.
- `get_component_react_api` - Get React API documentation for a component.
- `get_component_web_component_api` - Get Web Component/TypeScript API documentation.
- `get_component_styling` - Get styling/theming documentation for a component.

**Note**: Claude Code must be restarted after modifying `.mcp.json` to load MCP server changes.

## Package Structure

All Java packages follow the base package pattern: `org.vaadin.bakery.*`

The Java code is organized into the following packages (object `User` and its derivatives are used below hypothetically):

- `org.vaadin.bakery` - Base package; `BakeryBase` marker interface for type-safe component scanning by Spring
- `org.vaadin.bakery.common.util` - shared libraries
- `org.vaadin.bakery.jpamodel.code` - JPA entity enums (e.g., `UserTypeCode`)
- `org.vaadin.bakery.jpamodel.entity` - JPA entity classes (e.g., `UserEntity`)
- `org.vaadin.bakery.jpamodel.projection` - JPA interface projections (e.g., `UserNameProjection`, `UserSummaryProjection`, `UserDetailProjection`)
- `org.vaadin.bakery.jpaclient.config` - Spring Data configuration (e.g., `JpaConfig`)
- `org.vaadin.bakery.jpaclient.repository` - Spring Data repositories
- `org.vaadin.bakery.jpaservice` - Service implementations (e.g., `JpaUserService`)
- `org.vaadin.bakery.jpaservice.mapper` - MapStruct mappers
- `org.vaadin.bakery.uimodel.data` - UI model POJOs (e.g., `UserName`, `UserSummary`, `UserDetail`)
- `org.vaadin.bakery.uimodel.type` - UI model enums (e.g., `UserType`)
- `org.vaadin.bakery.service` - Service interfaces
- `org.vaadin.bakery.service.util` - Service utilities (e.g., `PageRequest`)
- `org.vaadin.bakery.ui` - Shared UI components (e.g., `MainLayout`, `ViewToolbar`)
- `org.vaadin.bakery.ui.component` - Shared UI components
- `org.vaadin.bakery.ui.view` - Views and their related classes (each view within its own package)
- `org.vaadin.bakery` - Main Application class (in root package so UI subpackages are auto-scanned)
- `org.vaadin.bakery.app.config` - Application configuration
- `org.vaadin.bakery.app.config.security` - Application security configuration

## Architecture and Organization

Multi-module Maven project with layered separation where code is organized into separate modules by technical concern. Java packages are organized into Maven modules with corresponding names (e.g., package `org.vaadin.bakery.ui` would be located in module `bakery-ui`).

### Module Structure

The project is organized as a Maven multi-module build with the following modules:

**Module Inheritance Graph**

```
spring-boot-starter-parent
└── bakery
    ├── bakery-common
    ├── bakery-jpamodel
    ├── bakery-jpaclient
    ├── bakery-uimodel
    ├── bakery-service
    ├── bakery-jpaservice
    ├── bakery-ui
    └── bakery-app
```

### Module Brief Descriptions & Dependencies

- **bakery** - Parent of all the other project modules
- **bakery-common** - Shared utility classes and enums
- **bakery-jpamodel** - JPA entities with `@Entity` annotations, code enums, interface projections
    - **Naming convention**: Entities are suffixed with `Entity` (e.g., `UserEntity`)
- **bakery-jpaclient** - Spring Data JPA repository interfaces (depends on: `bakery-jpamodel`)
    - Contains Spring configuration (e.g., `JpaConfig`)
- **bakery-uimodel** - Plain POJOs for UI layer, type enums (no persistence knowledge)
    - **Naming convention**: UI models have no suffix (e.g., `User`)
- **bakery-service** - Service interfaces that work with UI models only (depends on: `bakery-uimodel`)
- **bakery-jpaservice** - Service implementations using MapStruct to convert from JPA interface projections to UI models and from UI models to JPA entities (depends on: bakery-service & bakery-jpaclient)
    - **Important**: Must NOT depend on UI libraries (e.g., Vaadin) to maintain layer separation
- **bakery-ui** - Vaadin UI components (views, layouts, and components) (depends on: `bakery-service`)
    - Contains only UI layer code (no application infrastructure)
    - Pure UI library module with no direct persistence dependencies
- **bakery-app** - Spring Boot application entry point (main executable JAR) (depends on: `bakery-ui` & `bakery-jpaservice`)
    - Contains `Application.java` in root package (`org.vaadin.bakery`) so UI subpackages are auto-scanned
    - Contains `BakeryBase` marker interface in root package
    - Contains security configuration
    - Contains `application.properties`
    - Contains frontend resources
    - Depends on `bakery-jpaservice` at runtime only (enforces layer separation)
    - Assembles and runs the complete application

**Important**: `bakery-jpaservice` must be a **runtime-only** dependency of `bakery-app`. This enforces compile-time separation--the UI layer may not reference JPA entities, repositories, or service implementations during compilation.

### Compile-Time Layer Separation

The `bakery-jpaservice` dependency in `bakery-app` has **`<scope>runtime</scope>`**. Additionally, `bakery-ui` has no direct dependency on `bakery-jpaservice` at all. This enforces:
- ✅ UI code can call service interfaces
- ✅ UI code can work with UI models
- ❌ UI code **cannot** import or reference JPA entities at compile time
- ❌ UI code **cannot** import or reference repositories at compile time
- ❌ UI code **cannot** import or reference MapStruct mappers at compile time

Additionally, service implementations must not depend on UI libraries:
- ❌ Service code **cannot** import or reference Vaadin classes

This guarantees proper layering and prevents accidental coupling between UI and persistence layers. The application layer (`bakery-app`) is responsible for wiring everything together at runtime.

### Layer Separation Details

- **Presentation Layer** (`bakery-ui`, `bakery-uimodel`)
    - Vaadin UI components module
    - Contains Vaadin views, components, and layouts
    - Vaadin views work exclusively with UI model objects (no suffix)
    - Pure library module with no direct persistence dependencies
    - Cannot reference JPA entities, repositories, or service implementations at compile time
    - Has no knowledge of Persistence layer implementation

- **Persistence Layer** (`bakery-jpamodel` + `bakery-jpaclient`)
    - Contains JPA entities with full persistence annotations
    - Repositories for data access
    - Contains `JpaConfig` class with `@EntityScan` and `@EnableJpaRepositories` annotations
    - Has no knowledge of the presentation layer

- **Service Interface Layer** (`bakery-service`)
    - Defines business operations using UI models
    - No dependencies on JPA or persistence
    - Allows for multiple implementations

- **Service Implementation Layer** (`bakery-jpaservice`)
    - Implements service interfaces
    - **Uses MapStruct for automatic mapping** from JPA interface projections to UI model object and from UI model objects to JPA entities
    - MapStruct generates implementation classes at compile time (see `target/generated-sources/annotations`)
    - Example mapper: `UserMapper` converts between `UserDetailProjection` (JPA) and `UserDetail` (UI) and between `UserDetail` (UI) and `UserEntity` (JPA)
    - **Must NOT depend on UI libraries** (e.g., Vaadin)

- **Application Layer** (`bakery-app`)
    - Spring Boot application entry point
    - Contains `Application.java` in root package (`org.vaadin.bakery`) with `@SpringBootApplication`
    - Application is in root package so `org.vaadin.bakery.ui` is a subpackage and automatically scanned for Vaadin routes
    - Contains `BakeryBase` marker interface in root package
    - Contains `application.properties`
    - Contains frontend resources
    - **Runtime-only dependency on `bakery-jpaservice`** - enforces compile-time separation
    - Assembles all modules into executable application

## Vaadin Configuration

### Route Scanning

The `Application.java` class is in the root package (`org.vaadin.bakery`) so that Vaadin automatically scans the `org.vaadin.bakery.ui` subpackage for `@Route` annotated views:

```java
@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
public class Application implements AppShellConfigurator {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

By placing `Application.java` in the root package (`org.vaadin.bakery`), Vaadin automatically scans all subpackages including `org.vaadin.bakery.ui` for `@Route` annotated views. This avoids the need for `@EnableVaadin` which can interfere with theme loading.

### Allowed Packages

To efficiently scan for Vaadin components, including those that are core, VFC add-ons, and the application's, `application.properties` contains:
```
vaadin.allowed-packages=com.vaadin,org.vaadin
```
When adding new Vaadin add-ons, update this property to include their package prefixes.

## Persistence and Service Configuration

### Entity & Repository Registration

JPA entities and repositories are configured in `org.vaadin.bakery.jpaclient.config.JpaConfig`:
- `@EntityScan(basePackages = "org.vaadin.bakery.jpamodel")` - scans for JPA entities
- `@EnableJpaRepositories(basePackages = "org.vaadin.bakery.jpaclient")` - scans for Spring Data repositories

**Note**: In Spring Boot 4, `@EntityScan` is located in `org.springframework.boot.persistence.autoconfigure` package.

- Turn off Spring Data JPA's `open-session-in-view`

### MapStruct Integration

MapStruct is used to map from JPA interface projections to UI model objects and from UI model objects to JPA entities.

MapStruct is configured in the parent POM:
- Annotation processor configured in `maven-compiler-plugin`
- Mappers are Spring components (`componentModel = MappingConstants.ComponentModel.SPRING`)

Example partial mapper interface for `UserEntity`-related objects:
```java
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
    // from interface projectins to UI model
    UserDetail toDetail(UserDetailProjection detailProjection);
    List<UserSummary> toSummaryList(List<UserSummaryProjection> summaryProjection);

    // from UI model to entity (overwrites only common properties, leaves others untouched)
    UserEntity toEntity(UserDetail detail, @MappingTarget UserEntity entity);
}
```

### Fetch Queries (Selects)

Use interface projections and corresponding mappers to map to the UI model objects.

### Insert Operations

When saving a **new** UI model object:
1. Instantiate the corresponding entity.
2. Use the mapper to copy the UI model object values into it.
3. Explicitly persist the entity.

### Update Operations

When saving **edits** made to a UI model object for an existing entity:
1. First, fetch the entity (in order for it to be managed in the persistence context) via its unique identifier.
2. Have the service update (via MapStruct) corresponding values from the UI model object into the entity while leaving the entity's other values untouched.
3. The automatic committing of the transaction will persist the value changes.

## Database Configuration

- For development, `spring.jpa.hibernate.ddl-auto=create-drop` is used in `application.properties` (recreates schema on each startup)
- For production, use `spring.jpa.hibernate.ddl-auto=validate` to validate the schema without modifications

## Development Commands

### Running the Application
```bash
# Start in development mode from bakery-app module
cd bakery-app && ../mvnw spring-boot:run

# Or from root directory
./mvnw spring-boot:run -pl bakery-app -am
```

The application will start on port 8080 by default and automatically launch a browser window.

### Building
```bash
# Build all modules
./mvnw clean package

# Build and skip tests
./mvnw clean package -DskipTests

# Install to local Maven repository
./mvnw clean install
```

### Testing
```bash
# Run all tests across all modules
./mvnw test

# Run tests for a specific module
./mvnw test -pl bakery-jpaservice

# Run a specific test class
./mvnw test -pl bakery-jpaservice -Dtest=JpaUserServiceTest

# Run a specific test method
./mvnw test -pl bakery-jpaservice -Dtest=JpaUserServiceTest#users_are_stored_in_the_database_with_the_current_timestamp
```

## Security Configuration

### Application Security

Spring Security is to be configured using Vaadin 25-specific facilities and techniques as this is a Single Page Application (SPA)--most conventional Spring Security configuration advice will not apply.

### View Access Control

Use standard Jakarta Security annotations for view permissions.

## UI/UX

### Views

Application initially has only two views, "Login" and "About." "About" should be within the "main layout" and show the version of the application, dependency, app server, database, and browser.

All normal application views live within MainLayout

### Vaadin Error Views

Application displays exception views when there are unhandled errors:

| Num | Type                           | Purpose                                              |
|-----|--------------------------------|------------------------------------------------------|
| 400 | `HasErrorParameter`            | Interface for views that handle specific error types |
| 403 | `AccessDeniedExceptionHandler` | Handles Spring Security access denied                |
| 404 | `RouteNotFoundError`           | Base for 404 handling                                |
| 500 | `ErrorHandler`                 | Global handler for uncaught exceptions               |

#### Sensitive Information

Error views must not expose:
- Stack traces to users
- Internal system paths
- Database error details
- Security-sensitive information
- 403 errors must appear as 404 errors to the user
- 400 errors are only shown if there is no 403 error possible

Detailed error information is logged server-side only.

### Responsive Layout

Use Vaadin's responsive layout features.

Implement appropriate smooth transitions and animations.

### Composition

Views and custom composition components should extend `Composite<T>` rather than extending layout classes directly. This provides better encapsulation and cleaner APIs. Implement appropriate Vaadin "Has" interfaces to expose typical required functionality, such as for sizing and styling.

### Navigation

For routing of views in the main navigation menu, use the `@Menu` annotation.

### Theming and Styling

Use the "Lumo" theme with Lumo Utility Classes. Note: `LumoUtility` classes are only compatible with the Lumo theme, not the Aura theme.

Use component theme variants where available to achieve desired styling.

For simple styling adjustments to components, prefer using `addClassNames()` with `LumoUtility` class names over writing custom CSS. The `LumoUtility` class provides constants for common styling needs including padding, margins, colors, flexbox, and box-sizing—use these Java constants rather than adding CSS rules.

When elements with padding cause horizontal overflow (e.g., 100% width + padding exceeds container), apply `LumoUtility.BoxSizing.BORDER` to include padding within the element's declared width. This is the preferred approach over other overflow fixes.

### Dialogs

Custom dialogs should use **delegation rather than inheritance**. Instead of extending `Dialog`, create a class with a private `Dialog` field and expose only the methods needed. This prevents exposing Dialog's 50+ public methods to callers.

#### NonComponent Event System

For classes that don't extend `Component` but need event publishing (like delegating dialogs), use the event infrastructure in `org.vaadin.bakery.ui.event`:

- `NonComponent` - Interface marking classes that can fire events (analogous to `Component`)
- `NonComponentEvent<N extends NonComponent>` - Base event class with `getSource()` (analogous to `ComponentEvent`)
- `NonComponentEventSupport<N>` - Helper class for listener management

#### Example Pattern

```java
public class EditOrderDialog implements NonComponent {
    private final Dialog dialog = new Dialog();
    private final NonComponentEventSupport<EditOrderDialog> eventSupport = new NonComponentEventSupport<>();

    // Event class extends NonComponentEvent<SourceType>
    public static class SaveEvent extends NonComponentEvent<EditOrderDialog> {
        private final OrderDetail order;

        public SaveEvent(EditOrderDialog source, OrderDetail order) {
            super(source);
            this.order = order;
        }

        public OrderDetail getOrder() { return order; }
    }

    public static class CancelEvent extends NonComponentEvent<EditOrderDialog> {
        public CancelEvent(EditOrderDialog source) {
            super(source);
        }
    }

    public EditOrderDialog(...) {
        dialog.setCloseOnOutsideClick(false);
        dialog.add(createContent());
        dialog.getFooter().add(cancelButton, saveButton);
    }

    // Implement NonComponent interface
    @Override
    public <E extends NonComponentEvent<?>> Registration addListener(Class<E> eventType, Consumer<E> listener) {
        return eventSupport.addListener((Class) eventType, listener);
    }

    // Convenience listener methods
    public Registration addSaveListener(Consumer<SaveEvent> listener) {
        return eventSupport.addListener(SaveEvent.class, listener);
    }

    public Registration addCancelListener(Consumer<CancelEvent> listener) {
        return eventSupport.addListener(CancelEvent.class, listener);
    }

    // Fire events
    protected void fireEvent(NonComponentEvent<EditOrderDialog> event) {
        eventSupport.fireEvent(event);
    }

    // Only expose what's needed from Dialog
    public void open() { dialog.open(); }
    public void close() { dialog.close(); }
}
```

Benefits:
- Prevents exposing Dialog's 50+ public methods to callers
- Makes the public API explicit and intentional
- Provides `getSource()` on events (like `ComponentEvent`)
- Reusable event infrastructure across all delegating dialogs

The code that instantiates the dialog is responsible for attaching listeners and handling the events appropriately.

## Testing

### Unit Tests

Create unit tests for each non-UI public method.

Create UI unit tests for each UI feature using TestBench UI Unit Testing.

### Integration Tests

Create integration tests for each feature using Playwright.

### Patterns

Tests use:
- `@SpringBootTest` with custom `TestConfiguration`
- `@ComponentScan(basePackages = "org.vaadin.bakery")`
- `@EnableJpaRepositories(basePackages = "org.vaadin.bakery.jpaclient")`
- `@Transactional` to rollback changes
- AssertJ for assertions
- H2 in-memory database

## Naming Conventions

- **UI Model Objects**: No prefix or suffix, place in `uimodel.data` package. Names aligned with UI feature.
- **UI Model Enums**: No prefix or suffix, place in `uimodel.type` package
- **Service Interfaces**: Suffix with "Service" (e.g., "UserService"), place in `service` package
- **Entities**: Suffix with "Entity" and place in `jpamodel.entity` package
- **Entity Enums**: Suffix with "Code" and place in `jpamodel.code` package
- **Interface Projections**: Suffix with "Projection" and place in `jpamodel.projection` package
- **Service Implementations**: Prefix with the technology (e.g., "JpaUserService" for JPA implementation), place in `jpaservice` package

### Signal Field Naming

Suffix signal fields with their signal type for clarity:
- `ListSignal` fields: `orderItemsListSignal` (not `orderItems`)
- `ValueSignal` fields: `discountTypeSignal`, `editingItemSignal`
- Local computed signals: `subtotalValueSignal`, `discountValueSignal`, `totalValueSignal`

Use lowercase, descriptive comments for signal declarations:
```java
// signal for computed subtotal value
Signal<BigDecimal> subtotalValueSignal = Signal.computed(() -> ...);

// signal for computed discount value
Signal<BigDecimal> discountValueSignal = Signal.computed(() -> ...);
```

### UI Component Field Naming

Suffix UI component fields with their component type when the type isn't obvious from the name:
- `subtotalValueSpan` (not `subtotalValue`)
- `totalValueSpan` (not `totalValue`)
- `discountAmountField` (clarifies purpose and distinguishes from `discountAmountSignal`)

## Maven Conventions

- Specify versions for all dependencies in the parent POM's `dependencyManagement` section
- Specify versions for all plugins in the parent POM's `pluginManagement` section

## Java Conventions

- Use `var` instead of explicit types whenever possible

### Member Variable Initialization

Initialize member variables in constructors, not in their declarations. This keeps all initialization logic in one place.

```java
// Preferred: initialize in constructor
public class EditOrderDialog {
    private final Dialog dialog;
    private final ListSignal<OrderItemDetail> orderItemsListSignal;
    private final ValueSignal<DiscountType> discountTypeSignal;

    public EditOrderDialog(...) {
        dialog = new Dialog();
        orderItemsListSignal = new ListSignal<>();
        discountTypeSignal = new ValueSignal<>(DiscountType.PERCENT);
        // ...
    }
}

// Avoid: initialize at declaration
public class EditOrderDialog {
    private final Dialog dialog = new Dialog();  // Scattered initialization
    private final ListSignal<OrderItemDetail> orderItemsListSignal = new ListSignal<>();
    private final ValueSignal<DiscountType> discountTypeSignal = new ValueSignal<>(DiscountType.PERCENT);
}
```

### UI Initialization in Constructors

Keep all UI initialization in the constructor rather than splitting it across helper methods like `createHeader()`, `createContent()`, etc. This provides several benefits:

- **Visibility**: All initialization is visible in one place
- **Simplicity**: Fewer methods to navigate and understand
- **Local variables**: Components only used during construction can be local variables instead of fields
- **Consistency**: Avoids arbitrary decisions about what gets its own method (e.g., having `createHeader()` and `createContent()` but not `createFooter()`)

```java
// Preferred: all initialization in constructor
public EditOrderDialog(...) {
    // Component initializations
    var titleSpan = new Span("New Order");  // Local variable - only used here
    titleSpan.addClassNames(LumoUtility.FontSize.XLARGE);

    var header = new HorizontalLayout(titleSpan, locationComboBox);
    header.setWidthFull();

    // ... more initialization ...

    // Assemble layout
    dialog.getHeader().add(header);
    dialog.add(content);
    dialog.getFooter().add(cancelButton, saveButton);
}

// Avoid: splitting initialization across helper methods
public EditOrderDialog(...) {
    createHeader();      // Where does this add to?
    createContent();     // Inconsistent - why no createFooter()?
    createFooter();      // Or is footer created inline?
}
```

### Code Organization Within Methods

Group code by operation type, not by component. Within a constructor or method, organize in this order:
1. **Component initializations** - creating instances and configuring properties (min, max, width, items, data sources, etc.)
2. **Signal definitions** - creating and configuring signals
3. **Signal bindings** - connecting signals to components (reactive UI)
4. **Binder bindings** - connecting form fields to bean model (with validation)
5. **Value settings** - setting initial/default values on fields or bean on Binder

Use blank lines between each component in the initialization section for readability.

```java
// Preferred: grouped by operation type
private Div createTotalsSection() {
    // 1. Component initializations (creation + configuration)
    var subtotalLabel = new Span("Subtotal:");

    var subtotalValueSpan = new Span();

    var discountTypeGroup = new RadioButtonGroup<DiscountType>();
    discountTypeGroup.setItems(DiscountType.values());

    var discountAmountField = new NumberField();
    discountAmountField.setMin(0);
    discountAmountField.setWidth("80px");

    // 2. Signal definitions
    Signal<BigDecimal> subtotalValueSignal = Signal.computed(() -> ...);
    Signal<BigDecimal> discountValueSignal = Signal.computed(() -> ...);

    // 3. Signal bindings
    subtotalValueSpan.bindText(subtotalValueSignal.map(...));
    discountValueSpan.bindText(discountValueSignal.map(...));

    // 4. Binder bindings (if applicable)
    // binder.forField(discountAmountField).bind(...);

    // 5. Value settings (field values or bean on Binder)
    discountTypeGroup.setValue(DiscountType.PERCENT);
    // binder.setBean(order);
    // ...
}

// Avoid: grouped by component
private Div createTotalsSection() {
    var subtotalLabel = new Span("Subtotal:");
    subtotalLabel.addClassNames(...);  // Mixed with creation

    var discountTypeGroup = new RadioButtonGroup<DiscountType>();
    discountTypeGroup.setItems(DiscountType.values());  // Immediately after creation
    discountTypeGroup.setValue(DiscountType.PERCENT);

    Signal<BigDecimal> subtotalValueSignal = Signal.computed(() -> ...);  // Signal in middle
    // ...
}
```

### Local Variable Declaration

Declare local variables close to their first use, not at the top of methods:

```java
// Preferred: declare near first use
private VerticalLayout createContent() {
    var form = new FormLayout();
    // ... configure form ...

    var content = new VerticalLayout();  // Declared just before use
    content.add(form);
    return content;
}

// Avoid: declaring at top when used later
private VerticalLayout createContent() {
    var content = new VerticalLayout();  // Too early
    var form = new FormLayout();
    // ... configure form ...
    content.add(form);
    return content;
}
```

### Nested Types Placement

Place nested types (inner classes, enums) at the **end** of the class, after all methods:

```java
public class EditOrderDialog {
    // Fields
    // Constructor
    // Public API methods
    // Private methods

    // ========== Event Classes ==========
    public static class SaveEvent extends NonComponentEvent<EditOrderDialog> { ... }
    public static class CancelEvent extends NonComponentEvent<EditOrderDialog> { ... }

    // Private enums last
    private enum DiscountType { ... }
}
```

Use `private` visibility for enums that are only used internally.

### Stream Operations

Avoid unnecessary operations in streams:
- Don't filter for nulls if the data source guarantees non-null values
- Prefer simpler expressions when the result is equivalent

```java
// Preferred: no unnecessary null filter
.map(OrderItemDetail::getLineTotal)
.reduce(BigDecimal.ZERO, BigDecimal::add);

// Avoid: filtering nulls when not needed
.map(OrderItemDetail::getLineTotal)
.filter(Objects::nonNull)  // Remove if nulls aren't possible
.reduce(BigDecimal.ZERO, BigDecimal::add);
```

## Date/Time Handling

### Storage vs Display Pattern

Use `Instant` for timestamp storage and `LocalDateTime` for display:

- **Storage (JPA Entities)**: Use `java.time.Instant` for all timestamps (`createdAt`, `updatedAt`, etc.)
  - Stored as UTC in the database
  - Timezone-safe regardless of server location
  - Base class `AbstractAuditableEntity` provides standard audit fields

- **Display (UI Models)**: Use `java.time.LocalDateTime` for timestamps shown to users
  - Converted from `Instant` in the service/mapper layer
  - Base class `AbstractAuditableModel` provides standard audit fields

- **Conversion**: Use `InstantMapper` (MapStruct) for automatic conversion
  - Converts using system default timezone (or location-specific timezone when needed)
  - Include `InstantMapper.class` in mapper's `uses` clause

### Location Timezone

Each `LocationEntity` stores an IANA timezone ID (e.g., `"America/New_York"`) for display conversion:
- Orders can display times in their pickup location's timezone
- Future enhancement: Use location timezone instead of system default for display

### Browser Timezone Detection

The browser's timezone must be obtained from the client and stored in a session-scoped service:

1. **Service Interface** (`bakery-service`): `UserTimezoneService`
   - `setBrowserTimezone(ZoneId)` - called by UI after detecting browser TZ
   - `getBrowserTimezone()` - returns browser TZ or system default as fallback
   - `isBrowserTimezoneSet()` - checks if TZ has been set

2. **Session-Scoped Implementation** (`bakery-jpaservice`): `SessionUserTimezoneService`
   - Uses `@SessionScope` to persist timezone for user's session
   - Requires `spring-web` dependency for `@SessionScope`

3. **UI Detection** (`MainLayout.onAttach`):
   ```java
   @Override
   protected void onAttach(AttachEvent attachEvent) {
       super.onAttach(attachEvent);
       if (!userTimezoneService.isBrowserTimezoneSet()) {
           attachEvent.getUI().getPage().retrieveExtendedClientDetails(details -> {
               var timezoneId = details.getTimeZoneId();
               if (timezoneId != null && !timezoneId.isEmpty()) {
                   userTimezoneService.setBrowserTimezone(ZoneId.of(timezoneId));
               }
           });
       }
   }
   ```

4. **InstantMapper**: Abstract class with injected `UserTimezoneService`
   - `toBrowserTime(Instant)` - converts UTC to browser-local time
   - `toServerTime(LocalDateTime)` - converts browser-local to UTC

### Example Usage

```java
// Entity with Instant timestamps
@Entity
public class OrderEntity extends AbstractAuditableEntity {
    // createdAt and updatedAt are Instant (inherited)
}

// UI Model with LocalDateTime timestamps
public class OrderDetail extends AbstractAuditableModel {
    // createdAt and updatedAt are LocalDateTime (inherited)
}

// Mapper with InstantMapper for automatic conversion
@Mapper(componentModel = SPRING, uses = {InstantMapper.class})
public abstract class OrderMapper {
    OrderDetail toDetail(OrderEntity entity);
}
```

## Development Guidelines

Do things the Vaadin 25 way.

When adding new features across modules:

1. **Create UI model** in `bakery-uimodel`
    - **Name without suffix**: `UserSummary`
    - Plain POJO without JPA annotations
    - Specific to the feature using it (i.e., just key/name for combo boxes, summary for dashboards, abbreviated for listings, detail for modifying, all for admin)

2. **Create JPA entity** (if it does not already exist) in `bakery-jpamodel`
    - **Base name with `Entity` suffix**: `UserEntity`
    - Add `@Entity`, `@Table`, column mappings

3. **Create JPA Interface Projection** in `bakery-jpamodel`
    - **Name with `Projection` suffix**: `UserSummaryProjection`
    - Add projection as implemented by Entity, `public class UserEntity implements UserSummaryProjection`

4. **Create repository** (if it does not already exist) in `bakery-jpaclient`
    - Extend `JpaRepository<EntityName, ID>`
    - Example: `JpaRepository<UserEntity, Long>`

5. **Create MapStruct mapper** in `bakery-jpaservice/mapper`
    - Interface with `@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)`
    - Declare conversion methods between JPA entity/projections and UI model
    - Examples:
        - `UserSummary toModel(UserSummaryProjection projection)`
        - `UserEntity toEntity(UserSummary summary)`

6. **Define service interface** in `bakery-service`
    - Methods work with UI models, not entities
    - Example: `List<UserSummary> fetchSummaryByUserType(UserType userType)`
    - **Naming convention**: Sufffix base model with `Service` (e.g., `UserService`)

7. **Implement service** in `bakery-jpaservice`
    - Inject repository and mapper
    - Use `@Service` and `@Transactional`
    - **Naming convention**: Prefix with implementation type (e.g., `JpaUserService`)
    - **Do NOT add UI library dependencies** (e.g., Vaadin)

8. **Create UI view** in `bakery-ui`
    - Each view and its related classes should be in its own sub-package under `ui.view`
    - Hypothetical example: `UserListView` would be in `org.vaadin.bakery.ui.view.userlist`
    - Shared components should be in `org.vaadin.bakery.ui.component` (e.g., `FilterBar`)
    - Constructor-inject service interface
    - Work with UI models only (no suffix)
    - Add `@Route`, `@PageTitle`, `@Menu` annotations

9. **Write tests** in `bakery-jpaservice/src/test/java`
    - Use `@SpringBootTest` with `TestConfiguration`
    - Test service layer with actual repository and MapStruct mapping

## Important Notes

### frontend folder

The only module that needs and uses a Vaadin Flow `src/main/frontend` folder is the `bakery-app` folder.

### MapStruct Generated Code

- MapStruct implementations are generated at compile time
- Find generated classes in `target/generated-sources/annotations`
- If mapper changes don't take effect, run `./mvnw clean compile`

### Module Dependencies

- Modules must be built in dependency order (handled automatically by Maven reactor)
- When working on a single module, use `-am` (also-make) flag to build dependencies:
  ```bash
  ./mvnw test -pl bakery-jpaservice -am
  ```

### Spring Boot 4 Notes

- `@EntityScan` is in `org.springframework.boot.persistence.autoconfigure` package.

### Vaadin Development Mode

- The `vaadin-dev` dependency is required in `bakery-app` for development mode support (hot reload, debug features)
- This dependency should be marked as `<optional>true</optional>` so it's not included in production builds
- Without this dependency, you'll get a runtime error: `vaadin-dev-server artifact is not found`

### Managed Sources

- `.gitignore` should exclude node modules and target directories, files specific to developer enviroment, such as IDE, JRebel, MCPs, etc., and OS hidden files.

## Best Practices

- Make a reasonable effort to follow best practices
- Keep the code DRY (Don't Repeat Yourself)
- Use type-safe alternatives to string-based specifiers where applicable
- Service implementations must not depend on UI libraries to maintain layer separation
