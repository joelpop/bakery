package org.vaadin.bakery.jpaservice;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import org.vaadin.bakery.service.UserTimezoneService;

import java.time.ZoneId;

/**
 * Session-scoped implementation of UserTimezoneService.
 * Stores the browser timezone for the duration of the user's session.
 */
@Service
@SessionScope
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
