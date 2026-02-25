package com.example.application.views.kary;

import com.example.application.data.entity.Uzytkownik;
import com.example.application.data.entity.Wypozyczenie;
import com.example.application.data.service.RentalService;
import com.example.application.data.service.UserService;
import com.example.application.security.SecurityService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.stream.Collectors;

/**
 * Widok panelu czytelnika przedstawiający naliczone kary finansowe.
 * Wyświetla historię przetrzymanych książek oraz sumaryczne zadłużenie wobec biblioteki.
 */
@RolesAllowed({"USER"})
@Route(value = "kary", layout = MainLayout.class)
@PageTitle("Kary i opłaty | Wypożyczalnia książek")
public class KaryView extends VerticalLayout {

    private final RentalService rentalService;
    private final UserService userService;
    private final SecurityService securityService;

    private final Grid<Wypozyczenie> grid = new Grid<>(Wypozyczenie.class);
    private final H2 totalDebtLabel = new H2();

    public KaryView(RentalService rentalService, UserService userService, SecurityService securityService) {
        this.rentalService = rentalService;
        this.userService = userService;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        add(new H4("Twoje należności wobec wypożyczalni"));

        totalDebtLabel.getStyle().set("color", "red");
        totalDebtLabel.getStyle().set("font-size", "3em");
        totalDebtLabel.getStyle().set("margin", "0");
        add(totalDebtLabel);

        configureGrid();
        add(grid);

        updateView();
    }

    private void configureGrid() {
        grid.addClassName("kary-grid");
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(w -> w.getWypozyczoneKsiazki().stream()
                        .map(wk -> wk.getKsiazka().getDaneKsiazki().getTytul())
                        .collect(Collectors.joining(", ")))
                .setHeader("Książka")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(Wypozyczenie::getTerminZwrotu)
                .setHeader("Termin zwrotu")
                .setSortable(true);

        grid.addColumn(w -> w.getDataOddania() != null ? w.getDataOddania().toString() : "Nie oddano")
                .setHeader("Data oddania")
                .setSortable(true);

        grid.addComponentColumn(w -> {
            Span karaSpan = new Span(String.format("%.2f zł", w.getKara()));
            karaSpan.getElement().getThemeList().add("badge error");
            return karaSpan;
        }).setHeader("Naliczona kara").setSortable(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    /**
     * Odświeża dane na ekranie.
     */
    private void updateView() {
        UserDetails userDetails = securityService.getAuthenticatedUser();
        if (userDetails != null) {
            Uzytkownik u = userService.findUzytkownikByEmail(userDetails.getUsername());
            if (u != null) {
                var listaKar = rentalService.findWypozyczeniaZKarami(u);
                grid.setItems(listaKar);

                Double suma = rentalService.obliczSumeKar(u);
                totalDebtLabel.setText(String.format("%.2f zł", suma));

                if (suma == 0) {
                    totalDebtLabel.getStyle().set("color", "green");
                    totalDebtLabel.setText("0.00 zł");
                } else {
                    totalDebtLabel.getStyle().set("color", "red");
                }
            }
        }
    }
}