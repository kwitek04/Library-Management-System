package com.example.application.views.rezerwacje;

import com.example.application.data.entity.*;
import com.example.application.data.service.RentalService;
import com.example.application.data.service.UserService;
import com.example.application.security.SecurityService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.stream.Collectors;

/**
 * Widok panelu czytelnika prezentujący jego aktywne rezerwacje.
 * Z tego poziomu użytkownik może anulować rezerwację lub potwierdzić odbiór zarezerwowanej książki.
 */
@RolesAllowed({"USER"})
@Route(value = "moje-rezerwacje", layout = MainLayout.class)
@PageTitle("Moje rezerwacje | Wypożyczalnia książek")
public class MojeRezerwacjeView extends VerticalLayout {

    private final RentalService rentalService;
    private final UserService userService;
    private final SecurityService securityService;

    private final Grid<Rezerwacja> grid = new Grid<>(Rezerwacja.class);

    public MojeRezerwacjeView(RentalService rentalService, UserService userService, SecurityService securityService) {
        this.rentalService = rentalService;
        this.userService = userService;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);
        addClassName("moje-rezerwacje-view");

        add(new H2("Moje zarezerwowane książki"));
        add(new Html("<span>Gdy weźmiesz książkę z półki, kliknij <i>Odbierz</i>, aby ją wypożyczyć.</span>"));

        configureGrid();
        add(grid);

        updateList();
    }

    /**
     * Konfiguruje tabelę wyświetlającą przypisane do konta rezerwacje i ich statusy.
     */
    private void configureGrid() {
        grid.addClassName("rezerwacje-grid");
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(rezerwacja ->
                rezerwacja.getZarezerwowaneKsiazki().stream()
                        .map(zk -> zk.getKsiazka().getDaneKsiazki().getTytul())
                        .collect(Collectors.joining(", "))
        ).setHeader("Książki").setAutoWidth(true).setSortable(true);

        grid.addColumn(Rezerwacja::getDataRezerwacji).setHeader("Data rezerwacji").setAutoWidth(true).setSortable(true);
        grid.addColumn(Rezerwacja::getWaznaDo).setHeader("Ważna do").setAutoWidth(true).setSortable(true);

        grid.addComponentColumn(rezerwacja -> {
            Span badge = new Span(rezerwacja.getStatus() != null ? rezerwacja.getStatus().getNazwa() : "Błąd");
            badge.getElement().getThemeList().add("badge");

            if (rezerwacja.getStatus() == StatusRezerwacji.AKTYWNA) {
                badge.getElement().getThemeList().add("success");
            } else if (rezerwacja.getStatus() == StatusRezerwacji.ANULOWANA) {
                badge.getElement().getThemeList().add("error");
            } else if (rezerwacja.getStatus() == StatusRezerwacji.ZREALIZOWANA) {
                badge.getElement().getThemeList().add("contrast");
            }
            return badge;
        }).setHeader("Status").setAutoWidth(true).setSortable(true);

        grid.addComponentColumn(rezerwacja -> {
            Button odbierzBtn = new Button("Odbierz");
            odbierzBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

            odbierzBtn.setEnabled(rezerwacja.getStatus() == StatusRezerwacji.AKTYWNA);

            odbierzBtn.addClickListener(e -> {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Potwierdzenie odbioru");
                dialog.setText("Czy potwierdzasz odbiór książki?");
                dialog.setCancelable(true);
                dialog.setCancelText("Anuluj");
                dialog.setConfirmText("Tak, odbieram");
                dialog.setConfirmButtonTheme("success primary");

                dialog.addConfirmListener(event -> odbierzRezerwacje(rezerwacja));
                dialog.open();
            });

            return odbierzBtn;
        }).setHeader("Odbiór").setAutoWidth(true);

        grid.addComponentColumn(rezerwacja -> {
            Button anulujBtn = new Button("Anuluj");
            anulujBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            anulujBtn.setEnabled(rezerwacja.getStatus() == StatusRezerwacji.AKTYWNA);

            anulujBtn.addClickListener(e -> {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Anulowanie rezerwacji");
                dialog.setText("Czy na pewno chcesz anulować rezerwację?");
                dialog.setCancelable(true);
                dialog.setCancelText("Nie");
                dialog.setConfirmText("Tak, anuluj");
                dialog.setConfirmButtonTheme("error primary");

                dialog.addConfirmListener(event -> {
                    rentalService.anulujRezerwacje(rezerwacja);
                    Notification.show("Rezerwacja anulowana", 3000, Notification.Position.MIDDLE);
                    updateList();
                });
                dialog.open();
            });

            return anulujBtn;
        }).setHeader("Anulowanie");

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    /**
     * Realizuje proces odbioru zarezerwowanej książki przez czytelnika – zamienia rezerwację na aktywne wypożyczenie.
     */
    private void odbierzRezerwacje(Rezerwacja rezerwacja) {
        try {
            String username = securityService.getAuthenticatedUser().getUsername();
            Uzytkownik currentUser = userService.findUzytkownikByEmail(username);

            for (ZarezerwowanaKsiazka zk : rezerwacja.getZarezerwowaneKsiazki()) {
                Ksiazka ksiazka = zk.getKsiazka();
                rentalService.wypozyczKsiazke(ksiazka, currentUser);
            }

            Notification.show("Pomyślnie wypożyczono książki!", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            updateList();

        } catch (Exception ex) {
            Notification.show("Błąd: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void updateList() {
        String username = securityService.getAuthenticatedUser().getUsername();
        Uzytkownik uzytkownik = userService.findUzytkownikByEmail(username);

        if (uzytkownik != null) {
            grid.setItems(rentalService.findRezerwacjeByUser(uzytkownik));
        }
    }
}