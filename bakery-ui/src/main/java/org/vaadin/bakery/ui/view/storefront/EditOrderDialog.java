package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.service.CustomerService;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.OrderService;
import org.vaadin.bakery.service.UserLocationService;
import org.vaadin.bakery.uimodel.data.CustomerSummary;
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
 * Dialog for creating or editing an order.
 */
public class EditOrderDialog extends Dialog {

    private final OrderService orderService;
    private final LocationService locationService;
    private final CustomerService customerService;
    private final UserLocationService userLocationService;

    private final List<OrderItemDetail> orderItems = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    // Customer state tracking
    private CustomerSummary selectedCustomer;
    private boolean isExistingCustomer = false;
    private String customPhoneNumber; // Stores formatted phone when entering new customer

    // Order fields
    private final ComboBox<CustomerSummary> customerPhoneComboBox = new ComboBox<>("Phone Number");
    private final TextField customerNameField = new TextField("Customer Name");
    private final ComboBox<LocationSummary> locationComboBox = new ComboBox<>(); // In dialog header
    private final DatePicker dueDatePicker = new DatePicker("Pickup Date");
    private final TimePicker dueTimePicker = new TimePicker("Pickup Time");
    private final TextArea additionalDetailsField = new TextArea("Additional Details");

    // Item entry fields
    private final ComboBox<ProductSelect> productComboBox = new ComboBox<>("Product");
    private final IntegerField quantityField = new IntegerField("Qty");
    private final TextField itemDetailsField = new TextField("Notes");
    private final Button addUpdateButton = new Button();
    private final Grid<OrderItemDetail> itemsGrid = new Grid<>();
    private OrderItemDetail selectedItem = null;

    // Totals section
    private final Span subtotalValue = new Span("$0.00");
    private final RadioButtonGroup<String> discountTypeGroup = new RadioButtonGroup<>();
    private final NumberField discountField = new NumberField();
    private final Span discountValue = new Span();
    private final Span totalValue = new Span("$0.00");

    public EditOrderDialog(OrderService orderService, LocationService locationService,
                           CustomerService customerService, UserLocationService userLocationService) {
        this.orderService = orderService;
        this.locationService = locationService;
        this.customerService = customerService;
        this.userLocationService = userLocationService;

        setCloseOnOutsideClick(false);
        setWidth("700px");
        setMaxWidth("95vw");

        createHeader();
        add(createContent());

        // Footer buttons
        var cancelButton = new Button("Cancel", e -> {
            fireEvent(new CancelClickEvent(this));
            close();
        });
        var saveButton = new Button("Save", e -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        getFooter().add(cancelButton, saveButton);

        loadData();

        // Focus phone field when dialog opens
        addOpenedChangeListener(e -> {
            if (e.isOpened()) {
                customerPhoneComboBox.focus();
            }
        });
    }

    // ========== Event Classes ==========

    public static class SaveClickEvent extends ComponentEvent<EditOrderDialog> {
        private final OrderDetail order;

        public SaveClickEvent(EditOrderDialog source, OrderDetail order) {
            super(source, false);
            this.order = order;
        }

        public OrderDetail getOrder() {
            return order;
        }

        public boolean isNewCustomerCreated() {
            return order != null && order.isNewCustomerCreated();
        }
    }

    public static class CancelClickEvent extends ComponentEvent<EditOrderDialog> {
        public CancelClickEvent(EditOrderDialog source) {
            super(source, false);
        }
    }

    // ========== Event Registration ==========

    public Registration addSaveClickListener(ComponentEventListener<SaveClickEvent> listener) {
        return addListener(SaveClickEvent.class, listener);
    }

    public Registration addCancelClickListener(ComponentEventListener<CancelClickEvent> listener) {
        return addListener(CancelClickEvent.class, listener);
    }

    // ========== UI Creation ==========

    private void createHeader() {
        // Title
        var title = new Span("New Order");
        title.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD);

        // Location selector styled for header
        locationComboBox.setPlaceholder("Pickup location...");
        locationComboBox.setItemLabelGenerator(LocationSummary::getName);
        locationComboBox.setWidth("180px");
        locationComboBox.addClassNames(LumoUtility.Margin.Left.AUTO);

        // Header layout
        var header = new HorizontalLayout(title, locationComboBox);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassNames(LumoUtility.Gap.MEDIUM);

        getHeader().add(header);
    }

