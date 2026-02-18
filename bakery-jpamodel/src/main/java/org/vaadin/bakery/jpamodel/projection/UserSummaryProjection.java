package org.vaadin.bakery.jpamodel.projection;

import org.jspecify.annotations.Nullable;
import org.vaadin.bakery.jpamodel.code.UserRoleCode;

/**
 * Projection for user list grid display.
 */
public interface UserSummaryProjection {

    Long getId();

    String getEmail();

    String getFirstName();

    String getLastName();

    UserRoleCode getRole();

    byte[] getPhoto();

    String getPhotoContentType();

    @Nullable Long getPrimaryLocationId();

    Integer getVersion();
}
