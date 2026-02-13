package org.vaadin.bakery.uimodel.data;

import org.vaadin.bakery.uimodel.type.OrderActivityType;

import java.time.LocalDateTime;

/**
 * UI model for an entry in an order's activity timeline.
 */
public class OrderActivity {

    private Long id;
    private OrderActivityType type;
    private String text;
    private String authorName;
    private Long authorId;
    private String referencedItemName;
    private Long referencedItemId;
    private LocalDateTime postedAt;
    private boolean read;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrderActivityType getType() {
        return type;
    }

    public void setType(OrderActivityType type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getReferencedItemName() {
        return referencedItemName;
    }

    public void setReferencedItemName(String referencedItemName) {
        this.referencedItemName = referencedItemName;
    }

    public Long getReferencedItemId() {
        return referencedItemId;
    }

    public void setReferencedItemId(Long referencedItemId) {
        this.referencedItemId = referencedItemId;
    }

    public LocalDateTime getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt) {
        this.postedAt = postedAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
