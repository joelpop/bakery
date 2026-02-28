package org.vaadin.bakery.ui.component;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * A banner component for displaying stale data notifications in edit dialogs.
 * Shows when the record being edited has been modified or deleted by another user.
 */
public class StaleDataBanner extends Composite<HorizontalLayout> implements HasSize, HasStyle {

    private final Span messageSpan;
    private final HorizontalLayout actionsLayout;

    /** Creates a stale data banner, initially hidden. */
    public StaleDataBanner() {
        // Component initializations
        var icon = VaadinIcon.WARNING.create();
        icon.setSize("var(--lumo-icon-size-s)");

        messageSpan = new Span();
        messageSpan.addClassNames(LumoUtility.FontSize.SMALL);

        actionsLayout = new HorizontalLayout();
        actionsLayout.addClassNames(LumoUtility.Gap.SMALL);

        // Content layout
        var content = getContent();
        content.setVisible(false);
        content.addClassName("stale-data-banner");
        content.setWidthFull();
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.addClassNames(
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.BorderRadius.MEDIUM
        );
        content.add(icon, messageSpan, actionsLayout);
        content.setFlexGrow(1, messageSpan);
    }

    /**
     * Shows the banner indicating the record was modified by another user.
     *
     * @param onReload action to reload fresh data into the form
     */
    public void showModified(Runnable onReload) {
        messageSpan.setText("This record was modified by another user.");
        actionsLayout.removeAll();

        var reloadButton = new Button("Reload", _ -> onReload.run());
        reloadButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);

        var dismissButton = new Button("Dismiss", _ -> hide());
        dismissButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);

        actionsLayout.add(reloadButton, dismissButton);
        getContent().removeClassName("stale-data-banner-error");
        getContent().addClassName("stale-data-banner-warning");
        getContent().setVisible(true);
    }

    /**
     * Shows the banner indicating the record was deleted by another user.
     *
     * @param onClose action to close the dialog
     */
    public void showDeleted(Runnable onClose) {
        messageSpan.setText("This record has been deleted.");
        actionsLayout.removeAll();

        var closeButton = new Button("Close", _ -> onClose.run());
        closeButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);

        actionsLayout.add(closeButton);
        getContent().removeClassName("stale-data-banner-warning");
        getContent().addClassName("stale-data-banner-error");
        getContent().setVisible(true);
    }

    /** Hides the banner. */
    public void hide() {
        getContent().setVisible(false);
    }
}
