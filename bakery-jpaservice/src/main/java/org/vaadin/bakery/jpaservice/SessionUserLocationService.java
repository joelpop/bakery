package org.vaadin.bakery.jpaservice;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import org.vaadin.bakery.service.CurrentUserService;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.UserLocationService;
import org.vaadin.bakery.uimodel.data.LocationSummary;

/**
 * Session-scoped implementation of UserLocationService.
 * Stores the current working location for the duration of the user's session.
 */
@Service
@SessionScope
public class SessionUserLocationService implements UserLocationService {

    private final CurrentUserService currentUserService;
    private final LocationService locationService;

    private LocationSummary currentLocation;

    public SessionUserLocationService(CurrentUserService currentUserService, LocationService locationService) {
        this.currentUserService = currentUserService;
        this.locationService = locationService;
    }

    @Override
    public void setCurrentLocation(LocationSummary location) {
        this.currentLocation = location;
    }

    @Override
    public LocationSummary getCurrentLocation() {
        return currentLocation;
    }

    @Override
    public boolean isCurrentLocationSet() {
        return currentLocation != null;
    }

    @Override
    public void initializeFromUserPrimaryLocation() {
        if (currentLocation != null) {
            return; // Already initialized
        }

        currentUserService.getCurrentUser().ifPresent(user -> {
            var primaryLocationId = user.getPrimaryLocationId();
            if (primaryLocationId != null) {
                locationService.get(primaryLocationId).ifPresent(location -> {
                    this.currentLocation = location;
                });
            }
        });
    }
}
