package org.vaadin.bakery.ui.component;

import org.vaadin.bakery.uimodel.data.AbstractModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks version changes across data refreshes and identifies
 * new/changed items for highlighting.
 */
public class ChangeTracker<T extends AbstractModel> {

    private Map<Long, Integer> previousVersions;
    private final Set<Long> highlightedIds;
    private final Set<Long> newIds;

    public ChangeTracker() {
        highlightedIds = new HashSet<>();
        newIds = new HashSet<>();
    }

    /**
     * Detects changes between previous and current data.
     * Call this with the full (unfiltered) data set on each refresh.
     */
    public void detectChanges(List<? extends T> newData) {
        highlightedIds.clear();
        newIds.clear();

        if (previousVersions != null) {
            for (var item : newData) {
                var prevVersion = previousVersions.get(item.getId());
                if (prevVersion == null) {
                    highlightedIds.add(item.getId());
                    newIds.add(item.getId());
                } else if (!prevVersion.equals(item.getVersion())) {
                    highlightedIds.add(item.getId());
                }
            }
        }

        previousVersions = new HashMap<>();
        for (var item : newData) {
            previousVersions.put(item.getId(), item.getVersion());
        }
    }

    public boolean isHighlighted(Long id) {
        return highlightedIds.contains(id);
    }

    public boolean isNew(Long id) {
        return newIds.contains(id);
    }

    public boolean hasHighlights() {
        return !highlightedIds.isEmpty();
    }
}
