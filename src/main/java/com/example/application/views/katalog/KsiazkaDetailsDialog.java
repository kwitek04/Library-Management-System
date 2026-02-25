package com.example.application.views.katalog;

import com.example.application.data.entity.Ksiazka;
import com.example.application.data.entity.StatusKsiazki;
import com.example.application.data.entity.Uzytkownik;
import com.example.application.data.service.RentalService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;

/**
 * Komponent okna dialogowego prezentujący szczegółowe informacje o wybranej książce.
 * Umożliwia zalogowanym użytkownikom wypożyczenie lub zarezerwowanie książki.
 */
public class KsiazkaDetailsDialog extends Dialog {

    private final RentalService rentalService;
    private final Uzytkownik currentUser;
    private final Ksiazka ksiazka;

    public KsiazkaDetailsDialog(Ksiazka ksiazka, RentalService rentalService, Uzytkownik currentUser) {
        this.ksiazka = ksiazka;
        this.rentalService = rentalService;
        this.currentUser = currentUser;

        setHeaderTitle("Szczegóły książki");
        Button closeButton = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        getHeader().add(closeButton);

        setWidth("800px");
        setMaxWidth("90vw");

        VerticalLayout leftColumn = new VerticalLayout();
        leftColumn.setWidth("35%");
        leftColumn.setAlignItems(FlexComponent.Alignment.CENTER);

        Image coverImage = createCoverImage(ksiazka);
        coverImage.setWidth("100%");
        coverImage.getStyle().set("border-radius", "8px").set("box-shadow", "0 4px 12px rgba(0,0,0,0.15)");

        boolean czyDostepna = StatusKsiazki.DOSTEPNA.equals(ksiazka.getStatus());
        String statusText = czyDostepna ? "Dostępna" : "Niedostępna";

        Span statusBadge = new Span(statusText);
        statusBadge.getElement().getThemeList().add("badge " + (czyDostepna ? "success" : "error"));
        statusBadge.getStyle().set("margin-top", "15px").set("font-size", "1.1em").set("padding", "0.5em 1em");

        leftColumn.add(coverImage, statusBadge);

        boolean isStaff = isCurrentUserStaff();

        if (isStaff) {
            Span counterBadge = new Span("Licznik wypożyczeń: " + ksiazka.getLicznikWypozyczen());
            counterBadge.getElement().getThemeList().add("badge contrast");
            counterBadge.getStyle().set("margin-top", "5px");
            leftColumn.add(counterBadge);
        }

        VerticalLayout rightColumn = new VerticalLayout();
        rightColumn.setWidth("65%");
        rightColumn.getStyle().set("overflow-wrap", "anywhere");

        H2 tytul = new H2(ksiazka.getDaneKsiazki().getTytul());
        tytul.getStyle().set("margin-top", "0");

        String autorzyStr = ksiazka.getDaneKsiazki().getAutorzy().stream()
                .map(a -> a.getImie() + " " + a.getNazwisko())
                .collect(Collectors.joining(", "));
        H4 autor = new H4(autorzyStr);
        autor.getStyle().set("color", "#555").set("margin-top", "0");

        Span details = new Span(
                "Wydawnictwo: " + ksiazka.getDaneKsiazki().getWydawnictwo() + " | " +
                        "Rok: " + ksiazka.getDaneKsiazki().getRokWydania() + " | " +
                        "ISBN: " + ksiazka.getDaneKsiazki().getIsbn()
        );
        details.getStyle().set("font-size", "0.9em").set("color", "#777");

        Paragraph opis = new Paragraph(ksiazka.getDaneKsiazki().getOpis() != null ?
                ksiazka.getDaneKsiazki().getOpis() : "Brak opisu dla tej pozycji.");
        opis.getStyle().set("text-align", "justify").set("line-height", "1.6").set("white-space", "pre-wrap");

        Button btnWypozycz = new Button("Wypożycz", VaadinIcon.BOOK.create());
        btnWypozycz.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        boolean isAvailable = ksiazka.getStatus().getName().equalsIgnoreCase("Dostępna");
        boolean isLoggedIn = (currentUser != null);

        if (!isLoggedIn) {
            btnWypozycz.setEnabled(false);
            btnWypozycz.setTooltipText("Zaloguj się, aby wypożyczyć");
        } else if (!isAvailable) {
            btnWypozycz.setEnabled(false);
            btnWypozycz.setTooltipText("Książka jest obecnie niedostępna");
        } else {
            btnWypozycz.setEnabled(true);
        }

        btnWypozycz.addClickListener(e -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Potwierdzenie wypożyczenia");
            dialog.setText("Czy na pewno chcesz wypożyczyć książkę \"" + ksiazka.getDaneKsiazki().getTytul() + "\"?");
            dialog.setConfirmText("Wypożycz");
            dialog.setConfirmButtonTheme("primary");
            dialog.setCancelable(true);
            dialog.setCancelText("Anuluj");

            dialog.addConfirmListener(event -> {
                try {
                    rentalService.wypozyczKsiazke(ksiazka, currentUser);

                    Notification.show("Pomyślnie wypożyczono książkę!", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    this.close();

                } catch (IllegalStateException | IllegalArgumentException ex) {
                    Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);

                } catch (Exception ex) {
                    Notification.show("Wystąpił nieoczekiwany błąd serwera.", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });

            dialog.open();
        });

        Button btnRezerwuj = new Button("Zarezerwuj", VaadinIcon.CALENDAR_CLOCK.create());
        btnRezerwuj.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE);

        if (!isLoggedIn || !isAvailable) {
            btnRezerwuj.setEnabled(false);
        }

        btnRezerwuj.addClickListener(e -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setHeader("Potwierdź rezerwację");
            dialog.setText("Czy chcesz zarezerwować książkę \"" + ksiazka.getDaneKsiazki().getTytul() + "\"?\nBędziesz miał 3 dni na jej odbiór.");

            dialog.setConfirmText("Rezerwuję");
            dialog.setConfirmButtonTheme("primary");
            dialog.setCancelable(true);
            dialog.setCancelText("Anuluj");

            dialog.addConfirmListener(ev -> {
                try {
                    rentalService.zarezerwujKsiazke(ksiazka, currentUser);
                    Notification.show("Rezerwacja pomyślna! Masz 3 dni na odbiór.", 5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    this.close();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            dialog.open();
        });

        HorizontalLayout actions = new HorizontalLayout(btnWypozycz, btnRezerwuj);
        actions.setWidthFull();
        actions.setSpacing(true);
        actions.getStyle().set("margin-top", "auto");
        actions.setFlexGrow(1, btnWypozycz, btnRezerwuj);

        rightColumn.add(tytul, autor, details, opis, actions);
        rightColumn.setFlexGrow(1, opis);

        HorizontalLayout layout = new HorizontalLayout(leftColumn, rightColumn);
        layout.setSizeFull();
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);

        add(layout);
    }

    private Image createCoverImage(Ksiazka ksiazka) {
        byte[] okladka = ksiazka.getDaneKsiazki().getOkladka();
        if (okladka != null && okladka.length > 0) {
            StreamResource resource = new StreamResource("cover_" + ksiazka.getId(), () -> new ByteArrayInputStream(okladka));
            return new Image(resource, "Okładka");
        } else {
            return new Image("https://placehold.co/200x300?text=Brak+okładki", "Brak");
        }
    }

    private boolean isCurrentUserStaff() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_KIEROWNIK") ||
                        a.getAuthority().equals("ROLE_BIBLIOTEKARZ") ||
                        a.getAuthority().equals("ROLE_MAGAZYNIER"));
    }
}