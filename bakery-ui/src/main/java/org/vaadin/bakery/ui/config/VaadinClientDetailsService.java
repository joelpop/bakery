package org.vaadin.bakery.ui.config;

import com.vaadin.flow.component.UI;
import org.springframework.stereotype.Service;
import org.vaadin.bakery.service.ClientDetailsService;

import java.time.ZoneId;

/**
 * Vaadin-based implementation of {@link ClientDetailsService}.
 * Delegates to {@link UI#getCurrent()} to access the browser's extended client details.
 *
 * <p>Methods must be called from a Vaadin request context (where {@link UI#getCurrent()}
 * is available). Calling from the wrong context will result in an NPE.</p>
 */
@Service
public class VaadinClientDetailsService implements ClientDetailsService {

    @Override
    public ZoneId getBrowserTimezone() {
        return ZoneId.of(UI.getCurrent().getPage().getExtendedClientDetails().getTimeZoneId());
    }
}
