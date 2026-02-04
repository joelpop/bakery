package org.vaadin.bakery.service;

import org.vaadin.bakery.uimodel.data.LocationSummary;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for location management operations.
 */
public interface LocationService {

    List<LocationSummary> list();

    List<LocationSummary> listActive();

    Optional<LocationSummary> get(Long id);

    LocationSummary create(LocationSummary location);

    LocationSummary update(Long id, LocationSummary location);

    void delete(Long id);

    boolean nameExists(String name);

    boolean nameExistsForOtherLocation(String name, Long locationId);
}
