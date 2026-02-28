package org.vaadin.bakery.ui.view.error;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
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
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Error view for 403 Access Denied errors.
 * Note: For security, this displays as a 404 to avoid information disclosure.
 */
@PageTitle("Page not Found")
@PermitAll
public class AccessDeniedView extends Composite<VerticalLayout> implements HasSize, HasStyle, HasErrorParameter<AccessDeniedException> {

    public AccessDeniedView() {
        // Component initializations
        var container = new Div();
        container.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.TextAlignment.CENTER
        );
        container.setMaxWidth("400px");

        // Display as 404 to avoid revealing that the page exists
        var icon = new Icon(VaadinIcon.SEARCH);
        icon.setSize("64px");
        icon.addClassNames(LumoUtility.TextColor.SECONDARY);

        var heading = new H1("Page not Found");
        heading.addClassNames(LumoUtility.Margin.NONE);

        var message = new Paragraph(
                "The page you're looking for doesn't exist or has been moved."
        );
        message.addClassNames(LumoUtility.TextColor.SECONDARY);

        var homeLink = new Anchor("/", "Go to Home");
        homeLink.addClassNames(
                LumoUtility.TextColor.PRIMARY,
                LumoUtility.FontWeight.SEMIBOLD
        );

        container.add(icon, heading, message, homeLink);

        // Content layout
        var content = getContent();
        content.addClassName("error-view");
        content.setSizeFull();
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        content.add(container);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<AccessDeniedException> parameter) {
        // Return 404 instead of 403 to avoid information disclosure
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
