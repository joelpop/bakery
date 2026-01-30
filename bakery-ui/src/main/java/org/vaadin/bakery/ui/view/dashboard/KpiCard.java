package org.vaadin.bakery.ui.view.dashboard;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * KPI card component for the dashboard.
 */
public class KpiCard extends Div {

    private final Span valueSpan;
    private final Span subtitleSpan;
    private final Div deltaContainer;

    public KpiCard(String title, VaadinIcon icon) {
        addClassName("kpi-card");
        getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("box-shadow", "var(--lumo-box-shadow-s)")
                .set("padding", "var(--lumo-space-m)");

        addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL
        );

        // Header with icon and title
        var header = new Div();
        header.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.SMALL
        );

        var iconComponent = new Icon(icon);
        iconComponent.addClassNames(LumoUtility.TextColor.PRIMARY);
        iconComponent.setSize("20px");

        var titleSpan = new Span(title);
        titleSpan.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY,
                LumoUtility.FontWeight.SEMIBOLD
        );

        header.add(iconComponent, titleSpan);
        add(header);

        // Value
        valueSpan = new Span("0");
        valueSpan.addClassNames(
                LumoUtility.FontSize.XXLARGE,
                LumoUtility.FontWeight.BOLD
        );
        add(valueSpan);

        // Subtitle
        subtitleSpan = new Span();
        subtitleSpan.addClassNames(
                LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY
        );
        add(subtitleSpan);

        // Delta container (for comparison metrics)
        deltaContainer = new Div();
        deltaContainer.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.XSMALL,
                LumoUtility.FontSize.XSMALL
        );
        add(deltaContainer);
    }

    public void setValue(long value) {
        valueSpan.setText(String.valueOf(value));
    }

    public void setValue(String value) {
        valueSpan.setText(value);
    }

    public void setSubtitle(String text) {
        subtitleSpan.setText(text);
    }

    public void addDelta(String label, double percentChange) {
        var deltaLine = new Span();
        deltaLine.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.XSMALL, LumoUtility.AlignItems.CENTER);

        var arrow = new Span();
        if (percentChange > 0) {
            arrow.setText("\u25B2"); // Up arrow
            arrow.addClassNames(LumoUtility.TextColor.SUCCESS);
        } else if (percentChange < 0) {
            arrow.setText("\u25BC"); // Down arrow
            arrow.addClassNames(LumoUtility.TextColor.ERROR);
        } else {
            arrow.setText("\u2014"); // Dash
            arrow.addClassNames(LumoUtility.TextColor.SECONDARY);
        }

        var percentText = new Span(String.format("%.1f%% %s", Math.abs(percentChange), label));
        percentText.addClassNames(LumoUtility.TextColor.SECONDARY);

        deltaLine.add(arrow, percentText);
        deltaContainer.add(deltaLine);
    }

    public void clearDeltas() {
        deltaContainer.removeAll();
    }
}
