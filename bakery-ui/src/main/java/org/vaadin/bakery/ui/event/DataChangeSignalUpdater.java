package org.vaadin.bakery.ui.event;

import org.springframework.stereotype.Component;
import org.vaadin.bakery.service.DataChangeNotifier;
import org.vaadin.bakery.service.MessageNotification;

/**
 * Bridges service-layer data change notifications to shared signals.
 * <p>
 * When a service mutates data, this component records the change identity
 * in a {@link com.vaadin.flow.signals.shared.SharedMapSignal SharedMapSignal}
 * (entity ID → timestamp) and then increments the corresponding
 * {@link com.vaadin.flow.signals.shared.SharedNumberSignal SharedNumberSignal}
 * to trigger all subscribed sessions' effects.
 */
@Component
public class DataChangeSignalUpdater implements DataChangeNotifier {

    @Override
    public void notifyChange(EntityType entityType, long entityId) {
        // Record change identity (entity ID → timestamp)
        DataChangeSignals.changesFor(entityType)
                .put(String.valueOf(entityId), System.currentTimeMillis());

        // Increment trigger counter
        switch (entityType) {
            case ORDER -> DataChangeSignals.orderVersion().incrementBy(1);
            case USER -> DataChangeSignals.userVersion().incrementBy(1);
            case PRODUCT -> DataChangeSignals.productVersion().incrementBy(1);
            case LOCATION -> DataChangeSignals.locationVersion().incrementBy(1);
            case MESSAGE -> DataChangeSignals.messageVersion().incrementBy(1);
        }
    }

    @Override
    public void notifyTileChange(String groupingKey) {
        DataChangeSignals.tileChanges().put(groupingKey, System.currentTimeMillis());
        DataChangeSignals.orderVersion().incrementBy(1);
    }

    @Override
    public void notifyMessage(MessageNotification notification) {
        DataChangeSignals.messageChanges()
                .put(String.valueOf(notification.orderId()), System.currentTimeMillis());
        DataChangeSignals.messageVersion().incrementBy(1);
        MessageBroadcaster.broadcast(notification);
    }
}
