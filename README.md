# Bakery

A Vaadin 25 + Spring Boot 4 multi-module application demonstrating layered architecture with complete separation between persistence and presentation layers.

## Tech Stack

- Java 25
- Vaadin 25 (Aura theme)
- Spring Boot 4
- Spring Data JPA
- MapStruct
- H2 Database (development)

## Prerequisites

- JDK 25+
- Maven 3.9+ (or use included `mvnw` wrapper)
- Node.js (for Vaadin frontend build)

## Building

```bash
./mvnw clean package
```

## Running

```bash
./mvnw spring-boot:run -pl bakery-app -am
```

The application starts at http://localhost:8080

### Default Users

| Username | Password | Role  |
|----------|----------|-------|
| user     | user     | USER  |
| admin    | admin    | ADMIN |

## Project Structure

```
bakery/
├── bakery-common      # Shared utilities
├── bakery-jpamodel    # JPA entities, projections, enums
├── bakery-jpaclient   # Spring Data repositories
├── bakery-uimodel     # UI model POJOs
├── bakery-service     # Service interfaces
├── bakery-jpaservice  # Service implementations with MapStruct
├── bakery-ui          # Vaadin views and components
└── bakery-app         # Spring Boot application entry point
```

## Architecture

The project enforces compile-time layer separation:

- **UI layer** (`bakery-ui`) depends only on service interfaces and UI models
- **Service implementation** (`bakery-jpaservice`) is a runtime-only dependency
- **MapStruct** handles mapping between JPA projections/entities and UI models

See [CLAUDE.md](CLAUDE.md) for detailed architecture documentation.

## Testing

```bash
# All tests
./mvnw test

# Specific module
./mvnw test -pl bakery-jpaservice -am
```

## License

Proprietary
