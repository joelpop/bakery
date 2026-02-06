package org.vaadin.bakery.ui.view.storefront;

import com.vaadin.flow.component.ComponentEffect;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.signals.Signal;
import com.vaadin.signals.local.ListSignal;
import com.vaadin.signals.local.ValueSignal;
import org.vaadin.bakery.service.CustomerService;
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.OrderService;
import org.vaadin.bakery.service.UserLocationService;
import org.vaadin.bakery.ui.event.NonComponent;
import org.vaadin.bakery.ui.event.NonComponentEvent;
import org.vaadin.bakery.ui.event.NonComponentEventSupport;
import org.vaadin.bakery.uimodel.data.CustomerSummary;
import org.vaadin.bakery.uimodel.data.LocationSummary;
import org.vaadin.bakery.uimodel.data.OrderDetail;
import org.vaadin.bakery.uimodel.data.OrderItemDetail;
import org.vaadin.bakery.uimodel.data.ProductSelect;
import org.vaadin.bakery.uimodel.type.OrderStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Dialog for creating or editing an order.
 * Uses delegation to Dialog rather than inheritance to control the public API.
 */
public class EditOrderDialog implements NonComponent {

    private final Dialog dialog;
    private final NonComponentEventSupport<EditOrderDialog> eventSupport;

    private final OrderService orderService;
    private final LocationService locationService;
    private final CustomerService customerService;
    private final UserLocationService userLocationService;

    private final ListSignal<OrderItemDetail> orderItemsListSignal;
    private final NumberFormat currencyFormat;

    // Customer state signals
    private final ValueSignal<CustomerSummary> selectedCustomerSignal;
    private final ValueSignal<String> customPhoneSignal;

    // Order fields
    private final ComboBox<CustomerSummary> customerPhoneComboBox;
    private final TextField customerNameField;
    private final ComboBox<LocationSummary> locationComboBox;
    private final DatePicker dueDatePicker;
    private final TimePicker dueTimePicker;
    private final TextArea additionalDetailsField;

    // Item entry fields
    private final ComboBox<ProductSelect> productComboBox;
    private final IntegerField quantityField;
    private final TextField itemDetailsField;
    private final Button addUpdateButton;
    private final Grid<OrderItemDetail> itemsGrid;

    // Edit mode signal - null means add mode, non-null means editing that item
    private final ValueSignal<OrderItemDetail> editingItemSignal;

    // Discount fields and signals
    private final RadioButtonGroup<DiscountType> discountTypeGroup;
    private final NumberField discountAmountField;
    private final ValueSignal<DiscountType> discountTypeSignal;
    private final ValueSignal<Double> discountAmountSignal;

    public EditOrderDialog(OrderService orderService, LocationService locationService,
                           CustomerService customerService, UserLocationService userLocationService) {
        this.orderService = orderService;
        this.locationService = locationService;
        this.customerService = customerService;
        this.userLocationService = userLocationService;

        // Component initializations
        eventSupport = new NonComponentEventSupport<>();

        currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);

