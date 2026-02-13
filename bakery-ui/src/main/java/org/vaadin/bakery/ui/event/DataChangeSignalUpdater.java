package org.vaadin.bakery.ui.event;

import org.springframework.stereotype.Component;
import org.vaadin.bakery.service.DataChangeNotifier;
import org.vaadin.bakery.service.MessageNotification;

/**
 * Bridges service-layer data change notifications to shared signals.
 * When a service mutates data, this component increments the corresponding
 * shared signal, causing all subscribed sessions to refresh.
 */
@Component
public class DataChangeSignalUpdater implements DataChangeNotifier {

    @Override
    public void notifyChange(EntityType entityType) {
        switch (entityType) {
            case ORDER -> DataChangeSignals.orderVersion().incrementBy(1);
            case USER -> DataChangeSignals.userVersion().incrementBy(1);
            case PRODUCT -> DataChangeSignals.productVersion().incrementBy(1);
            case LOCATION -> DataChangeSignals.locationVersion().incrementBy(1);
            case MESSAGE -> DataChangeSignals.messageVersion().incrementBy(1);
        }
    }

    @Override
    public void notifyMessage(MessageNotification notification) {
        DataChangeSignals.messageVersion().incrementBy(1);
        MessageBroadcaster.broadcast(notification);
    }
}
