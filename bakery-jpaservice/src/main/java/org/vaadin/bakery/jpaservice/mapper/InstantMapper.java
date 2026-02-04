package org.vaadin.bakery.jpaservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.bakery.service.UserTimezoneService;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * MapStruct mapper for converting between Instant (server/storage) and LocalDateTime (browser/display).
 * Uses the user's browser timezone from the session-scoped UserTimezoneService.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class InstantMapper {

    private UserTimezoneService userTimezoneService;

    @Autowired
    public void setUserTimezoneService(UserTimezoneService userTimezoneService) {
        this.userTimezoneService = userTimezoneService;
    }

    /**
     * Converts a server-side Instant (UTC) to a LocalDateTime in the user's browser timezone.
     *
     * @param instant the UTC instant from the server/database
     * @return LocalDateTime in the user's browser timezone, or null if instant is null
     */
    public LocalDateTime toBrowserTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, userTimezoneService.getBrowserTimezone());
    }

    /**
     * Converts a browser-local LocalDateTime to a server-side Instant (UTC).
     *
     * @param localDateTime the LocalDateTime from the user's browser timezone
     * @return Instant in UTC for server/database storage, or null if localDateTime is null
     */
    public Instant toServerTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(userTimezoneService.getBrowserTimezone()).toInstant();
    }
}
