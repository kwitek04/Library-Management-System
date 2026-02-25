package com.example.application.views;

import com.example.application.views.dziedziny.DziedzinaView;
import com.example.application.views.kary.KaryView;
import com.example.application.views.kontrolastanu.KontrolaStanuView;
import com.example.application.views.ksiazki.KsiazkiView;
import com.example.application.views.mojekonto.MojeKontoView;
import com.example.application.views.oczekujacekonta.OczekujaceKontaView;
import com.example.application.views.odlozenie.KsiazkiDoOdlozeniaView;
import com.example.application.views.pracownicy.PracownicyView;
import com.example.application.views.rezerwacje.MojeRezerwacjeView;
import com.example.application.views.statystyki.StatystykiView;
import com.example.application.views.uzytkownicy.UzytkownicyView;
import com.example.application.views.wycofanie.KsiazkiDoWycofaniaView;
import com.example.application.views.wypozyczenia.MojeWypozyczeniaView;
import com.example.application.views.wypozyczenia.ZarzadzanieWypozyczeniamiView;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.spring.security.AuthenticationContext;

public class MainLayout extends AppLayout {

    private final AuthenticationContext authContext;

    public MainLayout(AuthenticationContext authContext) {
        this.authContext = authContext;

        createHeader();
        createDrawer();
    }

    private void createHeader() {
        RouterLink logo = new RouterLink("Wypożyczalnia książek", com.example.application.views.HomeView.class);

        logo.addClassNames("text-2xl", "m-m", "logo-link");
        logo.getStyle().set("position", "absolute");
        logo.getStyle().set("left", "50%");
        logo.getStyle().set("transform", "translateX(-50%)");
        logo.getStyle().set("margin", "0");
        logo.getStyle().set("font-weight", "bold");
        logo.getStyle().set("text-decoration", "none");
        logo.getStyle().set("color", "var(--lumo-body-text-color)");

        Div themeToggle = new Div();
        themeToggle.addClassName("theme-toggle");
        themeToggle.getElement().setAttribute("title", "Zmień motyw");

        Icon sunIcon = VaadinIcon.SUN_O.create();
        sunIcon.addClassName("toggle-icon-sun");

        Icon moonIcon = VaadinIcon.MOON.create();
        moonIcon.addClassName("toggle-icon-moon");

        Div themeThumb = new Div();
        themeThumb.addClassName("theme-toggle-thumb");

        themeToggle.add(sunIcon, moonIcon, themeThumb);

        themeToggle.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.getPage().executeJs(
                    "const isDark = document.documentElement.getAttribute('theme') === 'dark';" +
                            "if (isDark) {" +
                            "  document.documentElement.removeAttribute('theme');" +
                            "  localStorage.setItem('theme', 'light');" +
                            "} else {" +
                            "  document.documentElement.setAttribute('theme', 'dark');" +
                            "  localStorage.setItem('theme', 'dark');" +
                            "}" +
                            "return !isDark;"
            ).then(Boolean.class, isDark -> {
                if (isDark) {
                    themeToggle.addClassName("dark");
                } else {
                    themeToggle.removeClassName("dark");
                }
            }));
        });

        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const theme = localStorage.getItem('theme');" +
                        "if (theme === 'dark') {" +
                        "  document.documentElement.setAttribute('theme', 'dark');" +
                        "  return true;" +
                        "} return false;"
        ).then(Boolean.class, isDark -> {
            if (Boolean.TRUE.equals(isDark)) {
                themeToggle.addClassName("dark");
            }
        }));

        Div buttons = new Div();
        buttons.getStyle().set("display", "flex");
        buttons.getStyle().set("align-items", "center");

        buttons.add(themeToggle);

        if (authContext.isAuthenticated()) {
            Button logout = new Button("Wyloguj się", new Icon(VaadinIcon.SIGN_OUT), e -> authContext.logout());
            logout.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            buttons.add(logout);
        } else {
            Button login = new Button("Zaloguj się", e -> getUI().ifPresent(ui -> ui.navigate("login")));
            Button register = new Button("Załóż konto", e -> getUI().ifPresent(ui -> ui.navigate("rejestracja")));
            login.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            register.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            register.getStyle().set("margin-left", "10px");
            buttons.add(login, register);
        }

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), buttons);

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();

        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.addClassNames("py-0", "px-m");

        addToNavbar(header, logo);
    }

    private void createDrawer() {
        SideNav nav = new SideNav();

        nav.addItem(new SideNavItem("Strona główna", com.example.application.views.HomeView.class, VaadinIcon.HOME.create()));
        nav.addItem(new SideNavItem("Katalog", com.example.application.views.katalog.KatalogView.class, VaadinIcon.BOOK.create()));

        if (authContext.isAuthenticated()) {
            nav.addItem(new SideNavItem("Moje konto", MojeKontoView.class, VaadinIcon.USER.create()));
        }

        if (authContext.isAuthenticated() && authContext.hasRole("USER")) {
            nav.addItem(new SideNavItem("Moje wypożyczenia", MojeWypozyczeniaView.class, VaadinIcon.ARCHIVE.create()));
            nav.addItem(new SideNavItem("Moje rezerwacje", MojeRezerwacjeView.class, VaadinIcon.CLOCK.create()));
            nav.addItem(new SideNavItem("Kary i opłaty", KaryView.class, VaadinIcon.MONEY.create()));
        }

        if (authContext.isAuthenticated() && (authContext.hasRole("BIBLIOTEKARZ"))) {
            nav.addItem(new SideNavItem("Zarządzanie Książkami", KsiazkiView.class, VaadinIcon.BOOK.create()));
            nav.addItem(new SideNavItem("Zarządzanie Użytkownikami", UzytkownicyView.class, VaadinIcon.USERS.create()));
            nav.addItem(new SideNavItem("Zarządzanie wypożyczeniami", ZarzadzanieWypozyczeniamiView.class, VaadinIcon.EXCHANGE.create()));
            nav.addItem(new SideNavItem("Konta do aktywacji", OczekujaceKontaView.class, VaadinIcon.CHECK_SQUARE_O.create()));
        }

        if (authContext.isAuthenticated() && authContext.hasRole("KIEROWNIK")) {
            nav.addItem(new SideNavItem("Zarządzanie pracownikami", PracownicyView.class, VaadinIcon.BRIEFCASE.create()));
            nav.addItem(new SideNavItem("Książki do wycofania", KsiazkiDoWycofaniaView.class, VaadinIcon.TRASH.create()));
            nav.addItem(new SideNavItem("Statystyki globalne", StatystykiView.class, VaadinIcon.CHART.create()));
            nav.addItem(new SideNavItem("Dziedziny i poddziedziny", DziedzinaView.class, VaadinIcon.SITEMAP.create()));
        }

        if (authContext.isAuthenticated() && (authContext.hasRole("MAGAZYNIER"))) {
            nav.addItem(new SideNavItem("Kontrola stanu", KontrolaStanuView.class, VaadinIcon.CLIPBOARD_CHECK.create()));
            nav.addItem(new SideNavItem("Książki do odłożenia", KsiazkiDoOdlozeniaView.class, VaadinIcon.INBOX.create()));
        }

        nav.getStyle().set("padding", "10px");

        addToDrawer(nav);
    }
}