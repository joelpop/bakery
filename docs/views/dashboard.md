# Dashboard View

The Dashboard provides a comprehensive overview of business performance with KPIs, charts, alerts, and upcoming order information.

**Route**: `/dashboard`

**Access**: All authenticated users (Admin, Baker, Barista)

## Screenshot
- Desktop: `originals/images/dashboard view/Desktop, dashboard.png`

## Layout

The dashboard is organized into several sections:
1. KPI Cards (top row)
2. Alerts / Bulletin Board
3. Pickup Charts (second row)
4. Sales Trend Chart (third row)
5. Products Breakdown and Upcoming Orders (bottom row)

---

## KPI Cards

Key performance indicators displayed as cards across the top:

| KPI | Description | Visual |
|-----|-------------|--------|
| **Remaining Today** | Orders still to be fulfilled today | Number with "Next pickup" time |
| **Not Available** | Products currently unavailable | Number with "Orders due today with N/A products" note |
| **New** | New orders awaiting verification | Number with "Last X ago" timestamp |
| **Tomorrow** | Orders scheduled for tomorrow | Number with "First pickup" time |

### Additional KPIs (Back-Office)

For Admin users, additional metrics may be visible:

| KPI | Description |
|-----|-------------|
| **Warnings** | Orders with NOT_OK status requiring attention |
| **Failures** | Cancelled orders this period |
| **Month Total** | Orders this month with dual comparison deltas |
| **Year Total** | Orders this year with dual comparison deltas |

### KPI Delta Calculations

Each month/year KPI shows two comparison deltas:

| Comparison | Description | Example |
|------------|-------------|---------|
| vs. Previous Period | Current month vs last month, current year vs last year | "↑12% vs last month" |
| vs. Same Period Last Year | Current month vs same month last year | "↑8% vs Jun 2024" |

This dual comparison helps distinguish between:
- General growth trends (vs. previous period)
- Seasonal patterns (vs. same period last year)

---

## Alerts / Bulletin Board

A notification area for important messages and alerts:

| Alert Type | Description | Example |
|------------|-------------|---------|
| **Ingredient Alert** | Low stock or unavailable items | "We're out of pink sugarcoating!" |
| **Problem Orders** | Orders marked as NOT_OK | "Order #234 needs attention" |
| **Staff Messages** | Communications between staff | General announcements |

Alerts help ensure issues are spotted quickly by everyone.

---

## Pickup Charts

### Pickups in [Current Month]
- **Type**: Bar chart
- **X-axis**: Days of the month (1-31)
- **Y-axis**: Number of pickups
- **Purpose**: Shows daily pickup volume for capacity planning

### Pickups in [Current Year]
- **Type**: Bar chart
- **X-axis**: Months (Jan-Dec)
- **Y-axis**: Number of pickups
- **Purpose**: Shows monthly pickup trends

---

## Sales Trend Chart

### Sales Last Years
- **Type**: Multi-line chart
- **X-axis**: Time period (months or quarters)
- **Y-axis**: Sales value
- **Lines**: Multiple years for comparison (e.g., 2023, 2024, 2025)
- **Purpose**: Year-over-year sales comparison and trend analysis

---

## Products Delivered Breakdown

### Products Delivered in [Current Month]
- **Type**: Donut/Pie chart
- **Segments**: Product categories with quantities
- **Center**: Total count for the period

Example segments:
- Princess Cake
- Vanilla Bun
- Bacon Tart
- Bacon Cheese Cake
- Blueberry Strawberry Cake
- Custom Cakes

---

## Upcoming Orders

A summary list of upcoming orders displayed alongside the products chart:

| Column | Description |
|--------|-------------|
| Status | Order status badge (Verified, In Progress, Baked, etc.) |
| Paid | Payment indicator |
| Day | Day of week and date |
| Time | Pickup time |
| Location | Café or Bakery |
| Customer | Customer name |
| Items | Product summary with quantities |

This is a condensed view of the order list, showing only the most imminent orders for quick reference.

---

## Soon Due Orders

Orders that are due soon (today or tomorrow) and require attention:

| Status | Urgency |
|--------|---------|
| New (today) | High - needs verification urgently |
| Verified (today) | Should be in progress |
| In Progress (past due time) | Running late |

---

## Responsive Behavior

The dashboard components reflow on smaller screens:
- KPI cards may stack vertically (2x2 grid on tablet, single column on phone)
- Charts resize to fit available width
- Upcoming orders list may move below charts on mobile
- Alerts remain prominently visible at top

---

## Related Documentation

- [Storefront View](storefront.md) - Full order list and management
- [DashboardService](../services/interfaces.md#dashboardservice) - Dashboard data service
