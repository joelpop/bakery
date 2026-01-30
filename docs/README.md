# Bakery Application Documentation

This documentation describes the **Café Sunshine** bakery order management application.

## Overview

Café Sunshine is a comprehensive bakery order management system that enables bakery staff to manage customer orders, track deliveries, maintain product catalogs, and analyze business performance.

## Documentation Index

### Application Overview
- [Application Overview](overview/application-overview.md) - Purpose, user roles, and key features

### Views
- [Login](views/login.md) - Authentication (password and passkey)
- [Storefront](views/storefront.md) - Order management and creation
- [Dashboard](views/dashboard.md) - Business analytics and KPIs
- [Users](views/users.md) - User management (CRUD)
- [Products](views/products.md) - Product catalog management
- [Locations](views/locations.md) - Pickup location management (CRUD)
- [Preferences](views/preferences.md) - User settings and security
- [Exception Views](views/exceptions.md) - Error pages (404, 403, 500)

### Features
- [Navigation](features/navigation.md) - Desktop and mobile navigation patterns
- [User Menu](features/user-menu.md) - User profile, preferences, and notifications
- [Orders](features/orders.md) - Order workflow, statuses, and data model

### Persistence
- [Overview](persistence/overview.md) - Persistence architecture and technology stack
- [JPA Model](persistence/model/overview.md) - Entities, codes, and projections
- [Repositories](persistence/repositories.md) - Spring Data JPA repository interfaces

### Services
- [Overview](services/overview.md) - Service layer architecture
- [Interfaces](services/interfaces.md) - Service interface definitions
- [Implementations](services/implementations.md) - JPA service implementations and MapStruct mappers

### Security
- [Overview](security/overview.md) - Security architecture and role-based access
- [Configuration](security/configuration.md) - Spring Security setup
- [Authentication](security/authentication.md) - Login, logout, session management
- [Authorization](security/authorization.md) - Role-based access control

### Screenshots
Screenshots of the application are located in the [originals/images](originals/images/) folder, organized by view.

## Quick Reference

| View | Purpose | Access |
|------|---------|--------|
| Login | User authentication | Anonymous |
| Storefront | Manage customer orders | All users |
| Dashboard | View business analytics | All users |
| Users | Manage user accounts | Admin only |
| Products | Manage product catalog | Admin (edit), Baker (read-only) |
| Locations | Manage pickup locations | Admin only |
| Preferences | User settings and security | All users |
