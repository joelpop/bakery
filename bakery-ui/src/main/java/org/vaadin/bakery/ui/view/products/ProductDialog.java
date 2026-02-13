package org.vaadin.bakery.ui.view.products;

import com.vaadin.flow.component.ComponentEffect;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.service.ProductService;
import org.vaadin.bakery.service.StaleDataException;
import org.vaadin.bakery.ui.component.StaleDataBanner;
import org.vaadin.bakery.ui.component.StaleDataHelper;
import org.vaadin.bakery.ui.event.DataChangeSignals;
import org.vaadin.bakery.ui.event.NonComponent;
import org.vaadin.bakery.ui.event.NonComponentEvent;
import org.vaadin.bakery.ui.event.NonComponentEventSupport;
import org.vaadin.bakery.uimodel.data.ProductSummary;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * Dialog for creating and editing products.
 * Uses delegation rather than inheritance to avoid exposing Dialog's full API.
 */
public class ProductDialog implements NonComponent {

    private final Dialog dialog;
    private final NonComponentEventSupport<ProductDialog> eventSupport;

    private final ProductService productService;
    private ProductSummary product;
    private final boolean isNew;
    // Banner shown when another session modifies or deletes the product being edited
    private StaleDataBanner staleDataBanner;

    private final TextField nameField;
    private final TextArea descriptionField;
    private final TextField sizeField;
    private final BigDecimalField priceField;
    private final Checkbox availableCheckbox;

    private final Div photoContainerDiv;
    private final MemoryBuffer photoUploadBuffer;
    private byte[] uploadedPhoto;
    private String uploadedPhotoContentType;

    private final Binder<ProductSummary> binder;

    /**
     * Creates a product dialog. Opens in create mode if the product has no ID,
     * or edit mode if it has an existing ID.
     *
     * @param product        the product to create or edit
     * @param productService service for persisting product changes
     */
    public ProductDialog(ProductSummary product, ProductService productService) {
        this.productService = productService;
        this.product = product;
        this.isNew = product.getId() == null;

        dialog = new Dialog();
        eventSupport = new NonComponentEventSupport<>();

        // Component initializations
        nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setWidthFull();

        descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();
        descriptionField.setMinHeight("80px");

        sizeField = new TextField("Size");
        sizeField.setWidthFull();
        sizeField.setHelperText("e.g., \"12 ppl\", \"individual\"");

        priceField = new BigDecimalField("Price");
        priceField.setWidthFull();
        priceField.setPrefixComponent(new Span("$"));

        availableCheckbox = new Checkbox("Available");

        photoContainerDiv = new Div();
        photoContainerDiv.getStyle()
                .set("width", "100px")
                .set("height", "100px")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("overflow", "hidden");

        photoUploadBuffer = new MemoryBuffer();
        var upload = new Upload(photoUploadBuffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif");
        upload.setMaxFileSize(5 * 1024 * 1024); // 5MB
        upload.addSucceededListener(this::onPhotoUploadSucceeded);

        var photoSection = new Div();
        photoSection.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Margin.Bottom.MEDIUM
        );
        photoSection.add(photoContainerDiv, upload);

        var cancelButton = new Button("Cancel", _ -> close());

        var saveButton = new Button("Save", _ -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Binder bindings
        binder = new Binder<>(ProductSummary.class);

        binder.forField(nameField)
                .asRequired("Name is required")
                .withValidator(name -> isNew ?
                                !productService.nameExists(name) :
                                !productService.nameExistsForOtherProduct(name, product.getId()),
                        "A product with this name already exists")
                .bind(ProductSummary::getName, ProductSummary::setName);

        binder.forField(descriptionField)
                .bind(ProductSummary::getDescription, ProductSummary::setDescription);

        binder.forField(sizeField)
                .bind(ProductSummary::getSize, ProductSummary::setSize);

        binder.forField(priceField)
                .asRequired("Price is required")
                .withValidator(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0,
                        "Price must be greater than zero")
                .bind(ProductSummary::getPrice, ProductSummary::setPrice);

        binder.forField(availableCheckbox)
                .bind(ProductSummary::isAvailable, ProductSummary::setAvailable);

        // Value settings
        if (isNew) {
            availableCheckbox.setValue(true);
        }

        binder.readBean(product);
        updatePhotoPreview();

        // Layout assembly
        var form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );
        form.add(photoSection, 2);
        form.add(nameField, 2);
        form.add(descriptionField, 2);
        form.add(sizeField, 1);
        form.add(priceField, 1);
        form.add(availableCheckbox, 2);

        var footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        if (!isNew) {
            var deleteButton = new Button("Delete", _ -> confirmDelete());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

            var spacer = new Span();
            footer.add(deleteButton, spacer, cancelButton, saveButton);
            footer.setFlexGrow(1, spacer);
        } else {
            footer.add(cancelButton, saveButton);
        }

        // Stale data detection - when editing an existing product, monitor for changes from other sessions
        if (!isNew) {
            staleDataBanner = new StaleDataBanner();

            // Reactive effect: checks if the product was modified or deleted by another session
            // whenever the shared productVersion signal changes
            ComponentEffect.effect(dialog, () -> {
                DataChangeSignals.productVersion().value();
                checkForExternalChanges();
            });
        }

        // Dialog configuration
        dialog.setHeaderTitle(isNew ? "New Product" : "Edit Product");
        dialog.setCloseOnOutsideClick(false);
        dialog.getElement().getThemeList().add("responsive-dialog");
        dialog.setWidth("100%");
        dialog.setMaxWidth("600px");
        if (!isNew) {
            dialog.add(staleDataBanner);
        }
        dialog.add(form);
        dialog.getFooter().add(footer);
    }

