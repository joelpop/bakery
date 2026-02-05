package org.vaadin.bakery.jpamodel.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.vaadin.bakery.jpamodel.projection.LocationSummaryProjection;

/**
 * Pickup locations for orders.
 */
@Entity
@Table(name = "location")
public class LocationEntity extends AbstractAuditableEntity implements LocationSummaryProjection {

    @NotBlank
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "default_country_code")
    private String defaultCountryCode;

    @Column(name = "default_area_code")
    private String defaultAreaCode;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @NotNull
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getDefaultCountryCode() {
        return defaultCountryCode;
    }

    public void setDefaultCountryCode(String defaultCountryCode) {
        this.defaultCountryCode = defaultCountryCode;
    }

    public String getDefaultAreaCode() {
        return defaultAreaCode;
    }

    public void setDefaultAreaCode(String defaultAreaCode) {
        this.defaultAreaCode = defaultAreaCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
