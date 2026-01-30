package org.vaadin.bakery.ui.view.login;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;
/**
 * Login view for user authentication.
 * Features:
 * - Café Sunshine branding
 * - Username/password login
 * - Passkey login button
 * - Error display for invalid credentials
 * - Role-based redirect after login:
 *   - Admin → Dashboard
 *   - Baker/Barista → Storefront
 */
@Route(value = "login", autoLayout = false)
@PageTitle("Login | Café Sunshine")
@AnonymousAllowed
public class LoginView extends Main implements BeforeEnterObserver {

    private final LoginForm loginForm;

    public LoginView() {
        // Set explicit styles for full-page centering
        getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("min-height", "100vh")
                .set("width", "100%")
                .set("background", "var(--lumo-shade-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("box-sizing", "border-box");

        var container = new VerticalLayout();
        container.setAlignItems(FlexComponent.Alignment.CENTER);
        container.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        container.setMaxWidth("400px");
        container.setWidth("100%");
        container.addClassNames(
                LumoUtility.Background.BASE,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.BoxShadow.MEDIUM,
                LumoUtility.Padding.LARGE
        );

        // Branding
        var branding = createBranding();

        // Login form
        loginForm = createLoginForm();

        // Divider
        var divider = createDivider();

        // Passkey login button
        var passkeyButton = createPasskeyButton();

        container.add(branding, loginForm, divider, passkeyButton);
        add(container);
    }

    private Div createBranding() {
        var branding = new Div();
        branding.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Margin.Bottom.LARGE
        );

        var icon = new Icon(VaadinIcon.SUN_O);
        icon.setSize("64px");
        icon.getStyle().set("color", "#F5A623"); // Warm golden yellow

        var title = new H1("Café Sunshine");
        title.addClassNames(
                LumoUtility.FontSize.XXLARGE,
                LumoUtility.Margin.NONE,
                LumoUtility.Margin.Top.SMALL
        );

        var subtitle = new Paragraph("Bakery Order Management");
        subtitle.addClassNames(
                LumoUtility.FontSize.MEDIUM,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.Margin.NONE
        );

        branding.add(icon, title, subtitle);
        return branding;
    }

    private LoginForm createLoginForm() {
        var form = new LoginForm();
        form.setAction("login");
        form.setForgotPasswordButtonVisible(false);

        // Customize i18n
        var i18n = LoginI18n.createDefault();
        i18n.getForm().setUsername("Email");
        i18n.getForm().setTitle("");
        i18n.getErrorMessage().setTitle("Login failed");
        i18n.getErrorMessage().setMessage("Please check your email and password and try again.");
        form.setI18n(i18n);

        return form;
    }

    private Div createDivider() {
        var divider = new Div();
        divider.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.Width.FULL,
                LumoUtility.Margin.Vertical.MEDIUM
        );

        var line1 = new Div();
        line1.addClassNames(LumoUtility.Flex.GROW);
        line1.getStyle().set("height", "1px");
        line1.getStyle().set("background", "var(--lumo-contrast-20pct)");

        var text = new Paragraph("or");
        text.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.NONE);

        var line2 = new Div();
        line2.addClassNames(LumoUtility.Flex.GROW);
        line2.getStyle().set("height", "1px");
        line2.getStyle().set("background", "var(--lumo-contrast-20pct)");

        divider.add(line1, text, line2);
        return divider;
    }

    private Button createPasskeyButton() {
        var passkeyIcon = new Icon(VaadinIcon.KEY);
        var passkeyButton = new Button("Sign in with Passkey (Coming Soon)", passkeyIcon);
        passkeyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        passkeyButton.setWidthFull();
        passkeyButton.setEnabled(false);
        passkeyButton.getElement().setAttribute("title",
                "Passkey authentication will be available in a future update");

        return passkeyButton;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
