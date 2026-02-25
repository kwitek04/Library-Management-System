package com.example.application.views.uzytkownicy;

import com.example.application.data.entity.Uzytkownik;
import com.example.application.data.entity.Wypozyczenie;
import com.example.application.data.service.RentalService;
import com.example.application.data.service.UserService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Widok zarządzania użytkownikami.
 * Pozwala na edycję danych, blokowanie kont oraz podgląd historii wypożyczeń i naliczonych kar.
 */
@RolesAllowed({"BIBLIOTEKARZ"})
@Route(value = "uzytkownicy", layout = MainLayout.class)
@PageTitle("Lista użytkowników | Wypożyczalnia książek")
public class UzytkownicyView extends VerticalLayout {

    private final UserService userService;
    private final RentalService rentalService;

    private final Grid<Uzytkownik> grid = new Grid<>(Uzytkownik.class);
    private final TextField filterText = new TextField();
    private UzytkownicyForm form;

    public UzytkownicyView(UserService userService, RentalService rentalService) {
        this.userService = userService;
        this.rentalService = rentalService;

        addClassName("uzytkownicy-view");
        setSizeFull();
        configureGrid();
        configureForm();

        add(getToolbar(), getContent());
        updateList();
        closeEditor();
    }

    private void configureGrid() {
        grid.addClassNames("uzytkownicy-grid");
        grid.setSizeFull();
        grid.setColumns("imie", "nazwisko", "email", "nrTelefonu");

        grid.addColumn(u -> u.getDataUrodzenia() != null ? u.getDataUrodzenia().toString() : "")
                .setHeader("Data urodzenia").setSortable(true);;

        grid.addColumn(u -> {
            if (u.isLocked()) return "Zablokowane";
            if (!u.isEnabled()) return "Oczekujące";
            return "Aktywne";
        }).setHeader("Status konta").setSortable(true);;

        grid.addComponentColumn(uzytkownik -> {
            Button statsBtn = new Button(VaadinIcon.CHART.create());
            statsBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            statsBtn.setTooltipText("Statystyki użytkownika");
            statsBtn.addClickListener(e -> openStatsDialog(uzytkownik));

            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            editBtn.setTooltipText("Edytuj dane użytkownika");
            editBtn.addClickListener(e -> editUzytkownik(uzytkownik));

            return new HorizontalLayout(statsBtn, editBtn);
        }).setHeader("Akcje").setAutoWidth(true).setFlexGrow(0);

        grid.getColumns().forEach(col -> col.setAutoWidth(true));

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    private void openStatsDialog(Uzytkownik uzytkownik) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Statystyki użytkownika");
        dialog.setWidth("800px");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);

        H3 title = new H3(uzytkownik.getImie() + " " + uzytkownik.getNazwisko());
        title.getStyle().set("margin-top", "0");

        Span emailLabel = new Span(uzytkownik.getEmail());
        emailLabel.getStyle().set("color", "gray").set("font-size", "0.9em");

        dialogLayout.add(title, emailLabel);

        List<Wypozyczenie> historia = rentalService.findWypozyczeniaByUser(uzytkownik);

        int liczbaKsiazek = historia.stream()
                .mapToInt(w -> w.getWypozyczoneKsiazki().size())
                .sum();

        Double dlug = rentalService.obliczSumeKar(uzytkownik);
        String statusKonta = uzytkownik.isLocked() ? "Zablokowane" : (uzytkownik.isEnabled() ? "Aktywne" : "Oczekujące");

        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();
        cards.setSpacing(true);

        VerticalLayout card1 = createStatCard("Wypożyczone książki (łącznie)", String.valueOf(liczbaKsiazek), VaadinIcon.BOOK);
        VerticalLayout card2 = createStatCard("Należności (Kary)", String.format("%.2f zł", dlug), VaadinIcon.MONEY);
        VerticalLayout card3 = createStatCard("Status konta", statusKonta, VaadinIcon.USER_CARD);

        if (dlug > 0) {
            card2.getChildren().reduce((first, second) -> second).ifPresent(comp ->
                    comp.getStyle().set("color", "var(--lumo-error-text-color)"));
        }

        cards.add(card1, card2, card3);
        dialogLayout.add(cards);

