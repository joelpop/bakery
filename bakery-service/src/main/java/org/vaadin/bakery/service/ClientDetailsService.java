package org.vaadin.bakery.service;

import java.time.ZoneId;

/**
 * Service for accessing the current user's browser client details.
 * Provides typed accessors for client-side information such as timezone,
 * which is used for converting between server-side Instant (UTC) and browser-local LocalDateTime.
 *
 * <p>The implementation lazily detects and caches the browser's extended client details
 * on first access, so callers need not explicitly set values.</p>
 */
public interface ClientDetailsService {

    /**
     * Gets the browser timezone for the current user session.
     * Returns the system default if not yet detected.
     *
     * @return the browser's timezone, or system default as fallback
     */
    ZoneId getBrowserTimezone();
}