    private VerticalLayout createContent() {
        var content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);

        // Order details form
        var form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );

        // Phone ComboBox with autofill popup
        customerPhoneComboBox.setRequired(true);
        customerPhoneComboBox.setAllowCustomValue(true);
        // Show just phone number in the field when selected
        customerPhoneComboBox.setItemLabelGenerator(CustomerSummary::getPhoneNumber);
        // Show phone + name in the dropdown list
        customerPhoneComboBox.setRenderer(LitRenderer.<CustomerSummary>of(
                "${item.phone} - ${item.name}")
                .withProperty("phone", CustomerSummary::getPhoneNumber)
                .withProperty("name", CustomerSummary::getName));
        customerPhoneComboBox.addCustomValueSetListener(e -> handleNewPhoneNumber(e.getDetail()));
        customerPhoneComboBox.addValueChangeListener(e -> handleCustomerSelection(e.getValue()));
        configurePhoneComboBoxFiltering();

        // Customer name field - read-only until phone entered with new number
        customerNameField.setRequired(true);
        customerNameField.setReadOnly(true);

        dueDatePicker.setRequired(true);
        dueDatePicker.setValue(LocalDate.now());
        dueDatePicker.setMin(LocalDate.now());
        dueTimePicker.setRequired(true);
        dueTimePicker.setValue(LocalTime.of(12, 0));
        dueTimePicker.setStep(Duration.ofMinutes(15));

        form.add(customerPhoneComboBox, customerNameField);
        form.add(dueDatePicker, dueTimePicker);

        content.add(form);
        content.add(new Hr());

        // Items section
        content.add(createItemsSection());

        // Additional details at the end
        additionalDetailsField.setWidthFull();
        content.add(additionalDetailsField);

        return content;
    }

    private Div createItemsSection() {
        var section = new Div();
        section.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL,
                LumoUtility.Width.FULL
        );

        // Add item - first row: Product, Qty, Add button
        productComboBox.setItemLabelGenerator(ProductSelect::getDisplayName);
        // Show price in dropdown list
        productComboBox.setRenderer(LitRenderer.<ProductSelect>of(
                "<span>${item.name} <small>(${item.size})</small> - <strong>${item.price}</strong></span>")
                .withProperty("name", ProductSelect::getName)
                .withProperty("size", ProductSelect::getSize)
                .withProperty("price", p -> currencyFormat.format(p.getPrice())));

        quantityField.setValue(1);
        quantityField.setMin(1);
        quantityField.setMax(99);
        quantityField.setStepButtonsVisible(true);
        quantityField.setWidth("120px");

        addUpdateButton.setIcon(new Icon(VaadinIcon.PLUS));
        addUpdateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addUpdateButton.addClickListener(e -> addOrUpdateItem());

        var addItemRow1 = new HorizontalLayout(productComboBox, quantityField, addUpdateButton);
        addItemRow1.setWidthFull();
        addItemRow1.setAlignItems(FlexComponent.Alignment.END);
        addItemRow1.setFlexGrow(1, productComboBox);

        // Add item - second row: Notes (full width)
        itemDetailsField.setPlaceholder("Special instructions for this item");
        itemDetailsField.setWidthFull();

        // Wrap item entry in tight container
        var addItemBlock = new VerticalLayout(addItemRow1, itemDetailsField);
        addItemBlock.setPadding(false);
        addItemBlock.setSpacing(false);
        addItemBlock.setWidthFull();
        section.add(addItemBlock);

        // Items grid
        configureItemsGrid();
        itemsGrid.setMaxHeight("200px");
        section.add(itemsGrid);

        // Totals section - compact grid layout
        var totalsSection = new Div();
        totalsSection.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "auto auto")
                .set("gap", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("justify-content", "end")
                .set("align-items", "center")
                .set("margin-top", "var(--lumo-space-s)");

        // Subtotal row
        var subtotalLabel = new Span("Subtotal:");
        subtotalLabel.addClassNames(LumoUtility.TextColor.SECONDARY);
        subtotalValue.addClassNames(LumoUtility.TextColor.SECONDARY);
        subtotalValue.getStyle().set("font-variant-numeric", "tabular-nums").set("text-align", "right");
        totalsSection.add(subtotalLabel, subtotalValue);

        // Discount row - label with inline input
        var discountLabel = new Span("Discount:");
        var discountInput = new HorizontalLayout();
        discountInput.setSpacing(false);
        discountInput.setAlignItems(FlexComponent.Alignment.CENTER);
        discountInput.addClassNames(LumoUtility.Gap.XSMALL);

        discountTypeGroup.setItems("%", "$");
        discountTypeGroup.setValue("%");
        discountTypeGroup.addValueChangeListener(e -> updateTotal());

        discountField.setMin(0);
        discountField.setWidth("80px");
        discountField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_RIGHT);
        discountField.addValueChangeListener(e -> updateTotal());

        discountValue.addClassNames(LumoUtility.TextColor.SECONDARY);
        discountValue.getStyle().set("font-variant-numeric", "tabular-nums").set("min-width", "70px").set("text-align", "right");

        discountInput.add(discountTypeGroup, discountField, discountValue);
        totalsSection.add(discountLabel, discountInput);

        // Total row
        var totalLabel = new Span("Total:");
        totalLabel.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        totalValue.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        totalValue.getStyle().set("font-variant-numeric", "tabular-nums").set("text-align", "right");
        totalsSection.add(totalLabel, totalValue);

        section.add(totalsSection);

        return section;
    }

    private void configureItemsGrid() {
        itemsGrid.setAllRowsVisible(true);

        // Enable single selection for editing
        itemsGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        itemsGrid.addSelectionListener(e -> {
            var selected = e.getFirstSelectedItem().orElse(null);
            if (selected != null) {
                enterEditMode(selected);
            } else {
                exitEditMode();
            }
        });

        // Product column with two-line display: name (size) and notes
        itemsGrid.addComponentColumn(this::createProductCell)
                .setHeader("Product")
                .setFlexGrow(2);

        itemsGrid.addColumn(OrderItemDetail::getQuantity)
                .setHeader("Qty")
                .setPartNameGenerator(item -> "numeric")
                .setTextAlign(ColumnTextAlign.END)
                .setFlexGrow(0)
                .setWidth("60px");

        itemsGrid.addColumn(item -> currencyFormat.format(item.getLineTotal()))
                .setHeader("Total")
                .setPartNameGenerator(item -> "numeric")
                .setTextAlign(ColumnTextAlign.END)
                .setFlexGrow(0)
                .setWidth("90px");

        itemsGrid.addComponentColumn(item -> {
            var removeButton = new Button(new Icon(VaadinIcon.TRASH));
            removeButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            removeButton.addClickListener(e -> removeItem(item));
            return removeButton;
        }).setFlexGrow(0).setWidth("50px");
    }

    private Div createProductCell(OrderItemDetail item) {
        var cell = new Div();
        cell.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);

        // First line: Product name (size)
        var productLine = new Span();
        var nameText = item.getProductName();
        if (item.getProductSize() != null && !item.getProductSize().isBlank()) {
            nameText += " (" + item.getProductSize() + ")";
        }
        productLine.setText(nameText);
        cell.add(productLine);

        // Second line: Notes (if any)
        var details = item.getDetails();
        if (details != null && !details.isBlank()) {
            var notesLine = new Span(details);
            notesLine.addClassNames(
                    LumoUtility.FontSize.SMALL,
                    LumoUtility.TextColor.SECONDARY
            );
            cell.add(notesLine);
        }

        return cell;
    }

    // ========== Customer Phone Handling ==========

    private void configurePhoneComboBoxFiltering() {
        // Load all customers and filter by phone digits (ignoring punctuation)
        var allCustomers = customerService.search("");

        // Custom filter that matches phone digits only
        ComboBox.ItemFilter<CustomerSummary> phoneFilter = (customer, filterText) -> {
            if (filterText == null || filterText.isBlank()) {
                return true; // Show all when no filter
            }
            var filterDigits = filterText.replaceAll("\\D", "");
            if (filterDigits.isEmpty()) {
                return true; // Show all if filter has no digits
            }
            var phoneDigits = customer.getPhoneNumber() != null
                    ? customer.getPhoneNumber().replaceAll("\\D", "")
                    : "";
            return phoneDigits.contains(filterDigits);
        };

        customerPhoneComboBox.setItems(phoneFilter, allCustomers);
    }

    private void handleCustomerSelection(CustomerSummary customer) {
        if (customer != null) {
            // Existing customer selected
            selectedCustomer = customer;
            isExistingCustomer = true;
            customPhoneNumber = null; // Clear custom phone since we're using existing customer
            customerNameField.setValue(customer.getName());
            customerNameField.setReadOnly(true);
            // Move focus to next field (due date, since location is already set)
            dueDatePicker.focus();
        } else {
            // Selection cleared - reset to allow new entry
            selectedCustomer = null;
            isExistingCustomer = false;
            customPhoneNumber = null;
            customerNameField.clear();
            customerNameField.setReadOnly(true); // Back to read-only until phone entered
        }
    }

    private void handleNewPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }
        // Format the phone number using location defaults
        customPhoneNumber = formatPhoneNumber(phoneNumber);

        // Update the displayed value in the ComboBox input field
        customerPhoneComboBox.getElement()
                .executeJs("this.inputElement.value = $0", customPhoneNumber);

        // New phone number entered - enable name field for entry
        selectedCustomer = null;
        isExistingCustomer = false;
        customerNameField.setReadOnly(false);
        customerNameField.clear();
        customerNameField.focus();
    }

    /**
     * Format phone number using location defaults for country code and area code.
     * - If only 7 digits, prepends location's default area code
     * - If no country code, prepends location's default country code
     * - Formats as: +1 (212) 555-1234
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }

        // Extract digits only
        var digits = phone.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return phone;
        }

        // Get location defaults
        var location = locationComboBox.getValue();
        var defaultCountryCode = location != null && location.getDefaultCountryCode() != null
                ? location.getDefaultCountryCode().replaceAll("\\D", "")
                : "1"; // Default to US
        var defaultAreaCode = location != null && location.getDefaultAreaCode() != null
                ? location.getDefaultAreaCode().replaceAll("\\D", "")
                : "";

        // Check if number starts with country code
        var hasCountryCode = digits.length() > 10 ||
                (digits.length() == 11 && digits.startsWith("1"));

        // If only 7 digits and we have an area code, prepend it
        if (digits.length() == 7 && !defaultAreaCode.isEmpty()) {
            digits = defaultAreaCode + digits;
        }

        // If 10 digits (no country code), prepend country code
        if (digits.length() == 10 && !hasCountryCode) {
            digits = defaultCountryCode + digits;
        }

        // Format the phone number
        if (digits.length() == 11) {
            // Format as: +X (XXX) XXX-XXXX
            return String.format("+%s (%s) %s-%s",
                    digits.substring(0, 1),
                    digits.substring(1, 4),
                    digits.substring(4, 7),
                    digits.substring(7, 11));
        } else if (digits.length() == 10) {
            // Format as: (XXX) XXX-XXXX
            return String.format("(%s) %s-%s",
                    digits.substring(0, 3),
                    digits.substring(3, 6),
                    digits.substring(6, 10));
        }

        // Return original if we can't format it
        return phone;
    }

    private String getCustomerPhone() {
        // Get phone number from either selected customer or custom value
        var selected = customerPhoneComboBox.getValue();
        if (selected != null) {
            return selected.getPhoneNumber();
        }
        // Return formatted custom phone number if available
        if (customPhoneNumber != null && !customPhoneNumber.isBlank()) {
            return customPhoneNumber;
        }
        // Fall back to input element value
        var inputValue = customerPhoneComboBox.getElement().getProperty("_inputElementValue");
        return inputValue != null ? inputValue : "";
    }

    // ========== Data Operations ==========

    private void loadData() {
        var locations = locationService.listActive();
        locationComboBox.setItems(locations);

        // Pre-select from user's current location, or first location if only one
        var currentLocation = userLocationService != null ? userLocationService.getCurrentLocation() : null;
        if (currentLocation != null) {
            locations.stream()
                    .filter(loc -> loc.getId().equals(currentLocation.getId()))
                    .findFirst()
                    .ifPresent(locationComboBox::setValue);
        } else if (locations.size() == 1) {
            locationComboBox.setValue(locations.getFirst());
        }
    }

    public void setAvailableProducts(List<ProductSelect> products) {
        productComboBox.setItems(products);
    }

    private void enterEditMode(OrderItemDetail item) {
        selectedItem = item;

        // Find the matching product for the ComboBox
        productComboBox.getListDataView().getItems()
                .filter(p -> p.getId().equals(item.getProductId()))
                .findFirst()
                .ifPresent(productComboBox::setValue);
        productComboBox.setEnabled(false); // Can't change product when editing

        quantityField.setValue(item.getQuantity());
        itemDetailsField.setValue(item.getDetails() != null ? item.getDetails() : "");

        addUpdateButton.setIcon(new Icon(VaadinIcon.CHECK));
        quantityField.focus();
    }

    private void exitEditMode() {
        selectedItem = null;

        productComboBox.clear();
        productComboBox.setEnabled(true);
        quantityField.setValue(1);
        itemDetailsField.clear();

        addUpdateButton.setIcon(new Icon(VaadinIcon.PLUS));
        productComboBox.focus();
    }

    private void addOrUpdateItem() {
        if (selectedItem != null) {
            updateSelectedItem();
        } else {
            addNewItem();
        }
    }

    private void updateSelectedItem() {
        var quantity = quantityField.getValue();
        if (quantity == null || quantity < 1) {
            Notification.show("Enter a valid quantity", 2000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        var details = itemDetailsField.getValue();
        var detailsNormalized = details == null ? "" : details.trim();

        // Check if another item has same product and notes (for combining)
        var matchingItem = orderItems.stream()
                .filter(i -> i != selectedItem) // Exclude the selected item
                .filter(i -> i.getProductId().equals(selectedItem.getProductId()))
                .filter(i -> {
                    var existingDetails = i.getDetails() == null ? "" : i.getDetails().trim();
                    return existingDetails.equals(detailsNormalized);
                })
                .findFirst();

        if (matchingItem.isPresent()) {
            // Combine: add quantity to matching item, remove selected item
            var target = matchingItem.get();
            target.setQuantity(target.getQuantity() + quantity);
            target.calculateLineTotal();
            orderItems.remove(selectedItem);
        } else {
            // Update selected item
            selectedItem.setQuantity(quantity);
            selectedItem.setDetails(details);
            selectedItem.calculateLineTotal();
        }

        refreshItemsGrid();
        itemsGrid.deselectAll();
        // exitEditMode will be called by selection listener
    }

    private void addNewItem() {
        var product = productComboBox.getValue();
        var quantity = quantityField.getValue();

        if (product == null) {
            Notification.show("Select a product", 2000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        if (quantity == null || quantity < 1) {
            Notification.show("Enter a valid quantity", 2000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        var details = itemDetailsField.getValue();
        var detailsNormalized = details == null ? "" : details.trim();

        // Check for existing item with same product and notes
        var existingItem = orderItems.stream()
                .filter(i -> i.getProductId().equals(product.getId()))
                .filter(i -> {
                    var existingDetails = i.getDetails() == null ? "" : i.getDetails().trim();
                    return existingDetails.equals(detailsNormalized);
                })
                .findFirst();

        if (existingItem.isPresent()) {
            // Combine quantities
            var item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            item.calculateLineTotal();
        } else {
            // Add new item
            var item = new OrderItemDetail();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductSize(product.getSize());
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
            item.setDetails(details);
            item.calculateLineTotal();
            orderItems.add(item);
        }

        refreshItemsGrid();

        productComboBox.clear();
        quantityField.setValue(1);
        itemDetailsField.clear();
        productComboBox.focus();
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
        var subtotal = orderItems.stream()
                .map(OrderItemDetail::getLineTotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        subtotalValue.setText(currencyFormat.format(subtotal));

        var discount = calculateDiscount(subtotal);

        // Show calculated discount amount (always show, even if zero)
        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            discountValue.setText("-" + currencyFormat.format(discount));
        } else {
            discountValue.setText("$0.00");
        }

        // Validate discount
        var discountInput = discountField.getValue();
        if (discountInput != null && discountInput < 0) {
            discountField.setInvalid(true);
            discountField.setErrorMessage("Discount cannot be negative");
        } else if (discount.compareTo(subtotal) > 0) {
            discountField.setInvalid(true);
            discountField.setErrorMessage("Discount exceeds subtotal");
        } else {
            discountField.setInvalid(false);
        }

        var total = subtotal.subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
        totalValue.setText(currencyFormat.format(total));
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal) {
        var discountInput = discountField.getValue();
        if (discountInput == null || discountInput == 0) {
            return BigDecimal.ZERO;
        }

        var value = BigDecimal.valueOf(discountInput);

        if ("%".equals(discountTypeGroup.getValue())) {
            // Percentage discount
            return subtotal.multiply(value).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        } else {
            // Dollar discount
            return value;
        }
    }

    // ========== Validation and Save ==========

    private boolean validate() {
        var valid = true;
        var requiredMessage = "Required";

        var phoneValue = getCustomerPhone();
        if (phoneValue == null || phoneValue.isBlank()) {
            customerPhoneComboBox.setInvalid(true);
            customerPhoneComboBox.setErrorMessage(requiredMessage);
            valid = false;
        } else {
            customerPhoneComboBox.setInvalid(false);
        }

        if (customerNameField.getValue() == null || customerNameField.getValue().isBlank()) {
            customerNameField.setInvalid(true);
            customerNameField.setErrorMessage(requiredMessage);
            valid = false;
        } else {
            customerNameField.setInvalid(false);
        }

        if (locationComboBox.getValue() == null) {
            locationComboBox.setInvalid(true);
            Notification.show("Select a pickup location", 2000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            valid = false;
        } else {
            locationComboBox.setInvalid(false);
        }

        if (dueDatePicker.getValue() == null) {
            dueDatePicker.setInvalid(true);
            dueDatePicker.setErrorMessage(requiredMessage);
            valid = false;
        } else {
            dueDatePicker.setInvalid(false);
        }

        if (dueTimePicker.getValue() == null) {
            dueTimePicker.setInvalid(true);
            dueTimePicker.setErrorMessage(requiredMessage);
            valid = false;
        } else {
            dueTimePicker.setInvalid(false);
        }

        if (orderItems.isEmpty()) {
            Notification.show("Add at least one item", 2000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            valid = false;
        }

        return valid;
    }

    private void save() {
        if (!validate()) {
            return;
        }

        try {
            var order = new OrderDetail();
            order.setStatus(OrderStatus.NEW);
            order.setCustomerName(customerNameField.getValue());
            order.setCustomerPhone(getCustomerPhone());
            // Set customer ID if existing customer was selected
            if (selectedCustomer != null) {
                order.setCustomerId(selectedCustomer.getId());
            }
            order.setLocationId(locationComboBox.getValue().getId());
            order.setLocationName(locationComboBox.getValue().getName());
            order.setDueDate(dueDatePicker.getValue());
            order.setDueTime(dueTimePicker.getValue());
            order.setAdditionalDetails(additionalDetailsField.getValue());
            order.setItems(new ArrayList<>(orderItems));
            // Calculate discount based on subtotal
            var subtotal = orderItems.stream()
                    .map(OrderItemDetail::getLineTotal)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setDiscount(calculateDiscount(subtotal));
            order.calculateTotal();
            order.setPaid(false);

            var savedOrder = orderService.create(order);

            Notification.show("Order created", 2000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            fireEvent(new SaveClickEvent(this, savedOrder));
            close();
        } catch (Exception e) {
            Notification.show("Failed: " + e.getMessage(), 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
