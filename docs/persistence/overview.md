# Persistence Overview

This document describes the persistence architecture for the Bakery application.

## Architecture

The application follows a layered architecture with clear separation between persistence and presentation layers:

```
┌─────────────────────────────────────────┐
│           Presentation Layer            │
│         (Vaadin UI, Controllers)        │
└─────────────────┬───────────────────────┘
                  │ UI Models (POJOs)
┌─────────────────▼───────────────────────┐
│            Service Layer                │
│    (Business Logic, Transactions)       │
└─────────────────┬───────────────────────┘
                  │ Entities
┌─────────────────▼───────────────────────┐
│          Persistence Layer              │
│   (JPA Entities, Spring Data Repos)     │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│             Database                    │
│         (H2 / PostgreSQL)               │
└─────────────────────────────────────────┘
```

## Technology Stack

| Component | Technology |
|-----------|------------|
| ORM | Jakarta Persistence (JPA) via Hibernate |
| Repository | Spring Data JPA |
| Database (Dev) | H2 (in-memory) |
| Database (Prod) | PostgreSQL (recommended) |
| Migrations | Flyway or Liquibase (recommended) |

## Module Structure

Following the project's multi-module architecture:

| Module | Persistence Role |
|--------|------------------|
| `bakery-jpamodel` | JPA entities, enums, projections |
| `bakery-jpaclient` | Spring Data repositories, JPA config |
| `bakery-jpaservice` | Service implementations with MapStruct |

## Key Principles

### 1. Entity Isolation
JPA entities are confined to the persistence layer. The UI layer never directly accesses entities—all data flows through UI model POJOs.

### 2. MapStruct Mapping
Entity-to-model conversion is handled by MapStruct mappers, ensuring:
- Compile-time type safety
- Clean separation of concerns
- Efficient mapping without reflection overhead

### 3. Repository Pattern
Spring Data JPA repositories provide:
- Standard CRUD operations
- Custom query methods via method naming conventions
- Pagination and sorting support
- Specification-based dynamic queries

### 4. Transaction Management
- Transactions are managed at the service layer
- `@Transactional` annotations on service methods
- Read-only transactions for query operations

## Database Configuration

### Development
```properties
spring.datasource.url=jdbc:h2:mem:bakerydb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true
```

### Production
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bakery
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.open-in-view=false
```

## Related Documentation

- [JPA Model Overview](model/overview.md) - Entity overview, relationships, and index to model documentation
  - **Entities**
    - [UserEntity](model/entities/user.md)
    - [CustomerEntity](model/entities/customer.md)
    - [ProductEntity](model/entities/product.md)
    - [LocationEntity](model/entities/location.md)
    - [OrderEntity](model/entities/order.md)
    - [OrderItemEntity](model/entities/order-item.md)
    - [NotificationEntity](model/entities/notification.md)
  - **Codes**
    - [UserRoleCode](model/codes/user-role.md)
    - [OrderStatusCode](model/codes/order-status.md)
- [Repositories](repositories.md) - Spring Data repository interfaces
