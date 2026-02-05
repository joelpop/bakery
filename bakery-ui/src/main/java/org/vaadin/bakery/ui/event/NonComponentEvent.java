package org.vaadin.bakery.ui.event;

/**
 * Base class for events fired by non-component sources.
 * Analogous to {@link com.vaadin.flow.component.ComponentEvent} but for classes
 * that don't extend {@link com.vaadin.flow.component.Component}.
 *
 * @param <N> the type of the event source, must implement {@link NonComponent}
 */
public abstract class NonComponentEvent<N extends NonComponent> {
    private final N source;

    /**
     * Creates a new event with the given source.
     *
     * @param source the source of the event
     */
    protected NonComponentEvent(N source) {
        this.source = source;
    }

    /**
     * Returns the source of the event.
     *
     * @return the source of the event
     */
    public N getSource() {
        return source;
    }
}
