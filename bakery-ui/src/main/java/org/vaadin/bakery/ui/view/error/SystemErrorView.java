package org.vaadin.bakery.ui.view.error;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Error view for 500 Internal Server errors.
 */
@PageTitle("System Error")
public class SystemErrorView extends VerticalLayout implements HasErrorParameter<Exception> {

    private static final Logger logger = LoggerFactory.getLogger(SystemErrorView.class);

    private final Span errorIdSpan;

    public SystemErrorView() {
        addClassName("error-view");
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        var container = new Div();
        container.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.TextAlignment.CENTER
        );
        container.setMaxWidth("400px");

        var icon = new Icon(VaadinIcon.WARNING);
        icon.setSize("64px");
        icon.addClassNames(LumoUtility.TextColor.ERROR);

        var heading = new H1("Something Went Wrong");
        heading.addClassNames(LumoUtility.Margin.NONE);

        var message = new Paragraph(
                "We encountered an unexpected error. Please try again or contact support if the problem persists."
        );
        message.addClassNames(LumoUtility.TextColor.SECONDARY);

        // Error reference ID for support
        var errorIdContainer = new Div();
        errorIdContainer.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.SMALL,
                LumoUtility.FontSize.SMALL
        );
        var errorIdLabel = new Span("Error Reference: ");
        errorIdLabel.addClassNames(LumoUtility.TextColor.SECONDARY);
        errorIdSpan = new Span();
        errorIdSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        errorIdContainer.add(errorIdLabel, errorIdSpan);

        // Action buttons
        var actions = new HorizontalLayout();
        actions.setSpacing(true);

        var retryButton = new Button("Try Again", e -> UI.getCurrent().getPage().reload());
        retryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var homeLink = new Anchor("/", "Go to Home");
        homeLink.addClassNames(
                LumoUtility.TextColor.PRIMARY,
                LumoUtility.FontWeight.SEMIBOLD
        );

        actions.add(retryButton, homeLink);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);

        container.add(icon, heading, message, errorIdContainer, actions);
        add(container);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
        // Generate unique error ID for tracking
        var errorId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        errorIdSpan.setText(errorId);

        // Log the error with the reference ID
        var exception = parameter.getException();
        logger.error("System error [{}]: {}", errorId, exception.getMessage(), exception);

        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }
}