    /** Opens the dialog. */
    public void open() {
        dialog.open();
    }

    /** Closes the dialog. */
    public void close() {
        dialog.close();
    }

    private void updatePhotoPreview() {
        photoContainerDiv.removeAll();

        byte[] photoData = uploadedPhoto != null ? uploadedPhoto : product.getPhoto();
        if (photoData != null && photoData.length > 0) {
            var resource = new StreamResource("product-photo",
                    () -> new ByteArrayInputStream(photoData));
            var image = new Image(resource, "Product photo");
            image.setWidthFull();
            image.setHeightFull();
            image.getStyle().set("object-fit", "cover");
            photoContainerDiv.add(image);
        }
    }

    private void save() {
        try {
            binder.writeBean(product);

            // Apply uploaded photo if any
            if (uploadedPhoto != null) {
                product.setPhoto(uploadedPhoto);
                product.setPhotoContentType(uploadedPhotoContentType);
            }

            if (isNew) {
                productService.create(product);
                Notification.show("Product created", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                // Pre-save freshness check: abort save if the product was modified or deleted by another session
                if (StaleDataHelper.isStale(
                        () -> productService.getVersion(product.getId()),
                        product.getVersion(), staleDataBanner,
                        this::reloadData, this::close)) {
                    return;
                }

                productService.update(product.getId(), product);
                Notification.show("Product updated", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }

            eventSupport.fireEvent(new SaveEvent(this));
            close();
        } catch (StaleDataException _) {
            // Fallback: optimistic lock failed during flush — show stale data banner
            staleDataBanner.showModified(this::reloadData);
        } catch (ValidationException _) {
            Notification.show("Please fix the validation errors", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /** Live detection: compares the DB version against the version loaded into the form. */
    private void checkForExternalChanges() {
        if (!dialog.isOpened()) return;
        StaleDataHelper.checkForExternalChanges(
                () -> productService.getVersion(product.getId()),
                product.getVersion(), staleDataBanner,
                this::reloadData, this::close);
    }

    /** Reloads the latest product data from the database into the form, or shows deleted banner. */
    private void reloadData() {
        productService.get(product.getId()).ifPresentOrElse(
                freshProduct -> {
                    product = freshProduct;
                    binder.readBean(product);
                    updatePhotoPreview();
                    staleDataBanner.hide();
                },
                () -> staleDataBanner.showDeleted(this::close)
        );
    }

    private void confirmDelete() {
        var confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Delete Product");
        confirmDialog.add(new Span("Are you sure you want to delete \"" + product.getName() + "\"?"));

        var cancelButton = new Button("Cancel", _ -> confirmDialog.close());
        var deleteButton = new Button("Delete", _ -> {
            confirmDialog.close();
            delete();
        });
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        confirmDialog.getFooter().add(cancelButton, deleteButton);
        confirmDialog.open();
    }

    private void delete() {
        try {
            productService.delete(product.getId());
            Notification.show("Product deleted", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            eventSupport.fireEvent(new DeleteEvent(this));
            close();
        } catch (Exception e) {
            Notification.show("Cannot delete product: " + e.getMessage(), 5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // Event Handlers

    private void onPhotoUploadSucceeded(SucceededEvent event) {
        try {
            uploadedPhoto = photoUploadBuffer.getInputStream().readAllBytes();
            uploadedPhotoContentType = event.getMIMEType();
            updatePhotoPreview();
        } catch (IOException _) {
            Notification.show("Failed to upload image", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // NonComponent implementation

    @Override
    @SuppressWarnings("unchecked")
    public <E extends NonComponentEvent<?>> Registration addListener(Class<E> eventType, Consumer<E> listener) {
        return eventSupport.addListener((Class<NonComponentEvent<ProductDialog>>) eventType,
                (Consumer<NonComponentEvent<ProductDialog>>) listener);
    }

    /** Registers a listener for save events. */
    public Registration addSaveListener(Consumer<SaveEvent> listener) {
        return eventSupport.addListener(SaveEvent.class, listener);
    }

    /** Registers a listener for delete events. */
    public Registration addDeleteListener(Consumer<DeleteEvent> listener) {
        return eventSupport.addListener(DeleteEvent.class, listener);
    }

    // Events

    /** Event fired when a product is successfully saved. */
    public static class SaveEvent extends NonComponentEvent<ProductDialog> {
        /** Creates a save event. */
        public SaveEvent(ProductDialog source) {
            super(source);
        }
    }

    /** Event fired when a product is successfully deleted. */
    public static class DeleteEvent extends NonComponentEvent<ProductDialog> {
        /** Creates a delete event. */
        public DeleteEvent(ProductDialog source) {
            super(source);
        }
    }
}
