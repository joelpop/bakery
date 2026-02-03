package org.vaadin.bakery.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * Reusable view header component providing consistent styling across views.
 * Layout: [Title] [Filter components...] [Action button]
 */
public class ViewHeader extends HorizontalLayout {

    private final H2 title;
    private final HorizontalLayout filterArea;
    private Button actionButton;

    /**
     * Creates a view header with just a title.
     *
     * @param titleText the view title
     */
    public ViewHeader(String titleText) {
        addClassName("view-header");
        addClassName("title-only"); // Will be removed when filters/actions added
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        addClassNames(
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Padding.Vertical.SMALL
        );

        title = new H2(titleText);
        title.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.NONE,
                LumoUtility.Whitespace.NOWRAP
        );

        filterArea = new HorizontalLayout();
        filterArea.setAlignItems(FlexComponent.Alignment.CENTER);
        filterArea.addClassNames(LumoUtility.Gap.MEDIUM);
        filterArea.setSpacing(false);

        add(title, filterArea);
    }

    /**
     * Adds filter components to the header.
     *
     * @param components the filter components to add
     * @return this header for method chaining
     */
    public ViewHeader withFilters(Component... components) {
        removeClassName("title-only");
        filterArea.add(components);
        return this;
    }

    /**
     * Adds a primary action button to the header.
     * On mobile, only the icon is shown; on desktop, both icon and text are visible.
     *
     * @param buttonText the button text (without the "+")
     * @param clickHandler the click handler
     * @return this header for method chaining
     */
    public ViewHeader withAction(String buttonText, Runnable clickHandler) {
        removeClassName("title-only");

        var icon = new Icon(VaadinIcon.PLUS);
        var text = new Span(buttonText);
        text.addClassName("button-text");

        actionButton = new Button(icon);
        actionButton.setSuffixComponent(text);
        actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        actionButton.addClassName("view-action-button");
        actionButton.addClickListener(e -> clickHandler.run());
        add(actionButton);
        return this;
    }

    /**
     * Adds a custom action button to the header.
     *
     * @param button the button to add
     * @return this header for method chaining
     */
    public ViewHeader withAction(Button button) {
        removeClassName("title-only");
        this.actionButton = button;
        add(button);
        return this;
    }

    /**
     * Gets the title component for customization.
     */
    public H2 getTitle() {
        return title;
    }

    /**
     * Gets the filter area for direct manipulation.
     */
    public HorizontalLayout getFilterArea() {
        return filterArea;
    }

    /**
     * Gets the action button if one was added.
     */
    public Button getActionButton() {
        return actionButton;
    }
}
