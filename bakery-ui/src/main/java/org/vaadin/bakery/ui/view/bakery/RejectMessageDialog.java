package org.vaadin.bakery.ui.view.bakery;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.shared.Registration;
import org.vaadin.bakery.ui.event.NonComponent;
import org.vaadin.bakery.ui.event.NonComponentEvent;
import org.vaadin.bakery.ui.event.NonComponentEventSupport;

import java.util.function.Consumer;

/**
 * Dialog for entering a rejection reason when rejecting a bakery tile.
 * Uses delegation pattern rather than extending Dialog.
 */
public class RejectMessageDialog implements NonComponent {

    private final Dialog dialog;
    private final NonComponentEventSupport<RejectMessageDialog> eventSupport;

    /** Creates the reject message dialog. */
    public RejectMessageDialog() {
        dialog = new Dialog();
        eventSupport = new NonComponentEventSupport<>();

        dialog.setHeaderTitle("Reject Item");
        dialog.setWidth("400px");
        dialog.setCloseOnOutsideClick(false);

        var messageField = new TextArea("Rejection Reason");
        messageField.setWidthFull();
        messageField.setRequired(true);
        messageField.setPlaceholder("Explain why this item is being rejected...");
        messageField.setMinHeight("100px");
        dialog.add(messageField);

        var cancelButton = new Button("Cancel", _ -> {
            dialog.close();
            fireEvent(new CancelEvent(this));
        });

        var confirmButton = new Button("Reject", _ -> {
            if (messageField.getValue().isBlank()) {
                messageField.setInvalid(true);
                messageField.setErrorMessage("Reason is required");
                return;
            }
            dialog.close();
            fireEvent(new ConfirmEvent(this, messageField.getValue()));
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        dialog.getFooter().add(cancelButton, confirmButton);
    }

    /** Opens the dialog. */
    public void open() {
        dialog.open();
    }

    /** Closes the dialog. */
    public void close() {
        dialog.close();
    }

    @Override
    public <E extends NonComponentEvent<?>> Registration addListener(Class<E> eventType, Consumer<E> listener) {
        return eventSupport.addListener((Class<NonComponentEvent<RejectMessageDialog>>) eventType,
                (Consumer<NonComponentEvent<RejectMessageDialog>>) listener);
    }

    /** Adds a listener for confirm (reject) events. */
    public Registration addConfirmListener(Consumer<ConfirmEvent> listener) {
        return eventSupport.addListener(ConfirmEvent.class, listener);
    }

    /** Adds a listener for cancel events. */
    public Registration addCancelListener(Consumer<CancelEvent> listener) {
        return eventSupport.addListener(CancelEvent.class, listener);
    }

    private void fireEvent(NonComponentEvent<RejectMessageDialog> event) {
        eventSupport.fireEvent(event);
    }

    /** Event fired when the user confirms the rejection with a message. */
    public static class ConfirmEvent extends NonComponentEvent<RejectMessageDialog> {
        private final String message;

        public ConfirmEvent(RejectMessageDialog source, String message) {
            super(source);
            this.message = message;
        }

        /** Returns the rejection reason entered by the user. */
        public String getMessage() {
            return message;
        }
    }

    /** Event fired when the user cancels the rejection. */
    public static class CancelEvent extends NonComponentEvent<RejectMessageDialog> {
        public CancelEvent(RejectMessageDialog source) {
            super(source);
        }
    }
}
