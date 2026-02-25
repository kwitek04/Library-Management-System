package com.example.application.views.mojekonto;

import com.example.application.data.entity.Pracownik;
import com.example.application.data.entity.Uzytkownik;
import com.example.application.data.repository.PracownikRepository;
import com.example.application.data.repository.UzytkownikRepository;
import com.example.application.data.service.UserService;
import com.example.application.security.SecurityService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.userdetails.UserDetails;

@PermitAll
@Route(value = "konto", layout = MainLayout.class)
@PageTitle("Moje klonto | Wypożyczalnia książek")
public class MojeKontoView extends VerticalLayout {

    private final UserService userService;
    private Uzytkownik currentUser;
    private Pracownik currentWorker;

    public MojeKontoView(SecurityService securityService,
                         PracownikRepository pracownicyRepository,
                         UzytkownikRepository uzytkownicyRepository,
                         UserService userService) {
        this.userService = userService;

        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        UserDetails authenticatedUser = securityService.getAuthenticatedUser();
        String email = authenticatedUser.getUsername();
        String fullName = "";
        String phone = "";
        String role = "";

        Pracownik p = pracownicyRepository.findByEmail(email);
        if (p != null) {
            currentWorker = p;
            fullName = p.getImie() + " " + p.getNazwisko();
            phone = p.getNrTelefonu();
            role = "Pracownik (" + p.getRoleAsString() + ")";
        } else {
            Uzytkownik u = uzytkownicyRepository.findByEmail(email);
            if (u != null) {
                currentUser = u;
                fullName = u.getImie() + " " + u.getNazwisko();
                phone = u.getNrTelefonu();
                role = "Czytelnik";
            }
        }

        Avatar avatar = new Avatar(fullName);
        avatar.setWidth("100px");
        avatar.setHeight("100px");
        avatar.getStyle().set("font-weight", "bold");
        avatar.getStyle().set("font-size", "1.2rem");

        H2 header = new H2(fullName);
        header.getStyle().set("margin-top", "10px");
        header.getStyle().set("margin-bottom", "5px");

        VerticalLayout infoLayout = new VerticalLayout();
        infoLayout.setAlignItems(Alignment.CENTER);
        infoLayout.setSpacing(false);
        infoLayout.setPadding(false);

        infoLayout.add(
                createDetailRow(VaadinIcon.USER, "Rola: ", role),
                createDetailRow(VaadinIcon.ENVELOPE, "Email: ", email),
                createDetailRow(VaadinIcon.PHONE, "Telefon: ", phone)
        );

        Icon keyIcon = VaadinIcon.KEY.create();
        keyIcon.getStyle().set("transform", "rotate(180deg)");

        Button changePasswordBtn = new Button("Zmień hasło", keyIcon);
        changePasswordBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changePasswordBtn.getStyle().set("margin-top", "10px");
        changePasswordBtn.addClickListener(e -> openChangePasswordDialog());

        add(avatar, header, infoLayout, changePasswordBtn);
    }

    private HorizontalLayout createDetailRow(VaadinIcon icon, String label, String value) {
        HorizontalLayout row = new HorizontalLayout(icon.create(), new Span(label), new Span(value));
        row.setAlignItems(Alignment.CENTER);
        row.getStyle().set("margin-top", "5px");
        return row;
    }

    private void openChangePasswordDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Zmiana hasła");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setPadding(false);

        PasswordField newPassword = new PasswordField("Nowe hasło");
        PasswordField confirmPassword = new PasswordField("Powtórz hasło");

        newPassword.setWidthFull();
        confirmPassword.setWidthFull();
        newPassword.setValueChangeMode(ValueChangeMode.EAGER);
        confirmPassword.setValueChangeMode(ValueChangeMode.EAGER);

        class PasswordDto {
            String pass;
            String confirm;
        }
        Binder<PasswordDto> binder = new Binder<>(PasswordDto.class);

        binder.forField(newPassword)
                .asRequired("Hasło jest wymagane")
                .withValidator(pass -> pass.length() >= 6, "Hasło musi mieć min. 6 znaków")
                .withValidator(pass -> pass.matches(".*\\d.*"), "Hasło musi zawierać przynajmniej jedną cyfrę")
                .withValidator(pass -> pass.matches(".*[a-zA-Z].*"), "Hasło musi zawierać przynajmniej jedną literę")
                .bind(dto -> dto.pass, (dto, v) -> dto.pass = v);

        binder.forField(confirmPassword)
                .asRequired("Potwierdź hasło")
                .withValidator(pass -> {
                    String val1 = newPassword.getValue();
                    return pass != null && pass.equals(val1);
                }, "Hasła muszą być identyczne")
                .bind(dto -> dto.confirm, (dto, v) -> dto.confirm = v);

        newPassword.addValueChangeListener(e -> binder.validate());

        dialogLayout.add(newPassword, confirmPassword);
        dialog.add(dialogLayout);

        Button saveButton = new Button("Zmień hasło", e -> {
            if (binder.validate().isOk()) {
                try {
                    String newPass = newPassword.getValue();

                    if (currentUser != null) {
                        userService.updatePassword(currentUser, newPass);
                    } else if (currentWorker != null) {
                        userService.updatePassword(currentWorker, newPass);
                    } else {
                        throw new IllegalStateException("Nie znaleziono użytkownika do zmiany hasła");
                    }

                    Notification.show("Hasło zostało zmienione", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    dialog.close();
                } catch (Exception ex) {
                    Notification.show("Błąd: " + ex.getMessage(), 5000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Anuluj", e -> dialog.close());

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.open();
    }
}