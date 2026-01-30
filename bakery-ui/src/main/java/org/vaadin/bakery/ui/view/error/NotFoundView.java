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
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Error view for 404 Not Found errors.
 */
@PageTitle("Page Not Found")
public class NotFoundView extends VerticalLayout implements HasErrorParameter<NotFoundException> {

    public NotFoundView() {
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

        var icon = new Icon(VaadinIcon.SEARCH);
        icon.setSize("64px");
        icon.addClassNames(LumoUtility.TextColor.SECONDARY);

        var heading = new H1("Page Not Found");
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
        add(container);
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        return HttpServletResponse.SC_NOT_FOUND;
    }
}
