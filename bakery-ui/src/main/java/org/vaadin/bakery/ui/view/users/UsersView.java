package org.vaadin.bakery.ui.view.users;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
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
import org.vaadin.bakery.service.LocationService;
import org.vaadin.bakery.service.UserService;
import org.vaadin.bakery.ui.component.ViewHeader;
import org.vaadin.bakery.uimodel.data.UserDetail;
import org.vaadin.bakery.uimodel.data.UserSummary;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * User management view (Admin only).
 * Displays a grid of users with CRUD operations.
 */
@Route("users")
@PageTitle("Users")
@Menu(order = 4, icon = LineAwesomeIconUrl.USERS_SOLID)
@RolesAllowed("ADMIN")
public class UsersView extends VerticalLayout {

    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final LocationService locationService;
    private final Grid<UserSummary> grid;
    private final TextField searchField;

    private List<UserSummary> allUsers;

    public UsersView(UserService userService, CurrentUserService currentUserService, LocationService locationService) {
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.locationService = locationService;

        addClassName("users-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Header with title, search, and add button
        searchField = createSearchField();
        var header = new ViewHeader("Users")
                .withFilters(searchField)
                .withAction("New user", () -> openDialog(null));

        // Grid container with padding
        var gridContainer = new Div();
        gridContainer.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.BoxSizing.BORDER);
        gridContainer.setSizeFull();

        grid = createGrid();
        gridContainer.add(grid);

        add(header, gridContainer);
        setFlexGrow(1, gridContainer);
        refreshGrid();
    }

    private TextField createSearchField() {
        var field = new TextField();
        field.setPlaceholder("Search users...");
        field.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        field.setValueChangeMode(ValueChangeMode.LAZY);
        field.addValueChangeListener(e -> filterGrid(e.getValue()));
        field.setWidth("300px");
        return field;
    }

    private Grid<UserSummary> createGrid() {
        var grid = new Grid<>(UserSummary.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setSizeFull();

        grid.addComponentColumn(this::createUserAvatar)
                .setHeader("")
                .setFlexGrow(0)
                .setAutoWidth(true);

        grid.addColumn(UserSummary::getEmail)
                .setHeader("Email")
                .setSortable(true)
                .setFlexGrow(2);

        grid.addColumn(UserSummary::getFullName)
                .setHeader("Name")
                .setSortable(true)
                .setFlexGrow(2);

        grid.addComponentColumn(user -> {
            var badge = new Span(user.getRole().getDisplayName());
            String badgeColor = switch (user.getRole()) {
                case ADMIN -> "primary";
                case BAKER -> "success";
                case BARISTA -> "warning";
            };
            badge.getElement().getThemeList().add("badge pill " + badgeColor);
            return badge;
        }).setHeader("Role").setFlexGrow(0).setAutoWidth(true);

        grid.addItemClickListener(event -> openDialogForEdit(event.getItem().getId()));

        return grid;
    }

    private Avatar createUserAvatar(UserSummary user) {
        var avatar = new Avatar(user.getFullName());
        if (user.getPhoto() != null && user.getPhoto().length > 0) {
            var resource = new StreamResource(
                    "user-" + user.getId(),
                    () -> new ByteArrayInputStream(user.getPhoto())
            );
            avatar.setImageResource(resource);
        }
        return avatar;
    }

    private void openDialog(UserDetail user) {
        var currentUserEmail = currentUserService.getCurrentUserEmail().orElse(null);
        var dialog = new UserDialog(user, userService, locationService, currentUserEmail);
        dialog.addSaveListener(e -> refreshGrid());
        dialog.addDeleteListener(e -> refreshGrid());
        dialog.open();
    }

    private void openDialogForEdit(Long userId) {
        userService.get(userId).ifPresent(this::openDialog);
    }

    private void refreshGrid() {
        allUsers = userService.list();
        filterGrid(searchField.getValue());
    }

    private void filterGrid(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            grid.setItems(allUsers);
        } else {
            var lowerSearch = searchTerm.toLowerCase();
            grid.setItems(allUsers.stream()
                    .filter(u -> u.getEmail().toLowerCase().contains(lowerSearch) ||
                            u.getFullName().toLowerCase().contains(lowerSearch))
                    .toList());
        }
    }
}
