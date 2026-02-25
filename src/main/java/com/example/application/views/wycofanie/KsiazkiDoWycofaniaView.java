package com.example.application.views.wycofanie;

import com.example.application.data.entity.Ksiazka;
import com.example.application.data.entity.Pracownik;
import com.example.application.data.service.BookService;
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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Widok dla Kierownika, służący do ostatecznego wycofywania książek z obiegu
 * (np. z powodu zniszczenia lub zagubienia).
 * Prezentuje listę książek, które zostały oznaczone przez Magazyniera statusem "DO_WYCOFANIA".
 */
@RolesAllowed("KIEROWNIK")
@Route(value = "wycofanie", layout = MainLayout.class)
@PageTitle("Książki do wycofania | Wypożyczalnia książek")
public class KsiazkiDoWycofaniaView extends VerticalLayout {

    private final BookService bookService;
    private final RentalService rentalService;
    private final UserService userService;
    private final SecurityService securityService;

    private final Grid<Ksiazka> grid = new Grid<>(Ksiazka.class);

    public KsiazkiDoWycofaniaView(BookService bookService,
                                  RentalService rentalService,
                                  UserService userService,
                                  SecurityService securityService) {
        this.bookService = bookService;
        this.rentalService = rentalService;
        this.userService = userService;
        this.securityService = securityService;

        setSizeFull();
        setPadding(true);

        add(new H2("Książki zgłoszone do wycofania"));
        add(new Span("Poniższe książki zostały oznaczone do wycofania przez magazyniera. Wymagana decyzja kierownika."));

        configureGrid();
        add(grid);
        updateList();
    }

    private void configureGrid() {
        grid.addClassName("wycofanie-grid");
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(k -> k.getDaneKsiazki().getTytul()).setHeader("Tytuł").setAutoWidth(true).setSortable(true);
        grid.addColumn(k -> k.getDaneKsiazki().getIsbn()).setHeader("ISBN").setSortable(true);
        grid.addColumn(Ksiazka::getLicznikWypozyczen).setHeader("Licznik wypożyczeń").setSortable(true);

        grid.addComponentColumn(ksiazka -> {
            Button wycofajBtn = new Button("Wycofaj z biblioteki");
            wycofajBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

            wycofajBtn.addClickListener(e -> {
                TextArea powodField = new TextArea("Powód wycofania");
                powodField.setPlaceholder("np. Zniszczona okładka, brak stron, zalanie...");
                powodField.setWidthFull();
                powodField.setMinHeight("100px");

                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Potwierdź wycofanie");
                dialog.setText("Czy na pewno chcesz trwale wycofać książkę \"" + ksiazka.getDaneKsiazki().getTytul() + "\"?");
                dialog.add(powodField);

                dialog.setCancelable(true);
                dialog.setCancelText("Anuluj");

                dialog.setConfirmText("Zatwierdź wycofanie");
                dialog.setConfirmButtonTheme("error primary");

                dialog.addConfirmListener(event -> {
                    if (powodField.getValue().isEmpty()) {
                        Notification.show("Musisz podać powód!", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }

                    UserDetails userDetails = securityService.getAuthenticatedUser();
                    Pracownik kierownik = userService.findPracownikByEmail(userDetails.getUsername());

                    rentalService.wycofajKsiazke(ksiazka, kierownik, powodField.getValue());

                    Notification.show("Książka została wycofana.", 3000, Notification.Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    updateList();
                });

                dialog.open();
            });

            return wycofajBtn;
        }).setHeader("Decyzja").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    private void updateList() {
        grid.setItems(bookService.findKsiazkiDoDecyzjiWycofania());
    }
}