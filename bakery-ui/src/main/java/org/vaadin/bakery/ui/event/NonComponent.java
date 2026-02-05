package org.vaadin.bakery.ui.event;

import com.vaadin.flow.shared.Registration;

import java.util.function.Consumer;

/**
 * Interface for classes that can fire events but don't extend
 * {@link com.vaadin.flow.component.Component}.
 * Analogous to how {@link com.vaadin.flow.component.Component} provides event capabilities.
 */
public interface NonComponent {

    /**
     * Adds a listener for events of the given type.
     *
     * @param eventType the type of event to listen for
     * @param listener  the listener to add
     * @param <E>       the event type
     * @return a registration that can be used to remove the listener
     */
    <E extends NonComponentEvent<?>> Registration addListener(Class<E> eventType, Consumer<E> listener);
}
