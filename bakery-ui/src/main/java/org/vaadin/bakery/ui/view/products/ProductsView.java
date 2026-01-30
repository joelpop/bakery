package org.vaadin.bakery.ui.view.products;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.vaadin.bakery.service.CurrentUserService;
import org.vaadin.bakery.service.ProductService;
import org.vaadin.bakery.uimodel.data.ProductSummary;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.io.ByteArrayInputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Product catalog management view.
 * Admin can create, edit, delete products.
 * Baker can view products (read-only).
 */
@Route("products")
@PageTitle("Products")
@Menu(order = 2, icon = LineAwesomeIconUrl.BIRTHDAY_CAKE_SOLID)
@RolesAllowed({"ADMIN", "BAKER"})
public class ProductsView extends VerticalLayout {

    private final ProductService productService;
    private final CurrentUserService currentUserService;
    private final Grid<ProductSummary> grid;
    private final TextField searchField;
    private final boolean isAdmin;

    private List<ProductSummary> allProducts;
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    public ProductsView(ProductService productService, CurrentUserService currentUserService) {
        this.productService = productService;
        this.currentUserService = currentUserService;
        this.isAdmin = currentUserService.isAdmin();

        addClassName("products-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Header with title, search, and add button
        searchField = createSearchField();
        var header = createHeader();

        // Grid container with padding
        var gridContainer = new Div();
        gridContainer.addClassNames(LumoUtility.Padding.MEDIUM);
        gridContainer.setSizeFull();

        grid = createGrid();
        gridContainer.add(grid);

        add(header, gridContainer);
        setFlexGrow(1, gridContainer);
        refreshGrid();
    }

    private TextField createSearchField() {
        var field = new TextField();
        field.setPlaceholder("Search products...");
        field.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        field.setValueChangeMode(ValueChangeMode.LAZY);
        field.addValueChangeListener(e -> filterGrid(e.getValue()));
        field.setWidth("300px");
        return field;
    }

    private Div createHeader() {
        var header = new Div();
        header.addClassName("view-header");

        var title = new Span("Products");
        title.addClassNames(
                LumoUtility.FontSize.XLARGE,
                LumoUtility.FontWeight.SEMIBOLD
        );

        var actions = new Div();
        actions.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.MEDIUM
        );

        actions.add(searchField);

        if (isAdmin) {
            var addButton = new Button("New product", new Icon(VaadinIcon.PLUS));
            addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            addButton.addClickListener(e -> openDialog(new ProductSummary()));
            actions.add(addButton);
        }

        header.add(title, actions);
        return header;
    }

    private Grid<ProductSummary> createGrid() {
        var grid = new Grid<>(ProductSummary.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        grid.addComponentColumn(this::createProductImage)
                .setHeader("Image")
                .setFlexGrow(0)
                .setAutoWidth(true);

        grid.addColumn(ProductSummary::getName)
                .setHeader("Name")
                .setSortable(true)
                .setFlexGrow(2);

        grid.addColumn(ProductSummary::getSize)
                .setHeader("Size")
                .setSortable(true)
                .setFlexGrow(1);

        grid.addColumn(product -> currencyFormat.format(product.getPrice()))
                .setHeader("Price")
                .setSortable(true)
                .setFlexGrow(0)
                .setAutoWidth(true);

        grid.addComponentColumn(product -> {
            var badge = new Span(product.isAvailable() ? "Available" : "Unavailable");
            badge.getElement().getThemeList().add("badge " + (product.isAvailable() ? "success" : "error"));
            return badge;
        }).setHeader("Status").setFlexGrow(0).setAutoWidth(true);

        if (isAdmin) {
            grid.addItemClickListener(event -> openDialog(event.getItem()));
        }

        return grid;
    }

    private Image createProductImage(ProductSummary product) {
        if (product.getPhoto() != null && product.getPhoto().length > 0) {
            var resource = new StreamResource(
                    "product-" + product.getId(),
                    () -> new ByteArrayInputStream(product.getPhoto())
            );
            var image = new Image(resource, product.getName());
            image.setWidth("40px");
            image.setHeight("40px");
            image.getStyle().set("object-fit", "cover");
            image.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
            return image;
        } else {
            var placeholder = new Image("images/placeholder-product.png", "No image");
            placeholder.setWidth("40px");
            placeholder.setHeight("40px");
            placeholder.getStyle().set("object-fit", "cover");
            placeholder.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
            placeholder.getStyle().set("background", "var(--lumo-contrast-10pct)");
            return placeholder;
        }
    }

    private void openDialog(ProductSummary product) {
        if (!isAdmin) return;

        var dialog = new ProductDialog(product, productService);
        dialog.addSaveListener(e -> refreshGrid());
        dialog.addDeleteListener(e -> refreshGrid());
        dialog.open();
    }

    private void refreshGrid() {
        allProducts = productService.list();
        filterGrid(searchField.getValue());
    }

    private void filterGrid(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            grid.setItems(allProducts);
        } else {
            var lowerSearch = searchTerm.toLowerCase();
            grid.setItems(allProducts.stream()
                    .filter(p -> p.getName().toLowerCase().contains(lowerSearch) ||
                            (p.getDescription() != null && p.getDescription().toLowerCase().contains(lowerSearch)))
                    .toList());
        }
    }
}
