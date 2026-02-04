package org.vaadin.bakery.uimodel.data;

import org.vaadin.bakery.uimodel.type.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * UI model for order detail/edit.
 */
public class OrderDetail extends AbstractAuditableModel {

    private Long id;
    private OrderStatus status;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private String additionalDetails;
    private BigDecimal total;
    private BigDecimal discount;
    private boolean paid;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private Long locationId;
    private String locationName;
    private List<OrderItemDetail> items = new ArrayList<>();

    // Transient field set during create to indicate if a new customer was created
    private boolean newCustomerCreated;

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

    public String getAdditionalDetails() {
        return additionalDetails;
    }

    public void setAdditionalDetails(String additionalDetails) {
        this.additionalDetails = additionalDetails;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public Long getLocationId() {
        return locationId;
    }

    public void setLocationId(Long locationId) {
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public List<OrderItemDetail> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDetail> items) {
        this.items = items;
    }

    public boolean isNew() {
        return id == null;
    }

    public boolean isNewCustomerCreated() {
        return newCustomerCreated;
    }

    public void setNewCustomerCreated(boolean newCustomerCreated) {
        this.newCustomerCreated = newCustomerCreated;
    }

    public void calculateTotal() {
        var itemsTotal = items.stream()
                .map(OrderItemDetail::getLineTotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (discount != null && discount.compareTo(BigDecimal.ZERO) > 0) {
            total = itemsTotal.subtract(discount);
        } else {
            total = itemsTotal;
        }
    }
}
