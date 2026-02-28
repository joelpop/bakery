# Dashboard View

The Dashboard provides a comprehensive overview of business performance with KPIs, charts, alerts, and upcoming order information.

**Route**: `/dashboard`

**Access**: All authenticated users (Admin, Baker, Barista)

## Screenshot
- Desktop: `legacy/images/dashboard view/Desktop, dashboard.png`

## Layout

The dashboard is organized into the following sections:
1. KPI Cards (top row) — **implemented**
2. Alerts / Bulletin Board — *deferred*
3. Pickup Charts (second row) — *placeholder added, charts deferred*
4. Sales Trend Chart (third row) — *deferred*
5. Products Breakdown (bottom row) — *placeholder added, chart deferred*
6. Upcoming Orders (bottom row) — **implemented**

### Cross-Session Updates
The dashboard auto-refreshes via shared signals (`DataChangeSignals.orderVersion()` and `DataChangeSignals.productVersion()`) when data changes in another session.

---

## KPI Cards

Six key performance indicators displayed as cards across the top (all visible to all authenticated users):

| KPI | Description | Visual |
|-----|-------------|--------|
| **Remaining Today** | Orders still to be fulfilled today | Count + "Next pickup" time |
| **Not Available** | Products currently unavailable | Count |
| **New** | New orders awaiting verification (IN_REVIEW) | Count + "Last X ago" timestamp |
| **Tomorrow** | Orders scheduled for tomorrow | Count + "First pickup" time |
| **Month Total** | Orders this month | Count + dual delta (vs prev month AND vs same month last year) |
| **Year Total** | Orders this year | Count + dual delta (vs prev year AND vs same period last year) |

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

## Alerts / Bulletin Board *(Deferred)*

> **Note**: The alerts/bulletin board section is deferred to a future enhancement.

Planned alert types:

| Alert Type | Description | Example |
|------------|-------------|---------|
| **Ingredient Alert** | Low stock or unavailable items | "We're out of pink sugarcoating!" |
| **Problem Orders** | Orders in IN_REVIEW with rejected items | "Order #234 needs attention" |
| **Staff Messages** | Communications between staff | General announcements |

---

## Pickup Charts *(Placeholders — Charts Deferred)*

> **Note**: Chart placeholders are displayed in the UI. Actual chart implementations are deferred.

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

## Sales Trend Chart *(Deferred)*

### Sales Last Years
- **Type**: Multi-line chart
- **X-axis**: Time period (months or quarters)
- **Y-axis**: Sales value
- **Lines**: Multiple years for comparison (e.g., 2023, 2024, 2025)
- **Purpose**: Year-over-year sales comparison and trend analysis

---

## Products Delivered Breakdown *(Placeholder — Chart Deferred)*

> **Note**: A placeholder is displayed in the UI. The actual chart implementation is deferred.

### Products Delivered in [Current Month]
- **Type**: Donut/Pie chart
- **Segments**: Product categories with quantities
- **Center**: Total count for the period

---

## Upcoming Orders

A summary list of upcoming orders displayed alongside the products chart:

| Column | Description |
|--------|-------------|
| Status | Order status badge (In Review, Verified, In Progress, Produced, etc.) |
| Paid | Payment indicator |
| Day | Day of week and date |
| Time | Pickup time |
| Location | Café or Bakery |
| Customer | Customer name |
| Items | Product summary with quantities |

This is a condensed view of the order list, showing only the most imminent orders for quick reference.

---

## Soon Due Orders *(Not Yet Implemented)*

> **Note**: This feature is planned but not yet implemented. Currently, upcoming orders show all upcoming orders without urgency prioritization.

Planned urgency indicators:

| Status | Urgency |
|--------|---------|
| In Review (today) | High — needs verification urgently |
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
