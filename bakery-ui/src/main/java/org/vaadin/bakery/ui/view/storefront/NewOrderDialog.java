package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.OrderService;
import org.vaadin.bakery.uimodel.data.LocationSummary;
import org.vaadin.bakery.uimodel.data.OrderDetail;
import org.vaadin.bakery.uimodel.data.OrderItemDetail;
import org.vaadin.bakery.uimodel.data.ProductSelect;
import org.vaadin.bakery.uimodel.type.OrderStatus;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dialog for creating a new order.
 * Step 1: Customer info, location, date/time
 * Step 2: Add items
 */
public class NewOrderDialog extends Dialog {

    private final OrderService orderService;
    private final LocationService locationService;
    private final Runnable onSaveCallback;

    private final List<ProductSelect> availableProducts = new ArrayList<>();
    private final List<OrderItemDetail> orderItems = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    // Step 1 fields
    private final TextField customerNameField = new TextField("Customer Name");
    private final TextField customerPhoneField = new TextField("Phone Number");
    private final ComboBox<LocationSummary> locationComboBox = new ComboBox<>("Pickup Location");
    private final DatePicker dueDatePicker = new DatePicker("Due Date");
    private final TimePicker dueTimePicker = new TimePicker("Due Time");
    private final TextArea additionalDetailsField = new TextArea("Additional Details");

    // Step 2 components
    private final ComboBox<ProductSelect> productComboBox = new ComboBox<>("Product");
    private final IntegerField quantityField = new IntegerField("Quantity");
    private final TextField itemDetailsField = new TextField("Item Notes");
    private final Grid<OrderItemDetail> itemsGrid = new Grid<>();
    private final Span totalLabel = new Span("$0.00");

    private int currentStep = 1;
    private final Div step1Content;
    private final Div step2Content;
    private final Button nextButton;
    private final Button backButton;
    private final Button saveButton;
    private final Span stepIndicator;

    public NewOrderDialog(OrderService orderService, LocationService locationService,
                          Runnable onSaveCallback) {
        this.orderService = orderService;
        this.locationService = locationService;
        this.onSaveCallback = onSaveCallback;

        setHeaderTitle("New Order");
        setModal(true);
        setCloseOnOutsideClick(false);
        // Responsive sizing: max-width on desktop, full-screen on mobile via CSS theme
        getElement().getThemeList().add("responsive-dialog");
        setWidth("100%");
        setMaxWidth("700px");
        setHeight("auto");
        setMaxHeight("90vh");

        // Step indicator
        stepIndicator = new Span("Step 1 of 2: Order Details");
        stepIndicator.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        var headerContent = new HorizontalLayout(stepIndicator);
        headerContent.setWidthFull();
        getHeader().add(headerContent);

        // Create step content
        step1Content = createStep1Content();
        step2Content = createStep2Content();
        step2Content.setVisible(false);

        var content = new Div(step1Content, step2Content);
        content.setSizeFull();
        add(content);

        // Footer buttons
        backButton = new Button("Back", e -> goBack());
        backButton.setVisible(false);

        nextButton = new Button("Next", e -> goNext());
        nextButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        saveButton = new Button("Create Order", e -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        saveButton.setVisible(false);

        var cancelButton = new Button("Cancel", e -> close());

        var footer = new HorizontalLayout(cancelButton, new Span(), backButton, nextButton, saveButton);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setSpacing(true);
        getFooter().add(footer);

        loadData();
    }

    private Div createStep1Content() {
        var content = new Div();
        content.setSizeFull();

        var form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );

        customerNameField.setRequired(true);
        customerNameField.setWidthFull();

        customerPhoneField.setWidthFull();
        customerPhoneField.setPlaceholder("(555) 123-4567");

        locationComboBox.setRequired(true);
        locationComboBox.setWidthFull();
        locationComboBox.setItemLabelGenerator(LocationSummary::getName);

        dueDatePicker.setRequired(true);
        dueDatePicker.setValue(LocalDate.now());
        dueDatePicker.setMin(LocalDate.now());

        dueTimePicker.setRequired(true);
        dueTimePicker.setValue(LocalTime.of(12, 0));
        dueTimePicker.setStep(Duration.ofMinutes(15));

        additionalDetailsField.setWidthFull();
        additionalDetailsField.setMinHeight("80px");

        form.add(customerNameField, 2);
        form.add(customerPhoneField, 2);
        form.add(locationComboBox, 2);
        form.add(dueDatePicker, 1);
        form.add(dueTimePicker, 1);
        form.add(additionalDetailsField, 2);

        content.add(form);
        return content;
    }

