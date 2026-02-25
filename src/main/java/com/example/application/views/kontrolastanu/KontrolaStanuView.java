package com.example.application.views.kontrolastanu;

import com.example.application.data.entity.Ksiazka;
import com.example.application.data.entity.StanFizyczny;
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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@RolesAllowed({"MAGAZYNIER"})
@Route(value = "kontrola-stanu", layout = MainLayout.class)
@PageTitle("Kontrola stanu | Wypożyczalnia książek")
public class KontrolaStanuView extends VerticalLayout {

    private final BookService bookService;
    private final RentalService rentalService;
    private final Grid<Ksiazka> grid = new Grid<>(Ksiazka.class);

    public KontrolaStanuView(BookService bookService, RentalService rentalService) {
        this.bookService = bookService;
        this.rentalService = rentalService;

        setSizeFull();
        setPadding(true);

        add(new H2("Kontrola stanu fizycznego książek"));
        add(new Span("Poniższe książki przekroczyły cykl 5 wypożyczeń i wymagają weryfikacji przez magazyniera."));

        configureGrid();
        add(grid);

        updateList();
    }

    private void configureGrid() {
        grid.addClassName("kontrola-grid");
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(k -> k.getDaneKsiazki().getTytul()).setHeader("Tytuł").setAutoWidth(true).setSortable(true);
        grid.addColumn(k -> k.getDaneKsiazki().getIsbn()).setHeader("ISBN").setSortable(true);
        grid.addColumn(Ksiazka::getLicznikWypozyczen).setHeader("Licznik wypożyczeń").setSortable(true);

        grid.addComponentColumn(ksiazka -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setAlignItems(Alignment.BASELINE);

            Select<StanFizyczny> statusSelect = new Select<>();
            statusSelect.setItems(StanFizyczny.values());
            statusSelect.setValue(ksiazka.getStanFizyczny());
            statusSelect.setPlaceholder("Wybierz stan");
            statusSelect.setWidth("180px");

            Button confirmBtn = new Button("Zatwierdź");
            confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            confirmBtn.addClickListener(e -> {
                rentalService.zaktualizujStanPoKontroli(ksiazka, statusSelect.getValue());
                Notification.show("Stan zaktualizowany. Książka wraca do obiegu.", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                updateList();
            });

            actions.add(statusSelect, confirmBtn);
            return actions;
        }).setHeader("Aktualizacja stanu").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    private void updateList() {
        grid.setItems(bookService.findKsiazkiDoKontroli());
    }
}