        dialogLayout.add(new H4("Historia operacji"));

        Grid<Wypozyczenie> historyGrid = new Grid<>();
        historyGrid.addColumn(Wypozyczenie::getDataWypozyczenia)
                .setHeader("Data wypożyczenia").setAutoWidth(true);

        historyGrid.addColumn(w -> w.getWypozyczoneKsiazki().stream()
                        .map(wk -> wk.getKsiazka().getDaneKsiazki().getTytul())
                        .collect(Collectors.joining(", ")))
                .setHeader("Tytuły").setAutoWidth(true);

        historyGrid.addColumn(w -> w.getDataOddania() != null ? w.getDataOddania().toString() : "W trakcie")
                .setHeader("Data zwrotu").setAutoWidth(true);

        historyGrid.addColumn(w -> String.format("%.2f zł", w.getKara()))
                .setHeader("Kara");

        historyGrid.setItems(historia);
        historyGrid.setHeight("250px");
        historyGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);

        if(historia.isEmpty()) {
            dialogLayout.add(new Span("Brak historii wypożyczeń."));
        } else {
            dialogLayout.add(historyGrid);
        }

        Button closeButton = new Button("Zamknij", e -> dialog.close());
        dialog.getFooter().add(closeButton);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private VerticalLayout createStatCard(String title, String value, VaadinIcon icon) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(true);
        card.setWidth("33%");
        card.setAlignItems(Alignment.CENTER);

        card.getStyle().set("background-color", "#ffffff");
        card.getStyle().set("border", "1px solid #e0e0e0");
        card.getStyle().set("border-radius", "8px");
        card.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)");

        Span iconSpan = new Span(icon.create());
        iconSpan.getStyle().set("color", "var(--lumo-primary-color)");
        iconSpan.getStyle().set("font-size", "1.5em");
        iconSpan.getStyle().set("margin-bottom", "5px");

        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("color", "gray");
        titleSpan.getStyle().set("font-size", "0.85em");
        titleSpan.getStyle().set("text-transform", "uppercase");
        titleSpan.getStyle().set("letter-spacing", "0.05em");
        titleSpan.getStyle().set("text-align", "center");

        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("font-size", "1.2em");
        valueSpan.getStyle().set("font-weight", "600");
        valueSpan.getStyle().set("color", "var(--lumo-body-text-color)");

        card.add(iconSpan, titleSpan, valueSpan);
        return card;
    }

    private void configureForm() {
        form = new UzytkownicyForm();
        form.setWidth("25em");
        form.addListener(UzytkownicyForm.SaveEvent.class, this::saveUzytkownik);
        form.addListener(UzytkownicyForm.DeleteEvent.class, this::deleteUzytkownik);
        form.addListener(UzytkownicyForm.CloseEvent.class, e -> closeEditor());
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Wyszukaj...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        Button addBtn = new Button("Dodaj użytkownika");
        addBtn.addClickListener(click -> addUzytkownik());

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addBtn);
        toolbar.addClassName("uzytkownicy-toolbar");
        return toolbar;
    }

    private Component getContent() {
        HorizontalLayout content = new HorizontalLayout(grid, form);
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, form);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private void saveUzytkownik(UzytkownicyForm.SaveEvent event) {
        userService.saveUzytkownik(event.getUzytkownik());
        updateList();
        closeEditor();
    }

    private void deleteUzytkownik(UzytkownicyForm.DeleteEvent event) {
        userService.deleteUzytkownik(event.getUzytkownik());
        updateList();
        closeEditor();
    }

    public void editUzytkownik(Uzytkownik uzytkownik) {
        if (uzytkownik == null) {
            closeEditor();
        } else {
            form.setUzytkownik(uzytkownik);
            form.setVisible(true);
            addClassName("editing");
        }
    }

    private void addUzytkownik() {
        grid.asSingleSelect().clear();
        editUzytkownik(new Uzytkownik());
    }

    private void closeEditor() {
        form.setUzytkownik(null);
        form.setVisible(false);
        removeClassName("editing");
    }

    private void updateList() {
        grid.setItems(userService.findAllUzytkownicy(filterText.getValue()));
    }
}