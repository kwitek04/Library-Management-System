package com.example.application.views.rejestracja;

import com.example.application.data.entity.Uzytkownik;
import com.example.application.data.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.Locale;

@AnonymousAllowed
@Route("rejestracja")
@PageTitle("Załóż konto | Wypożyczalnia książek")
public class RejestracjaView extends VerticalLayout {

    private final UserService userService;
    private final Binder<Uzytkownik> binder = new BeanValidationBinder<>(Uzytkownik.class);

    TextField imie = new TextField("Imię");
    TextField nazwisko = new TextField("Nazwisko");
    EmailField email = new EmailField("Email");
    PasswordField password = new PasswordField("Hasło");
    TextField nrTelefonu = new TextField("Numer telefonu");
    DatePicker dataUrodzenia = new DatePicker("Data urodzenia");

    public RejestracjaView(UserService userService) {
        this.userService = userService;

        addClassName("rejestracja-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        FormLayout formLayout = new FormLayout();
        formLayout.setMaxWidth("400px");

        formLayout.getStyle().set("margin", "0 auto");
        setAlignItems(Alignment.CENTER);


        dataUrodzenia.setLocale(new Locale("pl", "PL"));

        binder.bindInstanceFields(this);

        Button registerButton = new Button("Zarejestruj się", e -> zarejestruj());
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        formLayout.add(imie, nazwisko, dataUrodzenia, nrTelefonu, email, password,
                registerButton);
        H1 title = new H1("Załóż konto");

        add(title, formLayout);

        binder.setBean(new Uzytkownik());
    }

    private void zarejestruj() {
        if (binder.isValid()) {
            userService.saveUzytkownik(binder.getBean());
            Notification.show("Konto utworzone! Logowanie będzie możliwe po aktywacji konta przez pracownika.", 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            getUI().ifPresent(ui -> ui.navigate("login"));
        }
    }
}