        var titleSpan = new Span("New Order");
        titleSpan.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD);

        locationComboBox = new ComboBox<>();
        locationComboBox.setPlaceholder("Pickup location...");
        locationComboBox.setItemLabelGenerator(LocationSummary::getName);
        locationComboBox.setWidth("180px");
        locationComboBox.addClassNames(LumoUtility.Margin.Left.AUTO);

        var header = new HorizontalLayout(titleSpan, locationComboBox);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassNames(LumoUtility.Gap.MEDIUM);

        var form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );

        customerPhoneComboBox = new ComboBox<>("Phone Number");
        customerPhoneComboBox.setRequired(true);
        customerPhoneComboBox.setAllowCustomValue(true);
        customerPhoneComboBox.setItemLabelGenerator(CustomerSummary::getPhoneNumber);
        customerPhoneComboBox.setRenderer(LitRenderer.<CustomerSummary>of(
                "${item.phone} - ${item.name}")
                .withProperty("phone", CustomerSummary::getPhoneNumber)
                .withProperty("name", CustomerSummary::getName));
        customerPhoneComboBox.addCustomValueSetListener(e -> handleNewPhoneNumber(e.getDetail()));
        customerPhoneComboBox.addValueChangeListener(e -> handleCustomerSelection(e.getValue()));

        customerNameField = new TextField("Customer Name");
        customerNameField.setRequired(true);
        customerNameField.setReadOnly(true);

        dueDatePicker = new DatePicker("Pickup Date");
        dueDatePicker.setRequired(true);
        dueDatePicker.setMin(LocalDate.now());

        dueTimePicker = new TimePicker("Pickup Time");
        dueTimePicker.setRequired(true);
        dueTimePicker.setStep(Duration.ofMinutes(15));

        additionalDetailsField = new TextArea("Additional Details");
        additionalDetailsField.setWidthFull();

        var itemsSection = new Div();
        itemsSection.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL,
                LumoUtility.Width.FULL
        );

        productComboBox = new ComboBox<>("Product");
        productComboBox.setItemLabelGenerator(ProductSelect::getDisplayName);
        productComboBox.setRenderer(LitRenderer.<ProductSelect>of(
                "<span>${item.name} <small>(${item.size})</small> - <strong>${item.price}</strong></span>")
                .withProperty("name", ProductSelect::getName)
                .withProperty("size", ProductSelect::getSize)
                .withProperty("price", p -> currencyFormat.format(p.getPrice())));

        quantityField = new IntegerField("Qty");
        quantityField.setMin(1);
        quantityField.setMax(99);
        quantityField.setStepButtonsVisible(true);
        quantityField.setWidth("120px");

        addUpdateButton = new Button();
        addUpdateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addUpdateButton.addClickListener(e -> addOrUpdateItem());
        addUpdateButton.addClickShortcut(Key.ENTER);

        var addItemRow = new HorizontalLayout(productComboBox, quantityField, addUpdateButton);
        addItemRow.setWidthFull();
        addItemRow.setAlignItems(FlexComponent.Alignment.END);
        addItemRow.setFlexGrow(1, productComboBox);

        itemDetailsField = new TextField("Notes");
        itemDetailsField.setPlaceholder("Special instructions for this item");
        itemDetailsField.setWidthFull();

        var addItemBlock = new VerticalLayout(addItemRow, itemDetailsField);
        addItemBlock.setPadding(false);
        addItemBlock.setSpacing(false);
        addItemBlock.setWidthFull();

        itemsGrid = new Grid<>();
        itemsGrid.setAllRowsVisible(true);
        itemsGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        itemsGrid.setMaxHeight("200px");
        itemsGrid.addComponentColumn(this::createProductCell)
                .setHeader("Product")
                .setFlexGrow(2);
        itemsGrid.addColumn(OrderItemDetail::getQuantity)
                .setHeader("Qty")
                .setPartNameGenerator(item -> "numeric")
                .setTextAlign(ColumnTextAlign.END)
                .setFlexGrow(0)
                .setWidth("60px");
        itemsGrid.addColumn(item -> currencyFormat.format(item.getUnitPrice()))
                .setHeader("Price")
                .setPartNameGenerator(item -> "numeric")
                .setTextAlign(ColumnTextAlign.END)
                .setFlexGrow(0)
                .setWidth("90px");
        itemsGrid.addColumn(item -> currencyFormat.format(item.getLineTotal()))
                .setHeader("Total")
                .setPartNameGenerator(item -> "numeric")
                .setTextAlign(ColumnTextAlign.END)
                .setFlexGrow(0)
                .setWidth("100px");
        itemsGrid.addComponentColumn(item -> {
            var removeButton = new Button(new Icon(VaadinIcon.TRASH));
            removeButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            removeButton.addClickListener(e -> removeItem(item));
            return removeButton;
        }).setFlexGrow(0).setWidth("50px");

        var totalsSection = new Div();
        totalsSection.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "auto auto")
                .set("gap", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("justify-content", "end")
                .set("align-items", "center")
                .set("margin-top", "var(--lumo-space-s)");

        var subtotalLabel = new Span("Subtotal:");
        subtotalLabel.addClassNames(LumoUtility.TextColor.SECONDARY);

        var subtotalValueSpan = new Span();
        subtotalValueSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
        subtotalValueSpan.getStyle().set("font-variant-numeric", "tabular-nums").set("text-align", "right");

        var discountLabel = new Span("Discount:");

        var discountInput = new HorizontalLayout();
        discountInput.setSpacing(false);
        discountInput.setAlignItems(FlexComponent.Alignment.CENTER);
        discountInput.addClassNames(LumoUtility.Gap.XSMALL);

        discountTypeGroup = new RadioButtonGroup<>();
        discountTypeGroup.setItemLabelGenerator(DiscountType::getSymbol);

        discountAmountField = new NumberField();
        discountAmountField.setMin(0);
        discountAmountField.setWidth("80px");
        discountAmountField.setClearButtonVisible(true);
        discountAmountField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_RIGHT);

        var discountValueSpan = new Span();
        discountValueSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
        discountValueSpan.getStyle().set("font-variant-numeric", "tabular-nums").set("min-width", "70px").set("text-align", "right");

        var totalLabel = new Span("Total:");
        totalLabel.addClassNames(LumoUtility.FontWeight.SEMIBOLD);

        var totalValueSpan = new Span();
        totalValueSpan.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        totalValueSpan.getStyle().set("font-variant-numeric", "tabular-nums").set("text-align", "right");

        var content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);

        var cancelButton = new Button("Cancel", e -> {
            fireEvent(new CancelEvent(this));
            close();
        });

        var saveButton = new Button("Save", e -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Signal definitions
        orderItemsListSignal = new ListSignal<>();

        selectedCustomerSignal = new ValueSignal<>(null);

        customPhoneSignal = new ValueSignal<>(null);

        editingItemSignal = new ValueSignal<>(null);

        discountTypeSignal = new ValueSignal<>(DiscountType.PERCENT);

        discountAmountSignal = new ValueSignal<>(0.0);

        Signal<BigDecimal> subtotalValueSignal = Signal.computed(() ->
                orderItemsListSignal.value().stream()
                        .map(ValueSignal::value)
                        .map(OrderItemDetail::getLineTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        Signal<BigDecimal> discountValueSignal = Signal.computed(() -> {
            var sub = subtotalValueSignal.value();
            var amount = discountAmountSignal.value();
            if (amount == null || amount <= 0) {
                return BigDecimal.ZERO;
            }
            var value = BigDecimal.valueOf(amount);
            var discount = DiscountType.PERCENT == discountTypeSignal.value()
                    ? sub.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : value;
            return discount.compareTo(sub) > 0 ? BigDecimal.ZERO : discount;
        });

        Signal<BigDecimal> totalValueSignal = Signal.computed(() -> {
            var sub = subtotalValueSignal.value();
            var disc = discountValueSignal.value();
            var result = sub.subtract(disc);
            return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
        });

        // Signal bindings
        subtotalValueSpan.bindText(subtotalValueSignal.map(currencyFormat::format));

        discountValueSpan.bindText(discountValueSignal.map(d ->
                d.compareTo(BigDecimal.ZERO) > 0 ? "-" + currencyFormat.format(d) : "$0.00"));

        totalValueSpan.bindText(totalValueSignal.map(currencyFormat::format));

        discountTypeGroup.addValueChangeListener(e -> discountTypeSignal.value(e.getValue()));

        discountAmountField.addValueChangeListener(e -> {
            var val = e.getValue();
            discountAmountSignal.value(val != null ? val : 0.0);
        });

        ComponentEffect.effect(discountAmountField, () -> {
            var sub = subtotalValueSignal.value();
            var amount = discountAmountSignal.value();
            if (amount == null || amount == 0) {
                discountAmountField.setInvalid(false);
                return;
            }
            if (amount < 0) {
                discountAmountField.setInvalid(true);
                discountAmountField.setErrorMessage("Discount cannot be negative");
                return;
            }
            var value = BigDecimal.valueOf(amount);
            var rawDiscount = DiscountType.PERCENT == discountTypeSignal.value()
                    ? sub.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : value;
            if (rawDiscount.compareTo(sub) > 0) {
                discountAmountField.setInvalid(true);
                discountAmountField.setErrorMessage("Discount exceeds subtotal");
            } else {
                discountAmountField.setInvalid(false);
            }
        });

        itemsGrid.addSelectionListener(e -> {
            var selected = e.getFirstSelectedItem().orElse(null);
            editingItemSignal.value(selected);
            if (selected != null) {
                quantityField.focus();
            } else {
                productComboBox.focus();
            }
        });

        ComponentEffect.effect(addUpdateButton, () -> {
            var editingItem = editingItemSignal.value();
            if (editingItem != null) {
                productComboBox.getListDataView().getItems()
                        .filter(p -> p.getId().equals(editingItem.getProductId()))
                        .findFirst()
                        .ifPresent(productComboBox::setValue);
                productComboBox.setEnabled(false);
                quantityField.setValue(editingItem.getQuantity());
                itemDetailsField.setValue(editingItem.getDetails() != null ? editingItem.getDetails() : "");
                addUpdateButton.setIcon(new Icon(VaadinIcon.CHECK));
            } else {
                productComboBox.clear();
                productComboBox.setEnabled(true);
                quantityField.setValue(1);
                itemDetailsField.clear();
                addUpdateButton.setIcon(new Icon(VaadinIcon.PLUS));
            }
        });

        // Value settings
        dueDatePicker.setValue(LocalDate.now());

        dueTimePicker.setValue(LocalTime.of(12, 0));

        discountTypeGroup.setItems(DiscountType.values());
        discountTypeGroup.setValue(DiscountType.PERCENT);

        quantityField.setValue(1);

        addUpdateButton.setIcon(new Icon(VaadinIcon.PLUS));

        // Data loading
        configurePhoneComboBoxFiltering();
        loadData();

        // Assemble layout
        form.add(customerPhoneComboBox, customerNameField);
        form.add(dueDatePicker, dueTimePicker);

        discountInput.add(discountTypeGroup, discountAmountField, discountValueSpan);

        totalsSection.add(subtotalLabel, subtotalValueSpan);
        totalsSection.add(discountLabel, discountInput);
        totalsSection.add(totalLabel, totalValueSpan);

        itemsSection.add(addItemBlock);
        itemsSection.add(itemsGrid);
        itemsSection.add(totalsSection);

        content.add(form);
        content.add(new Hr());
        content.add(itemsSection);
        content.add(additionalDetailsField);

        dialog = new Dialog();
        dialog.setCloseOnOutsideClick(false);
        dialog.setWidth("700px");
        dialog.setMaxWidth("95vw");
        dialog.getHeader().add(header);
        dialog.add(content);
        dialog.getFooter().add(cancelButton, saveButton);

        // Focus phone field when dialog opens
        dialog.addOpenedChangeListener(e -> {
            if (e.isOpened()) {
                customerPhoneComboBox.focus();
            }
        });
    }

    // ========== Public API ==========

    public void open() {
        dialog.open();
    }

    public void close() {
        dialog.close();
    }

    public void setAvailableProducts(List<ProductSelect> products) {
        productComboBox.setItems(products);
    }

    // ========== Event Registration (NonComponent interface) ==========

    @Override
    @SuppressWarnings("unchecked")
    public <E extends NonComponentEvent<?>> Registration addListener(Class<E> eventType, Consumer<E> listener) {
        return eventSupport.addListener((Class<NonComponentEvent<EditOrderDialog>>) eventType,
                (Consumer<NonComponentEvent<EditOrderDialog>>) listener);
    }

    public Registration addSaveListener(Consumer<SaveEvent> listener) {
        return eventSupport.addListener(SaveEvent.class, listener);
    }

    public Registration addCancelListener(Consumer<CancelEvent> listener) {
        return eventSupport.addListener(CancelEvent.class, listener);
    }

    protected void fireEvent(NonComponentEvent<EditOrderDialog> event) {
        eventSupport.fireEvent(event);
    }

    // ========== Grid Cell Renderer ==========

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
            selectedCustomerSignal.value(customer);
            customPhoneSignal.value(null); // Clear custom phone since we're using existing customer
            customerNameField.setValue(customer.getName());
            customerNameField.setReadOnly(true);
            // Move focus to next field (due date, since location is already set)
            dueDatePicker.focus();
        } else {
            // Selection cleared - reset to allow new entry
            selectedCustomerSignal.value(null);
            customPhoneSignal.value(null);
            customerNameField.clear();
            customerNameField.setReadOnly(true); // Back to read-only until phone entered
        }
    }

    private void handleNewPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return;
        }
        // Format the phone number using location defaults
        var formattedPhone = formatPhoneNumber(phoneNumber);
        customPhoneSignal.value(formattedPhone);

        // Update the displayed value in the ComboBox input field
        customerPhoneComboBox.getElement()
                .executeJs("this.inputElement.value = $0", formattedPhone);

        // New phone number entered - enable name field for entry
        selectedCustomerSignal.value(null);
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
        var customPhone = customPhoneSignal.value();
        if (customPhone != null && !customPhone.isBlank()) {
            return customPhone;
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

    private void addOrUpdateItem() {
        if (editingItemSignal.value() != null) {
            updateSelectedItem();
        } else {
            addNewItem();
        }
    }

    private void updateSelectedItem() {
        var editingItem = editingItemSignal.value();
        if (editingItem == null) {
            return; // Shouldn't happen, but guard against it
        }

        var quantity = quantityField.getValue();
        if (quantity == null || quantity < 1) {
            Notification.show("Enter a valid quantity", 2000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        var details = itemDetailsField.getValue();
        var detailsNormalized = details == null ? "" : details.trim();

        // Check if another item has same product and notes (for combining)
        var matchingSignal = orderItemsListSignal.value().stream()
                .filter(s -> s.value() != editingItem) // Exclude the editing item
                .filter(s -> s.value().getProductId().equals(editingItem.getProductId()))
                .filter(s -> {
                    var existingDetails = s.value().getDetails() == null ? "" : s.value().getDetails().trim();
                    return existingDetails.equals(detailsNormalized);
                })
                .findFirst();

        if (matchingSignal.isPresent()) {
            // Combine: add quantity to matching item, remove editing item
            var targetSignal = matchingSignal.get();
            var target = targetSignal.value();
            target.setQuantity(target.getQuantity() + quantity);
            target.calculateLineTotal();
            targetSignal.value(target); // Trigger reactivity
            removeItem(editingItem);
        } else {
            // Update editing item
            editingItem.setQuantity(quantity);
            editingItem.setDetails(details);
            editingItem.calculateLineTotal();
            touchItem(editingItem); // Trigger reactivity
        }

        refreshItemsGrid();
        itemsGrid.deselectAll();
        // Edit mode will be cleared by selection listener setting editingItemSignal to null
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
        var existingSignal = orderItemsListSignal.value().stream()
                .filter(s -> s.value().getProductId().equals(product.getId()))
                .filter(s -> {
                    var existingDetails = s.value().getDetails() == null ? "" : s.value().getDetails().trim();
                    return existingDetails.equals(detailsNormalized);
                })
                .findFirst();

        if (existingSignal.isPresent()) {
            // Combine quantities
            var signal = existingSignal.get();
            var item = signal.value();
            item.setQuantity(item.getQuantity() + quantity);
            item.calculateLineTotal();
            signal.value(item); // Trigger reactivity
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
            orderItemsListSignal.insertLast(item);
        }

        refreshItemsGrid();

        productComboBox.clear();
        quantityField.setValue(1);
        itemDetailsField.clear();
        productComboBox.focus();
    }

    private void removeItem(OrderItemDetail item) {
        // Find the signal for this item and remove it
        orderItemsListSignal.value().stream()
                .filter(s -> s.value() == item)
                .findFirst()
                .ifPresent(orderItemsListSignal::remove);
        refreshItemsGrid();
    }

    /**
     * Triggers reactivity for an item after in-place modification.
     */
    private void touchItem(OrderItemDetail item) {
        orderItemsListSignal.value().stream()
                .filter(s -> s.value() == item)
                .findFirst()
                .ifPresent(s -> s.value(item));
    }

    private void refreshItemsGrid() {
        // Extract current items from signals for grid display
        var items = orderItemsListSignal.value().stream()
                .map(ValueSignal::value)
                .toList();
        itemsGrid.setItems(items);
    }

    private BigDecimal calculateDiscount(BigDecimal subtotal) {
        var discountInput = discountAmountField.getValue();
        if (discountInput == null || discountInput == 0) {
            return BigDecimal.ZERO;
        }

        var value = BigDecimal.valueOf(discountInput);

        if (DiscountType.PERCENT == discountTypeGroup.getValue()) {
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

        if (orderItemsListSignal.value().isEmpty()) {
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
            var selectedCustomer = selectedCustomerSignal.value();
            if (selectedCustomer != null) {
                order.setCustomerId(selectedCustomer.getId());
            }
            order.setLocationId(locationComboBox.getValue().getId());
            order.setLocationName(locationComboBox.getValue().getName());
            order.setDueDate(dueDatePicker.getValue());
            order.setDueTime(dueTimePicker.getValue());
            order.setAdditionalDetails(additionalDetailsField.getValue());
            // Extract items from signals
            var itemsList = orderItemsListSignal.value().stream()
                    .map(ValueSignal::value)
                    .toList();
            order.setItems(new ArrayList<>(itemsList));
            // Calculate discount based on subtotal
            var subtotal = itemsList.stream()
                    .map(OrderItemDetail::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setDiscount(calculateDiscount(subtotal));
            order.calculateTotal();
            order.setPaid(false);

            var savedOrder = orderService.create(order);

            Notification.show("Order created", 2000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            fireEvent(new SaveEvent(this, savedOrder));
            close();
        } catch (Exception e) {
            Notification.show("Failed: " + e.getMessage(), 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // ========== Event Classes ==========

    public static class SaveEvent extends NonComponentEvent<EditOrderDialog> {
        private final OrderDetail order;

        public SaveEvent(EditOrderDialog source, OrderDetail order) {
            super(source);
            this.order = order;
        }

        public OrderDetail getOrder() {
            return order;
        }

        public boolean isNewCustomerCreated() {
            return order != null && order.isNewCustomerCreated();
        }
    }

    public static class CancelEvent extends NonComponentEvent<EditOrderDialog> {
        public CancelEvent(EditOrderDialog source) {
            super(source);
        }
    }

    /**
     * Discount type for order discounts.
     */
    private enum DiscountType {
        PERCENT("%"),
        DOLLAR("$");

        private final String symbol;

        DiscountType(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }

        @Override
        public String toString() {
            return symbol;
        }
    }
}
