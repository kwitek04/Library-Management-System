package com.example.application.views.pracownicy;
import com.example.application.data.entity.Pracownik;
import com.example.application.data.service.UserService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * Widok zarządzania pracownikami. Dostępny tylko dla Kierownika.
 */
@RolesAllowed("KIEROWNIK")
@Route(value = "pracownicy", layout = MainLayout.class)
@PageTitle("Lista pracowników | Wypożyczalnia książek")
public class PracownicyView extends VerticalLayout {

    private final UserService userService;

    private final Grid<Pracownik> grid = new Grid<>(Pracownik.class);
    private final TextField filterText = new TextField();
    private PracownicyForm form;

    public PracownicyView(UserService userService) {
        this.userService = userService;

        addClassName("pracownicy-view");
        setSizeFull();
        configureGrid();
        configureForm();

        add(getToolbar(), getContent());
        updateList();
        closeEditor();
    }

    private void configureGrid() {
        grid.addClassNames("pracownicy-grid");
        grid.setSizeFull();
        grid.setColumns("imie", "nazwisko", "email", "nrTelefonu");

        grid.addColumn(Pracownik::getRoleAsString)
                .setHeader("Role")
                .setSortable(true);

        grid.addColumn(p -> p.isEnabled() ? "Aktywne" : "Zablokowane")
                .setHeader("Status konta")
                .setSortable(true);

        grid.addComponentColumn(pracownik -> {
            Button editBtn = new Button(new Icon(VaadinIcon.EDIT));
            editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            editBtn.addClickListener(e -> editPracownicy(pracownik));
            return editBtn;
        }).setHeader("Akcje").setAutoWidth(true).setFlexGrow(0);

        grid.getColumns().forEach(col -> col.setAutoWidth(true));
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Wyszukaj...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        Button addPracownicyButton = new Button("Dodaj pracownika");
        addPracownicyButton.addClickListener(click -> addPracownicy());

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addPracownicyButton);
        toolbar.addClassName("pracownicy_toolbar");
        return toolbar;
    }

    private void configureForm() {
        form = new PracownicyForm(userService.findAllRoles());
        form.setWidth("25em");
        form.addListener(PracownicyForm.SaveEvent.class, this::savePracownicy);
        form.addListener(PracownicyForm.DeleteEvent.class, this::deletePracownicy);
        form.addListener(PracownicyForm.CloseEvent.class, e -> closeEditor());
    }

    private void savePracownicy(PracownicyForm.SaveEvent event) {
        userService.savePracownicy(event.getPracownicy());
        updateList();
        closeEditor();
    }

    private void deletePracownicy(PracownicyForm.DeleteEvent event) {
        userService.deletePracownicy(event.getPracownicy());
        updateList();
        closeEditor();
    }

    private Component getContent() {
        HorizontalLayout content = new HorizontalLayout(grid, form);
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, form);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    public void editPracownicy(Pracownik pracownik) {
        if (pracownik == null) {
            closeEditor();
        } else {
            form.setPracownicy(pracownik);
            form.setVisible(true);
            addClassName("editing");
        }
    }

    private void closeEditor() {
        form.setPracownicy(null);
        form.setVisible(false);
        removeClassName("editing");
    }

    private void addPracownicy() {
        grid.asSingleSelect().clear();
        editPracownicy(new Pracownik());
    }

    private void updateList() {
        grid.setItems(userService.findAllPracownicy(filterText.getValue()));
    }
}