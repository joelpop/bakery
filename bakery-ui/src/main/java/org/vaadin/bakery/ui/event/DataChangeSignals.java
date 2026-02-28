package org.vaadin.bakery.ui.event;

import com.vaadin.flow.signals.shared.SharedMapSignal;
import com.vaadin.flow.signals.shared.SharedNumberSignal;
import org.vaadin.bakery.service.DataChangeNotifier.EntityType;

/**
 * Registry of shared signals for cross-session data change notifications.
 * <p>
 * Each entity type has two complementary shared signals:
 * <ul>
 *   <li><b>Version counter</b> ({@link SharedNumberSignal}) — incremented on every change,
 *       serves as a reliable trigger for {@code Signal.effect()} subscriptions.</li>
 *   <li><b>Change map</b> ({@link SharedMapSignal}{@code <Long>}) — maps entity IDs (as strings)
 *       to change timestamps, carrying the identity of what changed and when.</li>
 * </ul>
 * <p>
 * Together they form a complete notification system: the counter guarantees effects fire
 * (even for repeated changes to the same entity), while the map tells views exactly
 * which entities changed — eliminating the need for version-based diffing.
 * <p>
 * These are static singletons so all UI sessions share the same signal instances.
 */
public final class DataChangeSignals {

    // Trigger signals — always increment, guaranteeing effect re-runs
    private static final SharedNumberSignal orderVersion;
    private static final SharedNumberSignal userVersion;
    private static final SharedNumberSignal productVersion;
    private static final SharedNumberSignal locationVersion;
    private static final SharedNumberSignal messageVersion;

    // Change detail signals — entity ID (as String key) → change timestamp
    private static final SharedMapSignal<Long> orderChanges;
    private static final SharedMapSignal<Long> userChanges;
    private static final SharedMapSignal<Long> productChanges;
    private static final SharedMapSignal<Long> locationChanges;
    private static final SharedMapSignal<Long> messageChanges;

    // Tile-level change signal — tile grouping key → change timestamp
    private static final SharedMapSignal<Long> tileChanges;

    static {
        orderVersion = new SharedNumberSignal();
        userVersion = new SharedNumberSignal();
        productVersion = new SharedNumberSignal();
        locationVersion = new SharedNumberSignal();
        messageVersion = new SharedNumberSignal();

        orderChanges = new SharedMapSignal<>(Long.class);
        userChanges = new SharedMapSignal<>(Long.class);
        productChanges = new SharedMapSignal<>(Long.class);
        locationChanges = new SharedMapSignal<>(Long.class);
        messageChanges = new SharedMapSignal<>(Long.class);
        tileChanges = new SharedMapSignal<>(Long.class);
    }

    private DataChangeSignals() {
    }

    // --- Trigger signal accessors ---

    public static SharedNumberSignal orderVersion() {
        return orderVersion;
    }

    public static SharedNumberSignal userVersion() {
        return userVersion;
    }

    public static SharedNumberSignal productVersion() {
        return productVersion;
    }

    public static SharedNumberSignal locationVersion() {
        return locationVersion;
    }

    public static SharedNumberSignal messageVersion() {
        return messageVersion;
    }

    // --- Change detail signal accessors ---

    public static SharedMapSignal<Long> orderChanges() {
        return orderChanges;
    }

    public static SharedMapSignal<Long> userChanges() {
        return userChanges;
    }

    public static SharedMapSignal<Long> productChanges() {
        return productChanges;
    }

    public static SharedMapSignal<Long> locationChanges() {
        return locationChanges;
    }

    public static SharedMapSignal<Long> messageChanges() {
        return messageChanges;
    }

    /** Returns the tile-level change signal for bakery board tile operations. */
    public static SharedMapSignal<Long> tileChanges() {
        return tileChanges;
    }

    /**
     * Returns the change detail signal for the given entity type.
     */
    public static SharedMapSignal<Long> changesFor(EntityType entityType) {
        return switch (entityType) {
            case ORDER -> orderChanges;
            case USER -> userChanges;
            case PRODUCT -> productChanges;
            case LOCATION -> locationChanges;
            case MESSAGE -> messageChanges;
        };
    }
}
