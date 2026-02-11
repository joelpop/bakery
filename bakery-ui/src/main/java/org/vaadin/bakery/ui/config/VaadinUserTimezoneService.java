package org.vaadin.bakery.ui.config;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.stereotype.Service;
import org.vaadin.bakery.service.UserTimezoneService;

import java.time.ZoneId;

/**
 * VaadinSession-based implementation of UserTimezoneService.
 * Stores the browser timezone in VaadinSession attributes, which are accessible
 * from both HTTP request threads and Push threads (unlike Spring's @SessionScope).
 */
@Service
public class VaadinUserTimezoneService implements UserTimezoneService {

    private static final String TIMEZONE_ATTRIBUTE = "browserTimezone";

    @Override
    public void setBrowserTimezone(ZoneId timezone) {
        var session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(TIMEZONE_ATTRIBUTE, timezone);
        }
    }

    @Override
    public ZoneId getBrowserTimezone() {
        var session = VaadinSession.getCurrent();
        if (session != null) {
            var tz = (ZoneId) session.getAttribute(TIMEZONE_ATTRIBUTE);
            if (tz != null) {
                return tz;
            }
        }
        return ZoneId.systemDefault();
    }

    @Override
    public boolean isBrowserTimezoneSet() {
        var session = VaadinSession.getCurrent();
        return session != null && session.getAttribute(TIMEZONE_ATTRIBUTE) != null;
    }
}
