package org.vaadin.bakery.uimodel.data;

import org.vaadin.bakery.uimodel.type.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * UI model for storefront order list.
 */
public class OrderList extends AbstractAuditableModel {

    private Long id;
    private OrderStatus status;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private BigDecimal total;
    private boolean paid;
    private String customerName;
    private String locationName;
    private List<OrderItemSummary> items = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    public void setDueTime(LocalTime dueTime) {
        this.dueTime = dueTime;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public List<OrderItemSummary> getItems() {
        return items;
    }

    public void setItems(List<OrderItemSummary> items) {
        this.items = items;
    }

    public String getItemsSummary() {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .map(OrderItemSummary::getDisplayName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}
