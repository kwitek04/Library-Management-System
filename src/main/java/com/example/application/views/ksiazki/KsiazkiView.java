package com.example.application.views.ksiazki;

import com.example.application.data.entity.DaneKsiazki;
import com.example.application.data.entity.Ksiazka;
import com.example.application.data.entity.StatusKsiazki;
import com.example.application.data.entity.WypozyczonaKsiazka;
import com.example.application.data.service.BookService;
import com.example.application.data.service.RentalService;
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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Widok zarządzania książkami dostępny dla pracownika z rolą BIBLIOTEKARZ.
 * Umożliwia przeglądanie książęk, dodawanie nowych pozycji, edycję istniejących
 * oraz podgląd szczegółowych statystyk egzemplarza.
 */

@RolesAllowed("BIBLIOTEKARZ")
@Route(value = "ksiazki", layout = MainLayout.class)
@PageTitle("Lista książek | Wypożyczalnia książek")
public class KsiazkiView extends VerticalLayout {

    private final BookService bookService;
    private final RentalService rentalService;

    private final Grid<Ksiazka> grid = new Grid<>(Ksiazka.class);
    private final TextField filterText = new TextField();
    private KsiazkiForm form;

    public KsiazkiView(BookService bookService, RentalService rentalService) {
        this.bookService = bookService;
        this.rentalService = rentalService;

        addClassName("ksiazka-view");
        setSizeFull();
        configureGrid();
        configureForm();

        add(getToolbar(), getContent());
        updateList();
        closeEditor();
    }

    private Component getContent() {
        HorizontalLayout content = new HorizontalLayout(grid, form);
        content.setFlexGrow(2, grid);
        content.setFlexGrow(1, form);
        content.addClassName("content");
        content.setSizeFull();
        return content;
    }

    /**
     * Inicjalizuje formularz edycji książki i ustawia listenery zdarzeń.
     */
    private void configureForm() {
        form = new KsiazkiForm(bookService, rentalService);
        form.setWidth("25em");
        form.addListener(KsiazkiForm.SaveEvent.class, this::saveKsiazka);
        form.addListener(KsiazkiForm.DeleteEvent.class, this::deleteKsiazka);
        form.addListener(KsiazkiForm.CloseEvent.class, e -> closeEditor());
    }

    /**
     * Konfiguruje kolumny tabeli, ich nagłówki oraz sposób wyświetlania danych.
     */
    private void configureGrid() {
        grid.addClassNames("contact-grid");
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(ksiazka -> ksiazka.getDaneKsiazki().getTytul())
                .setHeader("Tytuł")
                .setSortable(true)
                .setWidth("300px")
                .setResizable(true)
                .setFlexGrow(0);

        grid.addColumn(ksiazka -> ksiazka.getDaneKsiazki().getAutorzy().stream()
                        .map(a -> a.getImie() + " " + a.getNazwisko())
                        .collect(Collectors.joining(", ")))
                .setHeader("Autor")
                .setSortable(true)
                .setWidth("250px")
                .setResizable(true)
                .setFlexGrow(1);

        grid.addColumn(ksiazka -> ksiazka.getDaneKsiazki().getIsbn())
                .setHeader("ISBN")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(ksiazka -> ksiazka.getStatus().getName())
                .setHeader("Status")
                .setSortable(true)
                .setAutoWidth(true);

        grid.addColumn(ksiazka -> ksiazka.getStanFizyczny().getNazwa())
                .setHeader("Stan fizyczny")
                .setSortable(true)
                .setAutoWidth(true);

        // Kolumna akcji (Statystyki i edytowanie)
        grid.addComponentColumn(ksiazka -> {
            Button statsBtn = new Button(VaadinIcon.CHART.create());
            statsBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            statsBtn.setTooltipText("Pokaż historię i statystyki");
            statsBtn.addClickListener(e -> openStatsDialog(ksiazka));

            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            editBtn.setTooltipText("Edytuj książkę"); // Dodany tooltip
            editBtn.addClickListener(e -> editKsiazka(ksiazka));

            return new HorizontalLayout(statsBtn, editBtn);
        }).setHeader("Akcje").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    private void openStatsDialog(Ksiazka ksiazka) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Statystyki egzemplarza");
        dialog.setWidth("800px");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);

