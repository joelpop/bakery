# Services Overview

This document describes the service layer architecture for the Bakery application.

## Architecture

The service layer follows a clean separation between interfaces and implementations:

```
┌─────────────────────────────────────────┐
│           UI Layer (Vaadin)             │
│      Works with UI Models only          │
└─────────────────┬───────────────────────┘
                  │
                  │ Injects Service Interfaces
                  ▼
┌─────────────────────────────────────────┐
│       Service Interfaces (bakery-service)│
│       Define operations with UI models   │
└─────────────────┬───────────────────────┘
                  │
                  │ Implemented by
                  ▼
┌─────────────────────────────────────────┐
│   Service Implementations (bakery-jpaservice)│
│   MapStruct mapping, transactions        │
└─────────────────┬───────────────────────┘
                  │
                  │ Uses
                  ▼
┌─────────────────────────────────────────┐
│        Repositories (bakery-jpaclient)   │
└─────────────────────────────────────────┘
```

## Module Structure

| Module | Role |
|--------|------|
| `bakery-service` | Service interfaces using UI models |
| `bakery-jpaservice` | JPA-based implementations |
| `bakery-uimodel` | UI model POJOs |

## Key Principles

### 1. Interface Segregation
- Service interfaces are defined separately from implementations
- UI layer depends only on interfaces
- Implementation details are hidden

### 2. UI Model Focus
- Service methods accept and return UI models (POJOs)
- No JPA entities exposed to UI layer
- Clean API boundaries

### 3. MapStruct Mapping
- Automatic entity ↔ UI model conversion
- Compile-time generated mappers
- Type-safe and efficient

### 4. Transaction Management
- `@Transactional` on service implementations
- Read-only transactions for queries
- Proper rollback on exceptions

## Service Interfaces

| Service | Purpose |
|---------|---------|
| `UserService` | User CRUD and authentication |
| `CustomerService` | Customer management |
| `ProductService` | Product catalog |
| `OrderService` | Order management |
| `NotificationService` | User notifications |
| `DashboardService` | Analytics and KPIs |

## Related Documentation

- [Service Interfaces](interfaces.md) - Interface definitions
- [Service Implementations](implementations.md) - JPA implementations
