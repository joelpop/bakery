package org.vaadin.bakery.uimodel.data;

import java.math.BigDecimal;

/**
 * UI model for product admin grid.
 */
public class ProductSummary extends AbstractModel {

    private String name;
    private String description;
    private String size;
    private BigDecimal price;
    private boolean available;
    private boolean batchable;
    private byte[] photo;
    private String photoContentType;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isBatchable() {
        return batchable;
    }

    public void setBatchable(boolean batchable) {
        this.batchable = batchable;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public String getPhotoContentType() {
        return photoContentType;
    }

    public void setPhotoContentType(String photoContentType) {
        this.photoContentType = photoContentType;
    }
}
