package com.example.application.views;

import com.example.application.data.entity.Ksiazka;
import com.example.application.data.entity.Tlumacz;
import com.example.application.data.entity.Uzytkownik;
import com.example.application.data.service.BookService;
import com.example.application.data.service.RentalService;
import com.example.application.data.service.UserService;
import com.example.application.security.SecurityService;
import com.example.application.views.katalog.KsiazkaDetailsDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.core.userdetails.UserDetails;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Strona główna aplikacji.
 * Udostępnia wyszukiwarkę książek (po tytule, autorze lub ISBN)
 * oraz wyświetla listę dopasowanych pozycji w formie kart.
 */
@AnonymousAllowed
@Route(value = "", layout = MainLayout.class)
@PageTitle("Strona Główna | Wypożyczalnia książek")
@StyleSheet("https://cdn.jsdelivr.net/npm/swiper@11/swiper-bundle.min.css")
@JavaScript("https://cdn.jsdelivr.net/npm/swiper@11/swiper-bundle.min.js")
public class HomeView extends VerticalLayout {

    private final BookService bookService;
    private final UserService userService;
    private final RentalService rentalService;
    private final SecurityService securityService;

    private final Grid<Ksiazka> grid = new Grid<>(Ksiazka.class);
    private final TextField searchField = new TextField();
    private Uzytkownik currentUser;

    public HomeView(BookService bookService, UserService userService, RentalService rentalService, SecurityService securityService) {
        this.bookService = bookService;
        this.userService = userService;
        this.rentalService = rentalService;
        this.securityService = securityService;

        UserDetails userDetails = securityService.getAuthenticatedUser();
        if (userDetails != null) {
            this.currentUser = userService.findUzytkownikByEmail(userDetails.getUsername());
        }

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        addClassName("home-view");

        add(createBestsellersCarousel());

        HorizontalLayout searchBar = createSearchBar();
        searchBar.getStyle().set("margin-top", "0px");
        add(searchBar);

        configureGrid();
        add(grid);

        updateList();
    }

    private HorizontalLayout createSearchBar() {
        searchField.setPlaceholder("Szukaj po tytule, autorze, lub ISBN...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.getStyle().set("--vaadin-input-field-height", "var(--lumo-size-l)");
        searchField.getStyle().set("font-size", "var(--lumo-font-size-l)");
        searchField.setWidth("1000px");
        searchField.setMaxWidth("90vw");
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> updateList());

        Button searchButton = new Button("Szukaj");
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        searchButton.addClickListener(e -> updateList());

        HorizontalLayout layout = new HorizontalLayout(searchField, searchButton);
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.CENTER);
        layout.setAlignItems(Alignment.BASELINE);

