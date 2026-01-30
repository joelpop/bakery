# Application Overview

## Application Name

**Café Sunshine** - Bakery Order Management System

## Business Context

The Bakery owns two locations:
- **Café** - A centrally located retail storefront in the city center
- **Bakery** - The production facility located outside the city center

### Products

In addition to daily producing a standard set of baked goods (available fresh from the café or straight from the bakery), the Bakery is the most well-known producer of **custom cakes for special occasions**—offering customization to a variety of cakes in different sizes.

### Order Fulfillment

- Customers order cakes from the café (in person or by phone) and pick them up at the agreed date
- Orders are made **just-in-time**—typically the morning of the day they are due
- All orders are customer pickup; there is no delivery or shipping

### The Problem Being Solved

The previous process relied on handwritten notes passed along as photos in WhatsApp, leading to:
- Errors from misread or misunderstood orders
- Stress during busy periods
- Limited visibility into order status
- No way to verify orders were correctly received
- Difficulty knowing product/ingredient availability

This application provides a structured system to ensure orders are correctly received, made to specification, and available on the correct day.

---

## Personas

### Peter (Customer)

**Role**: Customer placing orders for special occasions

Peter contacts a Barista (or occasionally a Baker) to place orders. He has used the service for both personal occasions (family events) and work events (personnel occasions).

**Behaviors**:
- Often shows up in person to explain the situation and get suggestions
- Sometimes phones in orders for recurring work events
- Primary concern: getting something of suitable size on the right day

**Note**: Customers do not directly use the system. Customer data is used for autofill and contact purposes only.

---

### Malin (Barista)

**Role**: Front-of-house staff at the café

Malin works at the storefront serving customers, which includes taking orders from customers like Peter—in person or over the phone (sometimes with a queue of customers waiting).

**Pain Points with Previous System**:
- Error-prone paper-based process
- Significant stress during busy periods
- No way to verify order was correctly received until customer arrives
- Limited knowledge of bakery capabilities and ingredient availability
- One-way communication with bakery

**Primary Tasks**:
- Accept orders (capture customer info, products, due date, location)
- Verify orders with customers
- Handle customer pickup (find order, mark as picked up, mark as paid)

---

### Heidi (Baker)

**Role**: Baker at the production facility

Heidi receives orders mainly from Baristas but also directly from customers. Orders are reviewed and scheduled by Bakers for just-in-time production.

**Pain Points with Previous System**:
- Misunderstanding or misreading handwritten orders
- Ingredients running low (especially for uncommon products)
- Some products require extra preparation time and cannot be made on short notice
- Rules are "obvious" to bakery staff but hard to document for baristas

**Primary Tasks**:
- Review and verify incoming orders
- View today's and tomorrow's orders
- Mark orders as In Progress → Baked → Packaged
- Flag problems that require customer contact

---

## User Roles (System Access)

| Role | Description | System Access |
|------|-------------|---------------|
| **Admin** | Store managers and administrators | Full access including Users, Products, and Locations management |
| **Baker** | Kitchen staff responsible for preparing orders | Storefront and Dashboard |
| **Barista** | Front-of-house staff handling customer interactions | Storefront and Dashboard |

**Note**: Customers (like Peter) do not have system access. They interact via phone or in person with staff.

---

## System Contexts

### Storefront (Café)

Primary functions:
- Accept orders (customer info, items, due date, location)
- Verify orders with customers
- Deliver (hand over) orders being picked up
- View order status

### Bakery

Primary functions:
- Review and verify incoming orders for following days
- View today's and tomorrow's orders
- Progress orders through manufacturing: In Progress → Baked → Packaged
- Flag problems requiring customer contact
- Secondary: accept orders directly

### Back-Office

Primary functions:
- Status overview and decision-making
- System administration (users, products, locations)
- View statistics
- Secondary: accept phone orders

---

## Key Features

### Order Management (Storefront View)

- View orders grouped by pickup date (Today, Tomorrow, This Week, Upcoming)
- Filter orders by status, customer, payment status
- Create new orders with multi-product support
- Track order status through fulfillment
- Handle customer pickup and payment

### Business Analytics (Dashboard)

- Real-time KPIs (orders remaining, new orders, product availability)
- Pickup trend charts (daily and monthly)
- Year-over-year sales comparison
- Product popularity breakdown
- Alerts and notifications

### User Management

- CRUD operations for staff accounts
- Role-based access control (Admin, Baker, Barista)
- Profile photo support

### Product Catalog

- Manage bakery product offerings
- Set pricing and portion sizes
- Track product availability

### Location Management

- Manage pickup locations (Café, Bakery)
- Configure location details and availability

---

## What This System Does NOT Do

- **No online store** - Customers interact through staff, not directly with the system
- **No delivery or shipping** - All orders are customer pickup
- **No inventory management** - Focus is on order fulfillment, not manufacturing
- **No quick user switching** - Users are assumed to use personal devices or shared accounts

---

## Responsive Design

The application is fully responsive with optimized layouts for:

| Device | Navigation | Layout |
|--------|------------|--------|
| **Desktop** | Top horizontal navigation bar | Full-featured multi-column layouts |
| **Tablet** | Top navigation (possibly condensed) | Adapted layouts |
| **Phone** | Bottom tab bar with overflow menu | Single-column, touch-optimized |
