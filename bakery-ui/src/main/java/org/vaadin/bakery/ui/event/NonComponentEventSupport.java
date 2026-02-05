package org.vaadin.bakery.ui.event;

import com.vaadin.flow.shared.Registration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Helper class that provides event listener management for {@link NonComponent} implementations.
 * Use via composition to add event support to classes that don't extend
 * {@link com.vaadin.flow.component.Component}.
 *
 * @param <N> the type of the event source
 */
public class NonComponentEventSupport<N extends NonComponent> {
    private final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();

    /**
     * Adds a listener for events of the given type.
     *
     * @param eventType the type of event to listen for
     * @param listener  the listener to add
     * @param <E>       the event type
     * @return a registration that can be used to remove the listener
     */
    public <E extends NonComponentEvent<N>> Registration addListener(Class<E> eventType, Consumer<E> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
        return () -> {
            var list = listeners.get(eventType);
            if (list != null) {
                list.remove(listener);
            }
        };
    }

    /**
     * Fires an event to all registered listeners of the event's type.
     *
     * @param event the event to fire
     * @param <E>   the event type
     */
    @SuppressWarnings("unchecked")
    public <E extends NonComponentEvent<N>> void fireEvent(E event) {
        var list = listeners.get(event.getClass());
        if (list != null) {
            for (var listener : list) {
                ((Consumer<E>) listener).accept(event);
            }
        }
    }
}
