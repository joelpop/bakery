package org.vaadin.bakery.ui.component;

import com.vaadin.flow.signals.shared.SharedMapSignal;
import org.vaadin.bakery.uimodel.data.AbstractModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tracks data changes using a {@link SharedMapSignal} and identifies
 * new/changed items for highlighting with sticky timestamps.
 *
 * <p>Unlike version-diffing, this approach reads change timestamps from a
 * cross-session {@code SharedMapSignal<Long>} (entity ID → change timestamp).
 * Highlights persist across re-renders because the map timestamps survive
 * multiple effect re-runs — solving the double-render highlight stripping bug.
 *
 * <p>On the first call to {@link #processChanges}, a baseline is recorded
 * without applying any highlights. Subsequent calls check the shared map
 * for changes that occurred after the baseline timestamp and within the
 * highlight duration window.
 *
 * @param <T> the model type, must extend {@link AbstractModel}
 */
public class ChangeTracker<T extends AbstractModel> {

    private static final long HIGHLIGHT_DURATION_MS = 8_000;

    private final SharedMapSignal<Long> changesSignal;
    private Set<Long> previousIds;
    private long firstLoadTime;
    private final Map<Long, Long> highlightTimestamps;
    private final Set<Long> newIds;

    /**
     * Creates a change tracker backed by the given shared map signal.
     *
     * @param changesSignal the shared map signal carrying entity change timestamps
     */
    public ChangeTracker(SharedMapSignal<Long> changesSignal) {
        this.changesSignal = changesSignal;
        highlightTimestamps = new HashMap<>();
        newIds = new HashSet<>();
    }

    /**
     * Reads the {@link SharedMapSignal} for recent changes and applies highlights.
     *
     * <p>On the first call, records a baseline timestamp without highlighting.
     * On subsequent calls, checks each item against the shared map for changes
     * that occurred after the baseline and within the highlight duration window.
     *
     * <p>This method should be called inside a {@code Signal.effect} that is
     * already triggered by the corresponding version counter signal. The shared
     * map is read via {@code peek()} (non-reactive) to avoid creating additional
     * reactive dependencies.
     *
     * @param currentData the current data set from the service query
     */
    public void processChanges(List<? extends T> currentData) {
        var now = System.currentTimeMillis();

        if (previousIds == null) {
            // First load — record baseline, no highlights
            firstLoadTime = now;
        } else {
            // Subsequent loads — check shared map for recent changes
            var changes = changesSignal.peek();
            newIds.clear();
            for (var item : currentData) {
                var entry = changes.get(String.valueOf(item.getId()));
                if (entry != null) {
                    var changeTime = entry.peek();
                    if (changeTime != null
                            && changeTime > firstLoadTime
                            && now - changeTime < HIGHLIGHT_DURATION_MS) {
                        highlightTimestamps.put(item.getId(), changeTime);
                        if (!previousIds.contains(item.getId())) {
                            newIds.add(item.getId());
                        }
                    }
                }
            }
        }

        // Expire old highlights
        highlightTimestamps.entrySet().removeIf(
                e -> now - e.getValue() > HIGHLIGHT_DURATION_MS);
        newIds.retainAll(highlightTimestamps.keySet());

        // Track current IDs for next call
        previousIds = currentData.stream()
                .map(AbstractModel::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Returns whether the item with the given ID was new or modified recently.
     */
    public boolean isHighlighted(Long id) {
        return highlightTimestamps.containsKey(id);
    }

    /**
     * Returns whether the item with the given ID was newly added recently.
     */
    public boolean isNew(Long id) {
        return newIds.contains(id);
    }

    /**
     * Returns whether any items are currently highlighted.
     */
    public boolean hasHighlights() {
        return !highlightTimestamps.isEmpty();
    }
}
