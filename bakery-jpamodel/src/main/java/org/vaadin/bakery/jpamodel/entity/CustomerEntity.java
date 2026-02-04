package org.vaadin.bakery.jpamodel.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import org.vaadin.bakery.jpamodel.projection.CustomerSummaryProjection;

import java.util.ArrayList;
import java.util.List;

/**
 * Customers who place orders at the bakery.
 */
@Entity
@Table(name = "customer")
public class CustomerEntity extends AbstractAuditableEntity implements CustomerSummaryProjection {

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderEntity> orders = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<OrderEntity> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderEntity> orders) {
        this.orders = orders;
    }
}
