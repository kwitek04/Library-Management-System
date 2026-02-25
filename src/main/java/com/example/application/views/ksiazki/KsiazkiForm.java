package com.example.application.views.ksiazki;

import com.example.application.data.entity.*;
import com.example.application.data.service.BookService;
import com.example.application.data.service.RentalService;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Komponent formularza służący do tworzenia i edycji obiektów książek.
 * Odpowiada za wyświetlanie pól edycji, walidację danych wejściowych, obsługę przesyłania okładek
 * oraz komunikację z warstwą serwisową w celu zapisu zmian.
 */
public class KsiazkiForm extends FormLayout {

    private final BookService bookService;
    private final RentalService rentalService;

    // Komponenty interfejsu użytkownika
    private final Div wycofanieInfo = new Div();
    private final MemoryBuffer buffer = new MemoryBuffer();
    private final Upload upload = new Upload(buffer);
    private final Image previewImage = new Image();

    private final TextField isbn = new TextField("ISBN");
    private final TextField tytul = new TextField("Tytuł");
    private final TextField wydawnictwo = new TextField("Wydawnictwo");
    private final IntegerField rokWydania = new IntegerField("Rok wydania");
    private final TextArea opis = new TextArea("Opis książki");
    private final MultiSelectComboBox<Autor> autorzy = new MultiSelectComboBox<>("Autorzy");
    private final MultiSelectComboBox<Tlumacz> tlumacze = new MultiSelectComboBox<>("Tłumacze");

    private final ComboBox<StanFizyczny> stanFizyczny = new ComboBox<>("Stan fizyczny");
    private final TextField statusField = new TextField("Status");

    private final ComboBox<Dziedzina> dziedzina = new ComboBox<>("Dziedzina");
    private final ComboBox<Poddziedzina> poddziedzina = new ComboBox<>("Poddziedzina");
    private final NumberField cena = new NumberField("Cena (PLN)");

    private final Button save = new Button("Zapisz");
    private final Button delete = new Button("Usuń");
    private final Button close = new Button("Anuluj");

    // Mechanizm łączący pola formularza z danymi książki
    private final Binder<Ksiazka> binder = new BeanValidationBinder<>(Ksiazka.class);
    private final Binder<DaneKsiazki> daneBinder = new BeanValidationBinder<>(DaneKsiazki.class);

    /**
     * Konstruktor formularza inicjalizujący komponenty i wiązania danych.
     *
     * @param bookService serwis do zarządzania danymi książek i danymi słownikowymi
     * @param rentalService serwis do sprawdzania statusu wypożyczeń i wycofań
     */
    public KsiazkiForm(BookService bookService, RentalService rentalService) {
        this.bookService = bookService;
        this.rentalService = rentalService;

        addClassName("ksiazka-form");

        configureWycofanieInfo();
        configureUpload();
        configureFields();
        configureBinders();

        add(wycofanieInfo, createUploadLayout(), isbn, tytul, autorzy, tlumacze, wydawnictwo, rokWydania, cena, opis,
                dziedzina, poddziedzina, stanFizyczny, statusField,
                createButtonsLayout());
    }

    /**
     * Konfiguruje panel informacyjny wyświetlany dla wycofanych książek.
     */
    private void configureWycofanieInfo(){
        wycofanieInfo.setVisible(false);
        wycofanieInfo.getStyle().set("background-color", "#ffe0e0")
                .set("color", "#d32f2f")
                .set("padding", "15px")
                .set("border-radius", "5px")
                .set("margin-bottom", "20px")
                .set("border", "1px solid #ffcdd2");
        setColspan(wycofanieInfo, 2);
    }

    /**
     * Konfiguruje komponent przesyłania plików (zdjęć okładek książek).
     * Ustawia akceptowane typy plików oraz obsługę zdarzenia zakończenia przesyłania.
     */
    private void configureUpload(){
        upload.setAcceptedFileTypes("image/jpeg", "image/png");
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.setDropLabel(new com.vaadin.flow.component.html.Span("Upuść okładkę tutaj (max 5MB)"));

        previewImage.setWidth("150px");
        previewImage.setVisible(false);

        upload.addSucceededListener(event -> {
            try {
                byte[] imageBytes = buffer.getInputStream().readAllBytes();
                DaneKsiazki currentDane = daneBinder.getBean();
                if (currentDane != null) {
                    currentDane.setOkladka(imageBytes);
                    showImage(imageBytes);
                }
            } catch (IOException e) {
                Notification.show("Błąd podczas wczytywania obrazka");
            }
        });
    }

