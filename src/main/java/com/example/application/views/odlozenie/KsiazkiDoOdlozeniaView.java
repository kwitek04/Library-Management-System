package com.example.application.views.odlozenie;

import com.example.application.data.entity.Ksiazka;
import com.example.application.data.service.BookService;
import com.example.application.data.service.RentalService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

/**
 * Widok operacyjny dla Magazyniera.
 * Wyświetla listę książek, które zostały zwrócone przez czytelników i oczekują na odłożenie na półkę.
 * Magazynier potwierdza tutaj, że odniósł książkę na właściwą półkę, co zmienia jej status na "DOSTEPNA".
 */
@RolesAllowed({"MAGAZYNIER"})
@Route(value = "do-odlozenia", layout = MainLayout.class)
@PageTitle("Książki do odłożenia | Wypożyczalnia książek")
public class KsiazkiDoOdlozeniaView extends VerticalLayout {

    private final BookService bookService;
    private final RentalService rentalService;
    private final Grid<Ksiazka> grid = new Grid<>(Ksiazka.class);

    public KsiazkiDoOdlozeniaView(BookService bookService, RentalService rentalService) {
        this.bookService = bookService;
        this.rentalService = rentalService;

        setSizeFull();
        setPadding(true);

        add(new H2("Książki do odłożenia na półkę"));
        add(new Span("Lista książek, które zostały zwrócone i oczekują na odłożenie na półkę."));

        configureGrid();
        add(grid);

        updateList();
    }

    private void configureGrid() {
        grid.addClassName("odlozenie-grid");
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(k -> k.getDaneKsiazki().getTytul()).setHeader("Tytuł").setAutoWidth(true).setSortable(true);

        grid.addColumn(k -> k.getDaneKsiazki().getIsbn()).setHeader("ISBN").setSortable(true);

        grid.addColumn(k -> {
            if (k.getPoddziedzina() != null) {
                return k.getPoddziedzina().getDziedzina().getNazwa() + " > " + k.getPoddziedzina().getNazwa();
            }
            return "Brak lokalizacji";
        }).setHeader("Lokalizacja na półce").setAutoWidth(true).setSortable(true);

        grid.addComponentColumn(ksiazka -> {
            Button confirmBtn = new Button("Potwierdź odłożenie");
            confirmBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);

            confirmBtn.addClickListener(e -> {
                rentalService.potwierdzOdlozenie(ksiazka);

                Notification.show("Książka dostępna w systemie.", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateList();
            });

            return confirmBtn;
        }).setHeader("Akcja").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    private void updateList() {
        grid.setItems(bookService.findKsiazkiDoOdlozenia());
    }
}