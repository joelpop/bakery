package org.vaadin.bakery.service;

/**
 * Notification payload for a new staff message, used to deliver
 * toast notifications to the appropriate audience.
 *
 * @param orderId the order the message was posted on
 * @param orderLocationId the pickup location of the order
 * @param authorId the user who posted the message
 * @param authorName the display name of the author
 * @param textPreview a truncated preview of the message text
 */
public record MessageNotification(
        Long orderId,
        Long orderLocationId,
        Long authorId,
        String authorName,
        String textPreview
) {
}