        return layout;
    }

    private void configureGrid() {
        grid.addClassName("katalog-grid");
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addComponentColumn(ksiazka -> createBookCard(ksiazka));

        grid.addThemeVariants(GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_NO_BORDER);
        grid.addItemClickListener(event -> {
            Ksiazka clickedBook = event.getItem();
            new KsiazkaDetailsDialog(clickedBook, rentalService, currentUser).open();
        });
    }

    private HorizontalLayout createBookCard(Ksiazka ksiazka) {
        HorizontalLayout card = new HorizontalLayout();
        card.addClassName("book-card");
        card.setSpacing(true);
        card.setPadding(true);
        card.setWidthFull();
        card.setAlignItems(Alignment.START);

        Image coverImage;
        byte[] okladka = ksiazka.getDaneKsiazki().getOkladka();

        if (okladka != null && okladka.length > 0) {
            StreamResource resource = new StreamResource("cover_" + ksiazka.getId(), () -> new java.io.ByteArrayInputStream(okladka));
            coverImage = new Image(resource, "Okładka książki");
        } else {
            coverImage = new Image("https://placehold.co/100x150?text=Brak+okładki", "Brak okładki");
        }

        coverImage.setWidth("140px");
        coverImage.setHeight("210px");
        coverImage.getStyle().set("border-radius", "5px");
        coverImage.getStyle().set("box-shadow", "0 4px 6px rgba(0,0,0,0.1)");
        coverImage.getStyle().set("object-fit", "cover");
        coverImage.addClassName("book-cover-image");

        VerticalLayout details = new VerticalLayout();
        details.setSpacing(false);
        details.setPadding(false);


        H3 tytul = new H3(ksiazka.getDaneKsiazki().getTytul());
        tytul.getStyle().set("margin-top", "0");
        tytul.getStyle().set("margin-bottom", "5px");

        String autorzyStr = "Brak autora";
        if (ksiazka.getDaneKsiazki().getAutorzy() != null && !ksiazka.getDaneKsiazki().getAutorzy().isEmpty()) {
            autorzyStr = ksiazka.getDaneKsiazki().getAutorzy().stream()
                    .map(a -> a.getImie() + " " + a.getNazwisko())
                    .collect(Collectors.joining(", "));
        }
        Span autor = new Span(autorzyStr);
        autor.getStyle().set("font-weight", "bold");
        autor.getStyle().set("color", "#7f8c8d");

        Span wydawnictwoRok = new Span(
                ksiazka.getDaneKsiazki().getWydawnictwo() + " • " +
                        ksiazka.getDaneKsiazki().getRokWydania()
        );
        wydawnictwoRok.getStyle().set("font-size", "0.9em");
        wydawnictwoRok.getStyle().set("color", "#95a5a6");

        Span isbn = new Span("ISBN: " + ksiazka.getDaneKsiazki().getIsbn());
        isbn.getStyle().set("font-size", "0.9em");
        isbn.getStyle().set("color", "#95a5a6");

        Span tlumaczSpan = new Span();
        Set<Tlumacz> tlumaczeList = ksiazka.getDaneKsiazki().getTlumacze();

        if (tlumaczeList != null && !tlumaczeList.isEmpty()) {
            String tlumaczeStr = tlumaczeList.stream()
                    .map(t -> t.getImie() + " " + t.getNazwisko())
                    .collect(Collectors.joining(", "));

            tlumaczSpan.setText("Tłumacz: " + tlumaczeStr);
            tlumaczSpan.getStyle().set("font-size", "0.9em");
            tlumaczSpan.getStyle().set("color", "#95a5a6");
        }

        String kategoriaStr = "-";
        if (ksiazka.getPoddziedzina() != null) {
            kategoriaStr = ksiazka.getPoddziedzina().getDziedzina().getNazwa() + " > " + ksiazka.getPoddziedzina().getNazwa();
        }
        Span kategoria = new Span(kategoriaStr);
        kategoria.getElement().getThemeList().add("badge");
        kategoria.getElement().getThemeList().add("contrast");
        kategoria.getStyle().set("margin-top", "10px");

        details.add(tytul, autor, wydawnictwoRok, isbn, tlumaczSpan, kategoria);

        Div spacer = new Div();
        card.setFlexGrow(1, spacer);

        Span statusBadge = createStatusBadge(ksiazka);

        VerticalLayout rightSide = new VerticalLayout(spacer, statusBadge);
        rightSide.setSpacing(false);
        rightSide.setPadding(false);
        rightSide.setHeight("210px");
        rightSide.setAlignItems(Alignment.END);
        rightSide.setJustifyContentMode(JustifyContentMode.END);

        card.add(coverImage, details, spacer, rightSide);
        return card;
    }

    private Span createStatusBadge(Ksiazka ksiazka) {
        String statusName = ksiazka.getStatus().getName();
        boolean dostepna = "Dostępna".equalsIgnoreCase(statusName);

        String text = dostepna ? "Dostępna" : "Niedostępna";
        Span badge = new Span(text);

        badge.getElement().getThemeList().add("badge");
        if (dostepna) {
            badge.getElement().getThemeList().add("success");
        } else {
            badge.getElement().getThemeList().add("error");
        }

        badge.getStyle().set("font-size", "0.9em");
        badge.getStyle().set("padding", "0.5em 1em");

        return badge;
    }

    private com.vaadin.flow.component.Component createBestsellersCarousel() {
        VerticalLayout container = new VerticalLayout();
        container.setWidthFull();
        container.setPadding(false);
        container.setSpacing(false);
        container.setAlignItems(Alignment.CENTER);

        H3 title = new H3("Najpopularniejsze książki");
        title.getStyle().set("margin", "10px 0 10px 0");
        title.getStyle().set("text-align", "center");

        Div carouselWrapper = new Div();
        carouselWrapper.addClassName("carousel-wrapper");
        carouselWrapper.setWidthFull();

        HorizontalLayout carousel = new HorizontalLayout();
        carousel.addClassName("bestsellers-carousel");
        carousel.setId("bestsellers-carousel");
        carousel.setWidthFull();
        carousel.setSpacing(false);

        List<Ksiazka> bestsellers = bookService.findBestsellers();
        int listSize = bestsellers.size();

        if (listSize > 0) {
            for (int i = 0; i < 15; i++) {
                for (Ksiazka ksiazka : bestsellers) {
                    carousel.add(createCarouselCard(ksiazka));
                }
            }
        }

        Div prevButton = new Div(VaadinIcon.ANGLE_LEFT.create());
        prevButton.addClassNames("carousel-nav-btn", "prev-btn");

        Div nextButton = new Div(VaadinIcon.ANGLE_RIGHT.create());
        nextButton.addClassNames("carousel-nav-btn", "next-btn");

        carouselWrapper.add(prevButton, carousel, nextButton);
        container.add(title, carouselWrapper);

        if (listSize > 0) {
            int middleIndexOffset = 200 * (listSize * 7);

            com.vaadin.flow.component.UI.getCurrent().getPage().executeJs(
                    "setTimeout(() => {" +
                            "  const carousel = document.getElementById('bestsellers-carousel');" +
                            "  if(!carousel) return;" +
                            "  const cardWidth = 200;" +
                            "  let autoPlayTimer;" +

                            "  const updateCenter = () => {" +
                            "    const rect = carousel.getBoundingClientRect();" +
                            "    const center = rect.left + rect.width / 2;" +
                            "    let closestIdx = -1;" +
                            "    let closestDist = Infinity;" +
                            "    const cards = Array.from(carousel.querySelectorAll('.carousel-card'));" +

                            "    cards.forEach((card, idx) => {" +
                            "      const box = card.getBoundingClientRect();" +
                            "      const cardCenter = box.left + box.width / 2;" +
                            "      const dist = Math.abs(center - cardCenter);" +
                            "      if (dist < closestDist) {" +
                            "        closestDist = dist;" +
                            "        closestIdx = idx;" +
                            "      }" +
                            "    });" +

                            "    cards.forEach((card, idx) => {" +
                            "      card.classList.remove('active-card', 'side-1', 'side-2');" +
                            "      const stepsAway = Math.abs(closestIdx - idx);" +
                            "      if (stepsAway === 0) card.classList.add('active-card');" +
                            "      else if (stepsAway === 1) card.classList.add('side-1');" +
                            "      else card.classList.add('side-2');" +
                            "    });" +
                            "  };" +

                            "  carousel.addEventListener('scroll', updateCenter);" +
                            "  carousel.style.scrollBehavior = 'auto';" +
                            "  carousel.scrollLeft = " + middleIndexOffset + ";" +
                            "  updateCenter();" +
                            "  carousel.style.scrollBehavior = 'smooth';" +

                            "  function startAutoPlay() {" +
                            "    clearInterval(autoPlayTimer);" +
                            "    autoPlayTimer = setInterval(() => {" +
                            "      if (!carousel.matches(':hover')) {" +
                            "        carousel.scrollBy({ left: cardWidth, behavior: 'smooth' });" +
                            "      }" +
                            "    }, 5000);" +
                            "  }" +

                            "  const prevBtn = carousel.parentElement.querySelector('.prev-btn');" +
                            "  const nextBtn = carousel.parentElement.querySelector('.next-btn');" +
                            "  if(prevBtn) prevBtn.onclick = () => {" +
                            "    carousel.scrollBy({ left: -cardWidth, behavior: 'smooth' });" +
                            "    startAutoPlay();" +
                            "  };" +
                            "  if(nextBtn) nextBtn.onclick = () => {" +
                            "    carousel.scrollBy({ left: cardWidth, behavior: 'smooth' });" +
                            "    startAutoPlay();" +
                            "  };" +

                            "  startAutoPlay();" +
                            "}, 300);"
            );
        }

        return container;
    }

    private Div createCarouselCard(Ksiazka ksiazka) {
        Div card = new Div();
        card.addClassName("carousel-card");

        Image coverImage;
        byte[] okladka = ksiazka.getDaneKsiazki().getOkladka();
        if (okladka != null && okladka.length > 0) {
            StreamResource resource = new StreamResource("cov_" + ksiazka.getId(), () -> new java.io.ByteArrayInputStream(okladka));
            coverImage = new Image(resource, "Okładka");
        } else {
            coverImage = new Image("https://placehold.co/150x220?text=Brak+okładki", "Brak");
        }
        coverImage.addClassName("carousel-cover");

        Div info = new Div();
        info.addClassName("carousel-info");

        Span tytul = new Span(ksiazka.getDaneKsiazki().getTytul());
        tytul.addClassName("carousel-title");
        info.add(tytul);

        String autorStr = "Nieznany autor";
        if (ksiazka.getDaneKsiazki().getAutorzy() != null && !ksiazka.getDaneKsiazki().getAutorzy().isEmpty()) {
            autorStr = ksiazka.getDaneKsiazki().getAutorzy().stream()
                    .map(a -> a.getImie() + " " + a.getNazwisko())
                    .collect(java.util.stream.Collectors.joining(", "));
        }

        Span autor = new Span(autorStr);
        autor.addClassName("carousel-author");
        info.add(autor);

        String wydawnictwo = ksiazka.getDaneKsiazki().getWydawnictwo() != null ? ksiazka.getDaneKsiazki().getWydawnictwo() : "-";
        String rok = ksiazka.getDaneKsiazki().getRokWydania() != null ? String.valueOf(ksiazka.getDaneKsiazki().getRokWydania()) : "-";
        String isbn = ksiazka.getDaneKsiazki().getIsbn() != null ? ksiazka.getDaneKsiazki().getIsbn() : "-";

        Span daneWydawnicze = new Span("Wydawnictwo: " + wydawnictwo + " | Rok: " + rok + " | ISBN: " + isbn);
        daneWydawnicze.addClassName("carousel-details");
        info.add(daneWydawnicze);

        if (isCurrentUserStaff()) {
            Span wypozyczenia = new Span("Wypożyczeń: " + ksiazka.getLicznikWypozyczen());
            wypozyczenia.getElement().getThemeList().add("badge contrast primary small");
            wypozyczenia.getStyle().set("margin-top", "6px");
            info.add(wypozyczenia);
        }

        card.add(coverImage, info);
        card.addClickListener(e -> new KsiazkaDetailsDialog(ksiazka, rentalService, currentUser).open());

        return card;
    }

    private boolean isCurrentUserStaff() {
        UserDetails userDetails = securityService.getAuthenticatedUser();
        if (userDetails == null) return false;
        return userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_KIEROWNIK") ||
                        a.getAuthority().equals("ROLE_BIBLIOTEKARZ") ||
                        a.getAuthority().equals("ROLE_MAGAZYNIER"));
    }

    private void updateList() {
        grid.setItems(bookService.findKsiazkiBySearch(searchField.getValue()));
    }
}