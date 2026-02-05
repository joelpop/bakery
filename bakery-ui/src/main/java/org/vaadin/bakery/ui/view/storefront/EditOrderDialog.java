package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.service.CustomerService;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.OrderService;
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

    private final List<OrderItemDetail> orderItems = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

    // Customer state tracking
    private CustomerSummary selectedCustomer;
    private boolean isExistingCustomer = false;
    private String customPhoneNumber; // Stores formatted phone when entering new customer

    // Order fields - phone first for quick customer lookup
    private final ComboBox<CustomerSummary> customerPhoneComboBox = new ComboBox<>("Phone Number");
    private final TextField customerNameField = new TextField("Customer Name");
    private final ComboBox<LocationSummary> locationComboBox = new ComboBox<>("Pickup Location");
    private final DatePicker dueDatePicker = new DatePicker("Due Date");
    private final TimePicker dueTimePicker = new TimePicker("Due Time");
    private final TextArea additionalDetailsField = new TextArea("Additional Details");

    // Item entry fields
    private final ComboBox<ProductSelect> productComboBox = new ComboBox<>("Product");
    private final IntegerField quantityField = new IntegerField("Qty");
    private final TextField itemDetailsField = new TextField("Notes");
    private final Grid<OrderItemDetail> itemsGrid = new Grid<>();
    private final Span totalLabel = new Span("$0.00");

    public EditOrderDialog(OrderService orderService, LocationService locationService, CustomerService customerService) {
        this.orderService = orderService;
        this.locationService = locationService;
        this.customerService = customerService;

        setHeaderTitle("New Order");
        setCloseOnOutsideClick(false);
        setWidth("700px");
        setMaxWidth("95vw");

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

        locationComboBox.setRequired(true);
        locationComboBox.setItemLabelGenerator(LocationSummary::getName);
        dueDatePicker.setRequired(true);
        dueDatePicker.setValue(LocalDate.now());
        dueDatePicker.setMin(LocalDate.now());
        dueTimePicker.setRequired(true);
        dueTimePicker.setValue(LocalTime.of(12, 0));
        dueTimePicker.setStep(Duration.ofMinutes(15));

        form.add(customerPhoneComboBox, customerNameField);
        form.add(locationComboBox, 2);
        form.add(dueDatePicker, dueTimePicker);
        form.add(additionalDetailsField, 2);

        content.add(form);
        content.add(new Hr());

        // Items section
        content.add(createItemsSection());

        return content;
    }

    private Div createItemsSection() {
        var section = new Div();
        section.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL
        );

        // Add item row
        productComboBox.setItemLabelGenerator(ProductSelect::getDisplayName);
        productComboBox.setWidth("200px");

        quantityField.setValue(1);
        quantityField.setMin(1);
        quantityField.setMax(99);
        quantityField.setStepButtonsVisible(true);
        quantityField.setWidth("80px");

        itemDetailsField.setPlaceholder("Special instructions");
        itemDetailsField.setWidth("150px");

        var addButton = new Button(new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(e -> addItem());

        var addItemRow = new HorizontalLayout(productComboBox, quantityField, itemDetailsField, addButton);
        addItemRow.setAlignItems(FlexComponent.Alignment.END);
        addItemRow.setFlexGrow(1, productComboBox);
        section.add(addItemRow);

        // Items grid
        configureItemsGrid();
        itemsGrid.setMaxHeight("200px");
        section.add(itemsGrid);

        // Total
        var totalRow = new HorizontalLayout();
        totalRow.setWidthFull();
        totalRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        totalRow.setAlignItems(FlexComponent.Alignment.CENTER);

        var totalText = new Span("Total: ");
        totalText.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        totalLabel.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        totalRow.add(totalText, totalLabel);
        section.add(totalRow);

        return section;
    }

    private void configureItemsGrid() {
        itemsGrid.setAllRowsVisible(true);

        itemsGrid.addColumn(OrderItemDetail::getProductName)
                .setHeader("Product")
                .setFlexGrow(2);

        itemsGrid.addColumn(OrderItemDetail::getQuantity)
                .setHeader("Qty")
                .setFlexGrow(0)
                .setWidth("50px");

        itemsGrid.addColumn(item -> currencyFormat.format(item.getLineTotal()))
                .setHeader("Total")
                .setFlexGrow(0)
                .setWidth("80px");

        itemsGrid.addComponentColumn(item -> {
            var removeButton = new Button(new Icon(VaadinIcon.TRASH));
            removeButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            removeButton.addClickListener(e -> removeItem(item));
            return removeButton;
        }).setFlexGrow(0).setWidth("50px");
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
            // Move focus to next field (location)
            locationComboBox.focus();
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
        if (locations.size() == 1) {
            locationComboBox.setValue(locations.getFirst());
        }
    }

    public void setAvailableProducts(List<ProductSelect> products) {
        productComboBox.setItems(products);
    }

    private void addItem() {
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
            locationComboBox.setErrorMessage(requiredMessage);
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
