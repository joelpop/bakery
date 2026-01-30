package org.vaadin.bakery.uimodel.data;

import java.math.BigDecimal;

/**
 * UI model for order form product dropdown.
 */
public class ProductSelect {

    private Long id;
    private String name;
    private String size;
    private BigDecimal price;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getDisplayName() {
        return name + " (" + size + ")";
    }
}
