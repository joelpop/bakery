package org.vaadin.bakery.ui.view.products;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
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
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.vaadin.bakery.service.ProductService;
import org.vaadin.bakery.uimodel.data.ProductSummary;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Dialog for creating and editing products.
 */
public class ProductDialog extends Dialog {

    private final ProductService productService;
    private final ProductSummary product;
    private final boolean isNew;

    private final TextField nameField = new TextField("Name");
    private final TextArea descriptionField = new TextArea("Description");
    private final TextField sizeField = new TextField("Size");
    private final BigDecimalField priceField = new BigDecimalField("Price");
    private final Checkbox availableCheckbox = new Checkbox("Available");

    private final Div photoContainer = new Div();
    private byte[] uploadedPhoto;
    private String uploadedPhotoContentType;

    private final Binder<ProductSummary> binder = new Binder<>(ProductSummary.class);

    public ProductDialog(ProductSummary product, ProductService productService) {
        this.productService = productService;
        this.product = product;
        this.isNew = product.getId() == null;

        setHeaderTitle(isNew ? "New Product" : "Edit Product");
        setModal(true);
        setCloseOnOutsideClick(false);
        // Responsive sizing: max-width on desktop, full-screen on mobile via CSS theme
        getElement().getThemeList().add("responsive-dialog");
        setWidth("100%");
        setMaxWidth("600px");

        configureFields();
        configureBinder();
        createLayout();
        createFooter();

        binder.readBean(product);
        updatePhotoPreview();
    }

    private void configureFields() {
        nameField.setRequired(true);
        nameField.setWidthFull();

        descriptionField.setWidthFull();
        descriptionField.setMinHeight("80px");

        sizeField.setWidthFull();
        sizeField.setHelperText("e.g., \"12 ppl\", \"individual\"");

        priceField.setWidthFull();
        priceField.setPrefixComponent(new Span("$"));

        if (isNew) {
            availableCheckbox.setValue(true);
        }
    }

    private void configureBinder() {
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
    }

    private void createLayout() {
        var form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2)
        );

        // Photo upload section
        var photoSection = createPhotoSection();
        form.add(photoSection, 2);

        form.add(nameField, 2);
        form.add(descriptionField, 2);
        form.add(sizeField, 1);
        form.add(priceField, 1);
        form.add(availableCheckbox, 2);

        add(form);
    }

    private Div createPhotoSection() {
        var section = new Div();
        section.addClassNames(
                LumoUtility.Display.FLEX,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Margin.Bottom.MEDIUM
        );

        photoContainer.getStyle()
                .set("width", "100px")
                .set("height", "100px")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background", "var(--lumo-contrast-10pct)")
                .set("overflow", "hidden");

        var buffer = new MemoryBuffer();
        var upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif");
        upload.setMaxFileSize(5 * 1024 * 1024); // 5MB

        upload.addSucceededListener(event -> {
            try {
                uploadedPhoto = buffer.getInputStream().readAllBytes();
                uploadedPhotoContentType = event.getMIMEType();
                updatePhotoPreview();
            } catch (IOException e) {
                Notification.show("Failed to upload image", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        section.add(photoContainer, upload);
        return section;
    }

    private void updatePhotoPreview() {
        photoContainer.removeAll();

        byte[] photoData = uploadedPhoto != null ? uploadedPhoto : product.getPhoto();
        if (photoData != null && photoData.length > 0) {
            var resource = new StreamResource("product-photo",
                    () -> new ByteArrayInputStream(photoData));
            var image = new Image(resource, "Product photo");
            image.setWidthFull();
            image.setHeightFull();
            image.getStyle().set("object-fit", "cover");
            photoContainer.add(image);
        }
    }

    private void createFooter() {
        var footer = new HorizontalLayout();
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        var cancelButton = new Button("Cancel", e -> close());

        var saveButton = new Button("Save", e -> save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        if (!isNew) {
            var deleteButton = new Button("Delete", e -> confirmDelete());
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

            var spacer = new Span();
            footer.add(deleteButton, spacer, cancelButton, saveButton);
            footer.setFlexGrow(1, spacer);
        } else {
            footer.add(cancelButton, saveButton);
        }

        getFooter().add(footer);
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
                productService.update(product.getId(), product);
                Notification.show("Product updated", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            }

            fireEvent(new SaveEvent(this));
            close();
        } catch (ValidationException e) {
            Notification.show("Please fix the validation errors", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void confirmDelete() {
        var confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Delete Product");
        confirmDialog.add(new Span("Are you sure you want to delete \"" + product.getName() + "\"?"));

        var cancelButton = new Button("Cancel", e -> confirmDialog.close());
        var deleteButton = new Button("Delete", e -> {
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
            fireEvent(new DeleteEvent(this));
            close();
        } catch (Exception e) {
            Notification.show("Cannot delete product: " + e.getMessage(), 5000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    // Events
    public static class SaveEvent extends ComponentEvent<ProductDialog> {
        public SaveEvent(ProductDialog source) {
            super(source, false);
        }
    }

    public static class DeleteEvent extends ComponentEvent<ProductDialog> {
        public DeleteEvent(ProductDialog source) {
            super(source, false);
        }
    }

    public Registration addSaveListener(ComponentEventListener<SaveEvent> listener) {
        return addListener(SaveEvent.class, listener);
    }

    public Registration addDeleteListener(ComponentEventListener<DeleteEvent> listener) {
        return addListener(DeleteEvent.class, listener);
    }
}
