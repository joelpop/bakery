package org.vaadin.bakery.ui.event;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import org.vaadin.bakery.uimodel.data.LocationSummary;

/**
 * Event fired when the user changes their current working location
 * in the MainLayout header.
 */
public class CurrentLocationChangedEvent extends ComponentEvent<AppLayout> {

    private final LocationSummary location;

    public CurrentLocationChangedEvent(AppLayout source, LocationSummary location) {
        super(source, false);
        this.location = location;
    }

    public LocationSummary getLocation() {
        return location;
    }
}