    /**
     * Tworzy układ dla komponentu przesyłania i podglądu okładki.
     */
    private VerticalLayout createUploadLayout() {
        VerticalLayout layout = new VerticalLayout(upload, previewImage);
        layout.setPadding(false);
        layout.setSpacing(true);
        setColspan(layout, 2);
        return layout;
    }

    /**
     * Konfiguruje pola tekstowe i listy rozwijane formularza.
     * Ładuje dane słownikowe (autorzy, dziedziny) oraz ustawia listenery dla dynamicznego dodawania wartości.
     */
    private void configureFields(){
        List<Dziedzina> dziedziny = bookService.findAllDziedziny();
        List<Autor> dostepniAutorzy = bookService.findAllAutorzy();
        List<Tlumacz> dostepniTlumacze = bookService.findAllTlumacze();

        isbn.setRequired(true);
        tytul.setRequired(true);
        wydawnictwo.setRequired(true);
        rokWydania.setRequiredIndicatorVisible(true);

        stanFizyczny.setRequired(true);
        stanFizyczny.setItems(StanFizyczny.values());
        stanFizyczny.setItemLabelGenerator(StanFizyczny::getNazwa);
        stanFizyczny.setPlaceholder("Wybierz stan...");

        statusField.setReadOnly(true);
        statusField.addThemeName("align-center");

        dziedzina.setRequired(true);
        dziedzina.setItems(dziedziny);
        dziedzina.setItemLabelGenerator(Dziedzina::getNazwa);
        dziedzina.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                poddziedzina.setItems(e.getValue().getPoddziedziny());
                poddziedzina.setEnabled(true);
            } else {
                poddziedzina.setEnabled(false);
                poddziedzina.clear();
            }
        });

        poddziedzina.setRequired(false);
        poddziedzina.setItemLabelGenerator(Poddziedzina::getNazwa);
        poddziedzina.setEnabled(false);

        autorzy.setRequired(true);
        autorzy.setItems(dostepniAutorzy);
        autorzy.setItemLabelGenerator(autor -> autor.getImie() + " " + autor.getNazwisko());
        autorzy.setPlaceholder("Dodaj autora...");
        autorzy.setHelperText("Aby dodać nowego autora, wpisz imię i nazwisko, a następnie wciśnij Enter.");
        autorzy.setAllowCustomValue(true);
        autorzy.addCustomValueSetListener(e -> addNewAutor(e.getDetail()));

        tlumacze.setItems(dostepniTlumacze);
        tlumacze.setItemLabelGenerator(t -> t.getImie() + " " + t.getNazwisko());
        tlumacze.setPlaceholder("Dodaj tłumacza (opcjonalne)...");
        tlumacze.setClearButtonVisible(true);
        tlumacze.setAllowCustomValue(true);
        tlumacze.addCustomValueSetListener(e -> addNewTlumacz(e.getDetail()));

        opis.setPlaceholder("Podaj opis książki...");
        opis.setClearButtonVisible(true);
        opis.setHeight("150px");
        setColspan(opis, 2);

        cena.setRequiredIndicatorVisible(true);
        cena.setMin(0);
        cena.setValue(40.0); // Wartość domyślna
        Div zlSuffix = new Div();
        zlSuffix.setText("zł");
        cena.setSuffixComponent(zlSuffix);
    }

    /**
     * Dodaje nowego autora do bazy danych na podstawie wpisanej wartości.
     * Aktualizuje listę dostępnych autorów w komponencie MultiSelectComboBox.
     */
    private void addNewAutor(String wpisanaWartosc) {
        String[] czesci = wpisanaWartosc.trim().split(" ", 2);
        String imie = czesci[0];
        String nazwisko = czesci.length > 1 ? czesci[1] : "";

        if (!imie.isEmpty()) {
            Autor nowyAutor = new Autor(imie, nazwisko);
            bookService.saveAutor(nowyAutor);

            Set<Autor> aktualnieWybrani = new HashSet<>(autorzy.getValue());
            aktualnieWybrani.add(nowyAutor);

            autorzy.setItems(bookService.findAllAutorzy());
            autorzy.setValue(aktualnieWybrani);
            Notification.show("Dodano autora: " + imie + " " + nazwisko);
        }
    }

    /**
     * Dodaje nowego tłumacza do bazy danych na podstawie wpisanej wartości.
     * Aktualizuje listę dostępnych tłumaczów w komponencie MultiSelectComboBox.
     */
    private void addNewTlumacz(String wpisanaWartosc) {
        String[] czesci = wpisanaWartosc.trim().split(" ", 2);
        String imie = czesci[0];
        String nazwisko = czesci.length > 1 ? czesci[1] : "";

        if (!imie.isEmpty()) {
            Tlumacz nowy = new Tlumacz(imie, nazwisko);
            bookService.saveTlumacz(nowy);

            Set<Tlumacz> aktualnieWybrani = new HashSet<>(tlumacze.getValue());
            aktualnieWybrani.add(nowy);

            tlumacze.setItems(bookService.findAllTlumacze());
            tlumacze.setValue(aktualnieWybrani);
            Notification.show("Dodano tłumacza: " + imie + " " + nazwisko);
        }
    }

    /**
     * Konfiguruje mechanizm wiązania pól formularza z polami obiektów.
     * Definiuje reguły walidacji dla poszczególnych pól.
     */
    private void configureBinders(){
        binder.forField(stanFizyczny)
                .asRequired("Stan fizyczny jest wymagany")
                .bind(Ksiazka::getStanFizyczny, Ksiazka::setStanFizyczny);

        daneBinder.forField(cena)
                .asRequired("Cena jest wymagana")
                .bind(DaneKsiazki::getCena, DaneKsiazki::setCena);

        daneBinder.forField(isbn)
                .asRequired("ISBN jest wymagany")
                .withValidator(s -> {
                    String clean = s.replaceAll("[^0-9X]", "");
                    return clean.length() == 10 || clean.length() == 13;
                }, "ISBN musi mieć 10 lub 13 cyfr (myślniki i spacje są dozwolone)")
                .bind(DaneKsiazki::getIsbn, DaneKsiazki::setIsbn);

        daneBinder.forField(tytul)
                .asRequired("Tytuł jest wymagany")
                .bind(DaneKsiazki::getTytul, DaneKsiazki::setTytul);

        daneBinder.forField(wydawnictwo)
                .asRequired("Wydawnictwo jest wymagane")
                .bind(DaneKsiazki::getWydawnictwo, DaneKsiazki::setWydawnictwo);

        daneBinder.forField(rokWydania)
                .asRequired("Rok wydania jest wymagany")
                .bind(DaneKsiazki::getRokWydania, DaneKsiazki::setRokWydania);

        daneBinder.forField(autorzy)
                .asRequired("Przynajmniej jeden autor jest wymagany")
                .bind(DaneKsiazki::getAutorzy, DaneKsiazki::setAutorzy);

        daneBinder.forField(tlumacze).bind(DaneKsiazki::getTlumacze, DaneKsiazki::setTlumacze);

        binder.bindInstanceFields(this);
        daneBinder.bindInstanceFields(this);
    }

    private HorizontalLayout createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, binder.getBean())));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));

        return new HorizontalLayout(save, delete, close);
    }

    /**
     * Waliduje dane w formularzu i emituje zdarzenie zapisu, jeśli dane są poprawne.
     */
    private void validateAndSave() {
        try {
            binder.writeBean(binder.getBean());
            daneBinder.writeBean(daneBinder.getBean());

            fireEvent(new SaveEvent(this, binder.getBean()));
        } catch (ValidationException e) {
            Notification.show("Sprawdź poprawność danych w formularzu")
                    .addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
        }
    }

    /**
     * Ustawia obiekt książki do edycji w formularzu.
     * Dostosowuje widok w zależności od stanu książki.
     *
     * @param ksiazka obiekt książki do edycji
     */
    public void setKsiazka(Ksiazka ksiazka) {
        upload.clearFileList();
        previewImage.setVisible(false);
        wycofanieInfo.setVisible(false);
        wycofanieInfo.removeAll();

        save.setEnabled(true);
        delete.setEnabled(true);
        stanFizyczny.setReadOnly(false);

        if (ksiazka != null && ksiazka.getStatus() != null) {
            statusField.setValue(ksiazka.getStatus().getName());
        } else {
            statusField.setValue("");
        }

        // Blokada edycji dla książek wycofanych
        if (ksiazka != null && StatusKsiazki.WYCOFANA.equals(ksiazka.getStatus())) {
            save.setEnabled(false);
            delete.setEnabled(false);

            Wycofanie wycofanie = rentalService.findWycofanieByKsiazka(ksiazka);
            if (wycofanie != null) {
                wycofanieInfo.setVisible(true);
                wycofanieInfo.add(new Span("Książka została wycofana"));
                wycofanieInfo.add(new Div(new Span("Data: " + wycofanie.getDataWycofania())));
                wycofanieInfo.add(new Div(new Span("Przez: " + wycofanie.getPracownik().getImie() + " " + wycofanie.getPracownik().getNazwisko())));

                Span powodSpan = new Span("Powód: " + wycofanie.getPowod());
                powodSpan.getStyle().set("font-weight", "bold");
                wycofanieInfo.add(new Div(powodSpan));
            } else {
                wycofanieInfo.setVisible(true);
                wycofanieInfo.add(new Span("Książka ma status WYCOFANA (brak szczegółów w historii). Edycja zablokowana."));
            }
        }

        // Logika ustawiania dziedziny i poddziedziny
        if (ksiazka != null && ksiazka.getPoddziedzina() != null) {
            Dziedzina parentDziedzina = ksiazka.getPoddziedzina().getDziedzina();

            dziedzina.setValue(parentDziedzina);

            if (parentDziedzina != null) {
                poddziedzina.setItems(parentDziedzina.getPoddziedziny());
                poddziedzina.setEnabled(true);
            }
        } else {
            if (ksiazka != null && ksiazka.getId() == null) {
                dziedzina.clear();
                poddziedzina.clear();
                poddziedzina.setItems(Collections.emptyList());
                poddziedzina.setEnabled(false);
            }
        }

        binder.setBean(ksiazka);

        if (ksiazka != null && ksiazka.getDaneKsiazki() != null) {
            daneBinder.setBean(ksiazka.getDaneKsiazki());
            showImage(ksiazka.getDaneKsiazki().getOkladka());
        } else {
            daneBinder.setBean(null);
        }
    }

    private void showImage(byte[] imageBytes) {
        if (imageBytes != null && imageBytes.length > 0) {
            StreamResource resource = new StreamResource("cover", () -> new ByteArrayInputStream(imageBytes));
            previewImage.setSrc(resource);
            previewImage.setVisible(true);
        } else {
            previewImage.setVisible(false);
        }
    }

    public static abstract class KsiazkaFormEvent extends ComponentEvent<KsiazkiForm> {
        private Ksiazka ksiazka;
        protected KsiazkaFormEvent(KsiazkiForm source, Ksiazka ksiazka) {
            super(source, false);
            this.ksiazka = ksiazka;
        }
        public Ksiazka getKsiazka() { return ksiazka; }
    }

    public static class SaveEvent extends KsiazkaFormEvent {
        SaveEvent(KsiazkiForm source, Ksiazka ksiazka) { super(source, ksiazka); }
    }
    public static class DeleteEvent extends KsiazkaFormEvent {
        DeleteEvent(KsiazkiForm source, Ksiazka ksiazka) { super(source, ksiazka); }
    }
    public static class CloseEvent extends KsiazkaFormEvent {
        CloseEvent(KsiazkiForm source) { super(source, null); }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType, ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}