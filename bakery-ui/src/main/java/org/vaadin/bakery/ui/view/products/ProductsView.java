package org.vaadin.bakery.ui.view.products;

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
import org.vaadin.bakery.ui.component.ViewHeader;
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
    private final Grid<ProductSummary> grid;
    private final TextField searchField;
    private final boolean isAdmin;
    private final NumberFormat currencyFormat;

    private List<ProductSummary> allProducts;

    public ProductsView(ProductService productService, CurrentUserService currentUserService) {
        this.productService = productService;
        this.isAdmin = currentUserService.isAdmin();

        // Component initializations
        addClassName("products-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

        searchField = new TextField();
        searchField.setPlaceholder("Search products...");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> filterGrid(e.getValue()));
        searchField.setWidth("300px");

        var header = new ViewHeader("Products")
                .withFilters(searchField);
        if (isAdmin) {
            header.withAction("New product", () -> openDialog(new ProductSummary()));
        }

        var gridContainer = new Div();
        gridContainer.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.BoxSizing.BORDER);
        gridContainer.setSizeFull();

        grid = new Grid<>(ProductSummary.class, false);
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

        // Layout assembly
        gridContainer.add(grid);
        add(header, gridContainer);
        setFlexGrow(1, gridContainer);

        // Data loading
        refreshGrid();
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
            image.addClassNames(LumoUtility.BorderRadius.SMALL);
            image.getStyle().set("object-fit", "cover");
            return image;
        } else {
            var placeholder = new Image("images/placeholder-product.png", "No image");
            placeholder.setWidth("40px");
            placeholder.setHeight("40px");
            placeholder.addClassNames(
                    LumoUtility.BorderRadius.SMALL,
                    LumoUtility.Background.CONTRAST_10
            );
            placeholder.getStyle().set("object-fit", "cover");
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
