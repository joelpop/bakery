package org.vaadin.bakery.service;

import java.time.ZoneId;

/**
 * Service for managing the current user's browser timezone.
 * Used for converting between server-side Instant (UTC) and browser-local LocalDateTime.
 */
public interface UserTimezoneService {

    /**
     * Sets the browser timezone for the current user session.
     * Should be called by the UI layer after detecting the browser's timezone.
     *
     * @param timezone the browser's timezone
     */
    void setBrowserTimezone(ZoneId timezone);

    /**
     * Gets the browser timezone for the current user session.
     * Returns the system default if not yet set.
     *
     * @return the browser's timezone, or system default as fallback
     */
    ZoneId getBrowserTimezone();

    /**
     * Checks if the browser timezone has been set for this session.
     *
     * @return true if timezone has been explicitly set
     */
    boolean isBrowserTimezoneSet();
}
