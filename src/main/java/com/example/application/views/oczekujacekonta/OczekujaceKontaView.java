package com.example.application.views.oczekujacekonta;

import com.example.application.data.entity.Uzytkownik;
import com.example.application.data.service.UserService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@RolesAllowed({"BIBLIOTEKARZ"})
@Route(value = "aktywacja", layout = MainLayout.class)
@PageTitle("Aktywacja kont | Wypożyczalnia książek")
public class OczekujaceKontaView extends VerticalLayout {

    private final UserService userService;
    private final Grid<Uzytkownik> grid = new Grid<>(Uzytkownik.class);

    public OczekujaceKontaView(UserService userService) {
        this.userService = userService;
        setSizeFull();

        grid.setColumns("imie", "nazwisko", "email", "dataUrodzenia");

        grid.addComponentColumn(uzytkownik -> {
            Button activeBtn = new Button("Aktywuj", e -> {
                uzytkownik.setEnabled(true);
                userService.saveUzytkownik(uzytkownik);
                updateList();
                Notification.show("Konto aktywowane!");
            });
            activeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
            return activeBtn;
        }).setHeader("Akcja");

        add(new H2("Konta oczekujące na weryfikację"), grid);
        updateList();
    }

    private void updateList() {
        grid.setItems(userService.findAllPendingUzytkownicy());
    }
}