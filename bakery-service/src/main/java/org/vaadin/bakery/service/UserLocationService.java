package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.LocationSummary;

/**
 * Service for managing the current user's working location.
 * The working location is session-scoped and used for:
 * - Pre-populating the location field in new order dialogs
 * - Providing a "Current Location" filter option in views
 */
public interface UserLocationService {

    /**
     * Sets the current working location for the user session.
     *
     * @param location the location to set as current
     */
    void setCurrentLocation(LocationSummary location);

    /**
     * Gets the current working location for the user session.
     *
     * @return the current location, or null if not set
     */
    LocationSummary getCurrentLocation();

    /**
     * Checks if the current location has been set for this session.
     *
     * @return true if a location has been explicitly set
     */
    boolean isCurrentLocationSet();

    /**
     * Initializes the current location from the authenticated user's primary location.
     * Called on first attach of MainLayout to set initial working location.
     */
    void initializeFromUserPrimaryLocation();
}
