package org.vaadin.bakery.ui.component;

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
public class StaleDataBanner extends HorizontalLayout {

    private final Span messageSpan;
    private final HorizontalLayout actionsLayout;

    public StaleDataBanner() {
        setVisible(false);
        addClassName("stale-data-banner");
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        addClassNames(
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Vertical.SMALL,
                LumoUtility.BorderRadius.MEDIUM
        );

        var icon = VaadinIcon.WARNING.create();
        icon.setSize("var(--lumo-icon-size-s)");

        messageSpan = new Span();
        messageSpan.addClassNames(LumoUtility.FontSize.SMALL);

        actionsLayout = new HorizontalLayout();
        actionsLayout.addClassNames(LumoUtility.Gap.SMALL);

        add(icon, messageSpan, actionsLayout);
        setFlexGrow(1, messageSpan);
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
        removeClassName("stale-data-banner-error");
        addClassName("stale-data-banner-warning");
        setVisible(true);
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
        removeClassName("stale-data-banner-warning");
        addClassName("stale-data-banner-error");
        setVisible(true);
    }

    /**
     * Hides the banner.
     */
    public void hide() {
        setVisible(false);
    }
}
