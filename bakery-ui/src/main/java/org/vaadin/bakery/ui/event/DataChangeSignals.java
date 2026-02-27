package org.vaadin.bakery.ui.event;

import com.vaadin.flow.signals.shared.SharedNumberSignal;

/**
 * Registry of shared signals for cross-session data change notifications.
 * Each entity type has a counter signal that is incremented when data changes.
 * All sessions watching the counter automatically refresh.
 * <p>
 * These are static singletons so all UI sessions share the same signal instances.
 * When one session increments a counter, all sessions subscribed via
 * {@code Signal.effect()} are notified.
 */
public final class DataChangeSignals {

    private static final SharedNumberSignal orderVersion;
    private static final SharedNumberSignal userVersion;
    private static final SharedNumberSignal productVersion;
    private static final SharedNumberSignal locationVersion;
    private static final SharedNumberSignal messageVersion;

    static {
        orderVersion = new SharedNumberSignal();
        userVersion = new SharedNumberSignal();
        productVersion = new SharedNumberSignal();
        locationVersion = new SharedNumberSignal();
        messageVersion = new SharedNumberSignal();
    }

    private DataChangeSignals() {
    }

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
}
