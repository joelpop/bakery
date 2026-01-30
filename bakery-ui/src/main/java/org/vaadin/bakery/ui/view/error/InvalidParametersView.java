package org.vaadin.bakery.ui.view.error;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Error view for 400 Bad Request / Invalid Parameters errors.
 */
@PageTitle("Invalid Request")
public class InvalidParametersView extends VerticalLayout implements HasErrorParameter<IllegalArgumentException> {

    private final Paragraph detailsMessage;

    public InvalidParametersView() {
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

        var icon = new Icon(VaadinIcon.EXCLAMATION_CIRCLE);
        icon.setSize("64px");
        icon.addClassNames(LumoUtility.TextColor.WARNING);

        var heading = new H1("Invalid Request");
        heading.addClassNames(LumoUtility.Margin.NONE);

        var message = new Paragraph(
                "The request contains invalid parameters or data."
        );
        message.addClassNames(LumoUtility.TextColor.SECONDARY);

        // Details message (only shown when safe to display)
        detailsMessage = new Paragraph();
        detailsMessage.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.SMALL
        );
        detailsMessage.setVisible(false);

        var homeLink = new Anchor("/", "Go to Home");
        homeLink.addClassNames(
                LumoUtility.TextColor.PRIMARY,
                LumoUtility.FontWeight.SEMIBOLD
        );

        container.add(icon, heading, message, detailsMessage, homeLink);
        add(container);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<IllegalArgumentException> parameter) {
        // Only show details if they're safe (no sensitive info)
        var exception = parameter.getException();
        var errorMessage = exception.getMessage();

        if (errorMessage != null && isSafeToDisplay(errorMessage)) {
            detailsMessage.setText(errorMessage);
            detailsMessage.setVisible(true);
        }

        return HttpServletResponse.SC_BAD_REQUEST;
    }

    private boolean isSafeToDisplay(String message) {
        // Don't display messages that might contain sensitive info
        var lowerMessage = message.toLowerCase();
        return !lowerMessage.contains("password") &&
                !lowerMessage.contains("secret") &&
                !lowerMessage.contains("token") &&
                !lowerMessage.contains("key") &&
                !lowerMessage.contains("sql") &&
                !lowerMessage.contains("database") &&
                message.length() < 200;
    }
}