        H3 title = new H3(ksiazka.getDaneKsiazki().getTytul());
        title.getStyle().set("margin-top", "0");

        Span isbnLabel = new Span("ISBN: " + ksiazka.getDaneKsiazki().getIsbn());
        isbnLabel.getStyle().set("color", "gray").set("font-size", "0.9em");

        dialogLayout.add(title, isbnLabel);

        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();
        cards.setSpacing(true);

        VerticalLayout card1 = createStatCard("Liczba wypożyczeń", String.valueOf(ksiazka.getLicznikWypozyczen()), VaadinIcon.BOOK);

        String kontrolaText = ksiazka.getDataOstatniejKontroli() != null
                ? ksiazka.getDataOstatniejKontroli().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                : "Brak danych";
        VerticalLayout card2 = createStatCard("Ostatnia kontrola", kontrolaText, VaadinIcon.SEARCH);

        VerticalLayout card3 = createStatCard("Stan fizyczny", ksiazka.getStanFizyczny().getNazwa(), VaadinIcon.CLIPBOARD_CHECK);

        cards.add(card1, card2, card3);
        dialogLayout.add(cards);

        dialogLayout.add(new H4("Historia wypożyczeń tego egzemplarza"));

        Grid<WypozyczonaKsiazka> historyGrid = new Grid<>();
        historyGrid.addColumn(wk -> wk.getWypozyczenie().getUzytkownik().getEmail())
                .setHeader("Użytkownik").setAutoWidth(true);

        historyGrid.addColumn(wk -> wk.getWypozyczenie().getDataWypozyczenia())
                .setHeader("Wypożyczono").setSortable(true);

        historyGrid.addColumn(wk -> wk.getWypozyczenie().getDataOddania() != null
                        ? wk.getWypozyczenie().getDataOddania()
                        : "W trakcie")
                .setHeader("Oddano").setSortable(true);

        List<WypozyczonaKsiazka> history = rentalService.findHistoriaKsiazki(ksiazka);
        historyGrid.setItems(history);
        historyGrid.setHeight("250px");
        historyGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);

        if(history.isEmpty()) {
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

        Span valueSpan = new Span(value);
        valueSpan.getStyle().set("font-size", "1.2em");
        valueSpan.getStyle().set("font-weight", "600");
        valueSpan.getStyle().set("color", "var(--lumo-body-text-color)");

        card.add(iconSpan, titleSpan, valueSpan);
        return card;
    }

    private Component getToolbar() {
        filterText.setPlaceholder("Wyszukaj...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(e -> updateList());

        Button addKsiazkaButton = new Button("Dodaj książkę");
        addKsiazkaButton.addClickListener(click -> addKsiazka());

        var toolbar = new HorizontalLayout(filterText, addKsiazkaButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void saveKsiazka(KsiazkiForm.SaveEvent event) {
        bookService.saveKsiazka(event.getKsiazka());
        updateList();
        closeEditor();
    }

    private void deleteKsiazka(KsiazkiForm.DeleteEvent event) {
        bookService.deleteKsiazka(event.getKsiazka());
        updateList();
        closeEditor();
    }

    public void editKsiazka(Ksiazka ksiazka) {
        if (ksiazka == null) {
            closeEditor();
        } else {
            form.setKsiazka(ksiazka);
            form.setVisible(true);
            addClassName("editing");
        }
    }

    private void addKsiazka() {
        grid.asSingleSelect().clear();
        Ksiazka nowaKsiazka = new Ksiazka();
        nowaKsiazka.setDaneKsiazki(new DaneKsiazki());
        nowaKsiazka.setStatus(StatusKsiazki.DOSTEPNA);
        editKsiazka(nowaKsiazka);
    }

    private void closeEditor() {
        form.setKsiazka(null);
        form.setVisible(false);
        removeClassName("editing");
    }

    private void updateList() {
        grid.setItems(bookService.findAllKsiazki(filterText.getValue()));
    }
}