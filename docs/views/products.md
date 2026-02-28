# Products View

The Products view provides product catalog management with role-based access: Admin users have full CRUD functionality, while Baker users have read-only access.

**Route**: `/products`

**Access**: Admin (full CRUD), Baker (read-only)

## Product List

### Layout
A searchable data grid displaying all products.

### Toolbar

| Element | Description |
|---------|-------------|
| Search | Filter products by name |
| + New product | Button to create a new product (Admin only) |

### Grid Columns

| Column | Description |
|--------|-------------|
| (Image) | Product photo thumbnail |
| Name | Product name |
| Size | Serving size (e.g., "12 ppl", "Individual") |
| Price | Unit price |
| Available | Availability status toggle |

### Cross-Session Updates
The product grid auto-refreshes via shared signals when products are modified in another session. Changed rows receive a temporary gold highlight animation.

### Product Entity
```
Product {
  id: Long
  name: String
  description: String (optional)
  size: String (e.g., "12 ppl", "Individual")
  price: BigDecimal
  available: boolean
  batchable: boolean (default: true)
  photo: byte[] (optional)
  photoContentType: String (optional)
}
```

---

## Product Dialog

A dialog for creating and editing products (Admin only). Uses delegation pattern (wraps Dialog rather than extending it). Includes stale data detection with a warning banner when external changes are detected.

### Form Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| Photo | Image Upload | No | Product image |
| Name | Text Input | Yes | Product display name |
| Description | Text Area | No | Product description |
| Size | Text Input | Yes | Serving size indicator |
| Price | Currency Input | Yes | Unit price |
| Available | Toggle/Checkbox | Yes | Whether product can be ordered |
| Batchable | Toggle/Checkbox | Yes | Whether items can be grouped in the Bakery view (default: true) |

### Actions
- **Save** - Save product and close
- **Cancel** - Discard changes
- **Delete** - Remove product (with confirmation)

### Validation Rules
- Name must be unique
- Price must be positive

---

## Access Control

- **Admin**: Full CRUD access (create, edit, delete products)
- **Baker**: Read-only access (can view but not modify the product catalog)
- Products appears in the main navigation for both Admin and Baker roles
- Barista users cannot access this view
- All authenticated users can see products in the order creation dialog

---

## Product Availability

The "Not Available" KPI on the Dashboard suggests products can be marked unavailable:
- Temporarily out of stock
- Seasonal items
- Discontinued products

When a product is unavailable:
- It should not appear in the order creation product dropdown
- Existing orders with that product may show a warning
- Dashboard tracks unavailable count

---

## Responsive Behavior

### Phone Layout
- Accessed via Admin overflow menu
- Grid with reduced columns
- Full-screen edit dialog