    private Div createStep2Content() {
        var content = new Div();
        content.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.MEDIUM
        );
        content.setSizeFull();

        // Add item section
        var addItemSection = new Div();
        addItemSection.addClassNames(
                LumoUtility.Background.CONTRAST_5,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Padding.MEDIUM
        );

        var addItemHeader = new H4("Add Items");
        addItemHeader.addClassNames(LumoUtility.Margin.NONE, LumoUtility.Margin.Bottom.SMALL);

        productComboBox.setWidthFull();
        productComboBox.setItemLabelGenerator(ProductSelect::getDisplayName);
        productComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                quantityField.setValue(1);
            }
        });

        quantityField.setValue(1);
        quantityField.setMin(1);
        quantityField.setMax(99);
        quantityField.setStepButtonsVisible(true);
        quantityField.setWidth("100px");

        itemDetailsField.setPlaceholder("Special instructions");
        itemDetailsField.setWidth("200px");

        var addButton = new Button(new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> addItem());

        var addItemRow = new HorizontalLayout(productComboBox, quantityField, itemDetailsField, addButton);
        addItemRow.setWidthFull();
        addItemRow.setAlignItems(FlexComponent.Alignment.END);
        addItemRow.setFlexGrow(1, productComboBox);

        addItemSection.add(addItemHeader, addItemRow);
        content.add(addItemSection);

        // Items grid
        configureItemsGrid();
        var gridContainer = new Div(itemsGrid);
        gridContainer.getStyle().set("flex-grow", "1").set("min-height", "150px");
        content.add(gridContainer);

        // Total section
        var totalSection = new HorizontalLayout();
        totalSection.setWidthFull();
        totalSection.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        totalSection.setAlignItems(FlexComponent.Alignment.CENTER);

        var totalText = new Span("Order Total: ");
        totalText.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        totalLabel.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD);

        totalSection.add(totalText, totalLabel);
        content.add(totalSection);

        return content;
    }

    private void configureItemsGrid() {
        itemsGrid.setAllRowsVisible(true);
        itemsGrid.setMaxHeight("200px");

        itemsGrid.addColumn(OrderItemDetail::getProductName)
                .setHeader("Product")
                .setFlexGrow(2);

        itemsGrid.addColumn(OrderItemDetail::getQuantity)
                .setHeader("Qty")
                .setFlexGrow(0)
                .setWidth("60px");

        itemsGrid.addColumn(item -> currencyFormat.format(item.getUnitPrice()))
                .setHeader("Price")
                .setFlexGrow(0)
                .setWidth("80px");

        itemsGrid.addColumn(item -> currencyFormat.format(item.getLineTotal()))
                .setHeader("Total")
                .setFlexGrow(0)
                .setWidth("80px");

        itemsGrid.addColumn(OrderItemDetail::getDetails)
                .setHeader("Notes")
                .setFlexGrow(1);

        itemsGrid.addComponentColumn(item -> {
            var removeButton = new Button(new Icon(VaadinIcon.TRASH));
            removeButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            removeButton.addClickListener(e -> removeItem(item));
            return removeButton;
        }).setFlexGrow(0).setWidth("60px");
    }

    private void loadData() {
        var locations = locationService.listActive();
        locationComboBox.setItems(locations);
        if (locations.size() == 1) {
            locationComboBox.setValue(locations.get(0));
        }

        // Load products - we need to get them from somewhere
        // For now, we'll need to add ProductService to the dialog
    }

    private void addItem() {
        var product = productComboBox.getValue();
        var quantity = quantityField.getValue();

        if (product == null) {
            Notification.show("Please select a product", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        if (quantity == null || quantity < 1) {
            Notification.show("Please enter a valid quantity", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        var item = new OrderItemDetail();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductSize(product.getSize());
        item.setQuantity(quantity);
        item.setUnitPrice(product.getPrice());
        item.setDetails(itemDetailsField.getValue());
        item.calculateLineTotal();

        orderItems.add(item);
        refreshItemsGrid();

        // Clear fields
        productComboBox.clear();
        quantityField.setValue(1);
        itemDetailsField.clear();
    }

    private void removeItem(OrderItemDetail item) {
        orderItems.remove(item);
        refreshItemsGrid();
    }

    private void refreshItemsGrid() {
        itemsGrid.setItems(orderItems);
        updateTotal();
    }

    private void updateTotal() {
        var total = orderItems.stream()
                .map(OrderItemDetail::getLineTotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalLabel.setText(currencyFormat.format(total));
    }

    private boolean validateStep1() {
        var valid = true;

        if (customerNameField.getValue() == null || customerNameField.getValue().isBlank()) {
            customerNameField.setInvalid(true);
            customerNameField.setErrorMessage("Customer name is required");
            valid = false;
        } else {
            customerNameField.setInvalid(false);
        }

        if (locationComboBox.getValue() == null) {
            locationComboBox.setInvalid(true);
            locationComboBox.setErrorMessage("Location is required");
            valid = false;
        } else {
            locationComboBox.setInvalid(false);
        }

        if (dueDatePicker.getValue() == null) {
            dueDatePicker.setInvalid(true);
            dueDatePicker.setErrorMessage("Due date is required");
            valid = false;
        } else {
            dueDatePicker.setInvalid(false);
        }

        if (dueTimePicker.getValue() == null) {
            dueTimePicker.setInvalid(true);
            dueTimePicker.setErrorMessage("Due time is required");
            valid = false;
        } else {
            dueTimePicker.setInvalid(false);
        }

        return valid;
    }

    private void goNext() {
        if (currentStep == 1 && validateStep1()) {
            currentStep = 2;
            step1Content.setVisible(false);
            step2Content.setVisible(true);
            stepIndicator.setText("Step 2 of 2: Add Items");
            backButton.setVisible(true);
            nextButton.setVisible(false);
            saveButton.setVisible(true);
        }
    }

    private void goBack() {
        if (currentStep == 2) {
            currentStep = 1;
            step2Content.setVisible(false);
            step1Content.setVisible(true);
            stepIndicator.setText("Step 1 of 2: Order Details");
            backButton.setVisible(false);
            nextButton.setVisible(true);
            saveButton.setVisible(false);
        }
    }

    private void save() {
        if (orderItems.isEmpty()) {
            Notification.show("Please add at least one item", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        try {
            var order = new OrderDetail();
            order.setStatus(OrderStatus.NEW);
            order.setCustomerName(customerNameField.getValue());
            order.setCustomerPhone(customerPhoneField.getValue());
            order.setLocationId(locationComboBox.getValue().getId());
            order.setLocationName(locationComboBox.getValue().getName());
            order.setDueDate(dueDatePicker.getValue());
            order.setDueTime(dueTimePicker.getValue());
            order.setAdditionalDetails(additionalDetailsField.getValue());
            order.setItems(new ArrayList<>(orderItems));
            order.calculateTotal();
            order.setPaid(false);

            orderService.create(order);

            Notification.show("Order created successfully", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            close();
        } catch (Exception e) {
            Notification.show("Failed to create order: " + e.getMessage(), 5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Set available products for the order items.
     */
    public void setAvailableProducts(List<ProductSelect> products) {
        availableProducts.clear();
        availableProducts.addAll(products);
        productComboBox.setItems(products);
    }
}
