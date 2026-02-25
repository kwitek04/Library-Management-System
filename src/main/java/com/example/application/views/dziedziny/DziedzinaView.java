package com.example.application.views.dziedziny;

import com.example.application.data.entity.Dziedzina;
import com.example.application.data.service.BookService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@RolesAllowed("KIEROWNIK")
@Route(value = "dziedziny", layout = MainLayout.class)
@PageTitle("Dziedziny | Wypożyczalnia książek")
public class DziedzinaView extends VerticalLayout {

    private final BookService bookService;

    private final Grid<Dziedzina> grid = new Grid<>(Dziedzina.class);
    private final TextField nazwaField = new TextField("Nowa dziedzina");
    private final Button addBtn = new Button("Dodaj");

    public DziedzinaView(BookService bookService) {
        this.bookService = bookService;
        setSizeFull();

        configureGrid();

        HorizontalLayout toolbar = new HorizontalLayout(nazwaField, addBtn);
        toolbar.setAlignItems(Alignment.BASELINE);

        addBtn.addClickListener(e -> {
            try {
                String nazwa = nazwaField.getValue();
                if (nazwa != null && !nazwa.isEmpty()) {
                    Dziedzina nowa = new Dziedzina(nazwa);
                    bookService.saveDziedzina(nowa);
                    nazwaField.clear();
                    updateList();
                    com.vaadin.flow.component.notification.Notification.show("Dodano dziedzinę!");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                Notification.show("Błąd: " + ex.getMessage(),
                        5000, Notification.Position.MIDDLE);
            }
        });

        add(toolbar, grid);
        updateList();
    }

    private void configureGrid() {
        grid.setColumns("nazwa");
        grid.addColumn(d -> d.getPoddziedziny() != null ? d.getPoddziedziny().size() : 0)
                .setHeader("Liczba poddziedzin").setSortable(true);

        grid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                openPoddziedzinaDialog(e.getValue());
                grid.asSingleSelect().clear();
            }
        });

        grid.addComponentColumn(dziedzina -> {
            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

            deleteBtn.addClickListener(e -> {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Usunąć dziedzinę?");
                dialog.setText("Czy na pewno chcesz usunąć dziedzinę '" + dziedzina.getNazwa() + "'? " +
                        "Spowoduje to również usunięcie wszystkich jej poddziedzin.");

                dialog.setCancelable(true);
                dialog.setCancelText("Anuluj");
                dialog.setConfirmText("Usuń");
                dialog.setConfirmButtonTheme("error primary");

                dialog.addConfirmListener(event -> {
                    bookService.deleteDziedzina(dziedzina);
                    updateList();
                });

                dialog.open();
            });
            return deleteBtn;
        }).setHeader("Akcje").setFlexGrow(0).setAutoWidth(true);
    }

    private void openPoddziedzinaDialog(Dziedzina dziedzina) {
        new PoddziedzinaDialog(bookService, dziedzina, this::updateList).open();
    }

    private void updateList() {
        grid.setItems(bookService.findAllDziedziny());
    }
}
