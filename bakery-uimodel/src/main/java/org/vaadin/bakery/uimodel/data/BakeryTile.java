package org.vaadin.bakery.uimodel.data;

import org.vaadin.bakery.uimodel.type.OrderItemStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * UI model representing a tile on the bakery board.
 *
 * <p>Batchable products aggregate items across orders into a single tile.
 * Non-batchable products produce one tile per order item.
 */
public class BakeryTile implements Serializable {

    private String groupingKey;
    private boolean batchable;
    private Long productId;
    private String productName;
    private String productSize;
    private LocalDate dueDate;
    private OrderItemStatus status;
    private int totalQuantity;
    private int orderCount;
    private boolean hasNotes;
    private boolean hasUnreadMessages;
    private boolean onHold;
    private boolean undoAvailable;
    private int position;

    // Non-batchable fields (single order item)
    private Long orderId;
    private Long itemId;
    private Integer itemVersion;
    private String orderReference;
    private String itemDetails;

    // Batchable fields (aggregated across orders)
    private List<Long> itemIds;
    private List<Integer> itemVersions;

    public String getGroupingKey() {
        return groupingKey;
    }

    public void setGroupingKey(String groupingKey) {
        this.groupingKey = groupingKey;
    }

    public boolean isBatchable() {
        return batchable;
    }

    public void setBatchable(boolean batchable) {
        this.batchable = batchable;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductSize() {
        return productSize;
    }

    public void setProductSize(String productSize) {
        this.productSize = productSize;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public OrderItemStatus getStatus() {
        return status;
    }

    public void setStatus(OrderItemStatus status) {
        this.status = status;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public boolean isHasNotes() {
        return hasNotes;
    }

    public void setHasNotes(boolean hasNotes) {
        this.hasNotes = hasNotes;
    }

    public boolean isHasUnreadMessages() {
        return hasUnreadMessages;
    }

    public void setHasUnreadMessages(boolean hasUnreadMessages) {
        this.hasUnreadMessages = hasUnreadMessages;
    }

    public boolean isOnHold() {
        return onHold;
    }

    public void setOnHold(boolean onHold) {
        this.onHold = onHold;
    }

    public boolean isUndoAvailable() {
        return undoAvailable;
    }

    public void setUndoAvailable(boolean undoAvailable) {
        this.undoAvailable = undoAvailable;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Integer getItemVersion() {
        return itemVersion;
    }

    public void setItemVersion(Integer itemVersion) {
        this.itemVersion = itemVersion;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public void setOrderReference(String orderReference) {
        this.orderReference = orderReference;
    }

    public String getItemDetails() {
        return itemDetails;
    }

    public void setItemDetails(String itemDetails) {
        this.itemDetails = itemDetails;
    }

    public List<Long> getItemIds() {
        return itemIds;
    }

    public void setItemIds(List<Long> itemIds) {
        this.itemIds = itemIds;
    }

    public List<Integer> getItemVersions() {
        return itemVersions;
    }

    public void setItemVersions(List<Integer> itemVersions) {
        this.itemVersions = itemVersions;
    }
}
