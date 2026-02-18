package org.vaadin.bakery.uimodel.data;

import java.io.Serializable;

/**
 * Base class for UI models that need identity and version tracking.
 */
public abstract class AbstractModel implements Serializable {

    private Long id;
    private Integer version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean isNew() {
        return id == null;
    }
}
