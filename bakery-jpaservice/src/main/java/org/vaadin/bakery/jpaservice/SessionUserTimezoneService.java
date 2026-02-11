package org.vaadin.bakery.jpaservice;

import org.vaadin.bakery.service.UserTimezoneService;

import java.time.ZoneId;

/**
 * Simple field-based implementation of UserTimezoneService.
 * Superseded by VaadinUserTimezoneService which stores timezone in VaadinSession
 * (accessible from both HTTP request threads and Push threads).
 */
public class SessionUserTimezoneService implements UserTimezoneService {

    private ZoneId browserTimezone;

    @Override
    public void setBrowserTimezone(ZoneId timezone) {
        this.browserTimezone = timezone;
    }

    @Override
    public ZoneId getBrowserTimezone() {
        return browserTimezone != null ? browserTimezone : ZoneId.systemDefault();
    }

    @Override
    public boolean isBrowserTimezoneSet() {
        return browserTimezone != null;
    }
}
