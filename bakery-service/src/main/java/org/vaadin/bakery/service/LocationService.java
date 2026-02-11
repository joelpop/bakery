package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.LocationSummary;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for location management operations.
 */
public interface LocationService {

    /**
     * Returns all locations.
     */
    List<LocationSummary> list();

    /**
     * Returns only active (non-deleted) locations.
     */
    List<LocationSummary> listActive();

    /**
     * Returns the location with the given ID, if found.
     */
    Optional<LocationSummary> get(Long id);

    /**
     * Creates a new location and returns the saved result.
     */
    LocationSummary create(LocationSummary location);

    /**
     * Updates an existing location identified by the given ID.
     */
    LocationSummary update(Long id, LocationSummary location);

    /**
     * Deletes the location with the given ID.
     */
    void delete(Long id);

    /**
     * Returns the current optimistic-lock version for the given location.
     */
    Optional<Integer> getVersion(Long id);

    /**
     * Checks whether the given name is already in use by any location.
     */
    boolean nameExists(String name);

    /**
     * Checks whether the given name is in use by a location other than the specified one.
     */
    boolean nameExistsForOtherLocation(String name, Long locationId);
}
