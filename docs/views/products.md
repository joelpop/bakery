# Products View

The Products view provides CRUD functionality for managing the bakery's product catalog. This view is accessible under the Admin menu.

> **Note**: No direct screenshots of this view are available. This documentation is inferred from product data visible in order screens and dashboard charts.

## Inferred Product Data

From order screenshots, the following products are visible:

| Product Name | Size/Servings | Price | Notes |
|--------------|---------------|-------|-------|
| Princess Cake | 12 ppl | $39.90 | Most frequently ordered |
| Strawberry Cake | 12 ppl | $29.90 | |
| Salami Pastry | (individual) | $7.90 | Sold in larger quantities (e.g., 32) |
| Blueberry Cheese Cake | (unknown) | (unknown) | |
| Vanilla Bun | (unknown) | (unknown) | |
| Bacon Tart | (unknown) | (unknown) | |
| Bacon Cheese Cake | (unknown) | (unknown) | |
| Bacon Cracker | (unknown) | (unknown) | |

## Expected Layout

Based on the Users view pattern (similar CRUD interface), the Products view likely includes:

### Product List
- Searchable data grid
- "+ New product" button
- Sortable columns

### Expected Grid Columns

| Column | Description |
|--------|-------------|
| (Image) | Product photo (optional) |
| Name | Product name |
| Size | Serving size (e.g., "12 ppl", "individual") |
| Price | Unit price |
| Available | Availability status |

### Technical Note: Product Entity
```
Product {
  id: Long
  name: String
  description: String (optional)
  size: String (e.g., "12 ppl", "individual")
  price: BigDecimal
  available: Boolean
  photo: Blob (optional)
  photoContentType: String (optional)
}
```

---

## New/Edit Product Dialog

Expected side panel dialog with:

### Form Fields

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| Photo | Image Upload | No | Product image |
| Name | Text Input | Yes | Product display name |
| Description | Text Area | No | Product description |
| Size | Text Input | Yes | Serving size indicator |
| Price | Currency Input | Yes | Unit price |
| Available | Toggle/Checkbox | Yes | Whether product can be ordered |

### Actions
- **Save** - Save product and close
- **Cancel** - Discard changes
- **Delete** - Remove product (with order dependency check)

### Validation Rules
- Name must be unique
- Price must be positive
- Products with pending orders cannot be deleted (soft delete or warning)

---

## Access Control

- Only users with **Admin** role can access Products management
- Products appears under the "Admin" overflow menu in navigation
- Non-admin users can view products in order creation but cannot edit them

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
