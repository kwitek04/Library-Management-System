package com.example.application.views.katalog;

import com.example.application.data.entity.*;
import com.example.application.data.service.BookService;
import com.example.application.data.service.RentalService;
import com.example.application.data.service.UserService;
import com.example.application.security.SecurityService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.ByteArrayInputStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Główny widok katalogu bibliotecznego dostępny dla wszystkich (również niezalogowanych).
 * Umożliwia przeglądanie zbiorów, filtrowanie według dziedizn, poddziedizn i autora.
 */
@AnonymousAllowed
@Route(value = "katalog", layout = MainLayout.class)
@PageTitle("Katalog | Wypożyczalnia książek")
public class KatalogView extends VerticalLayout {

    private final BookService bookService;
    private final UserService userService;
    private final RentalService rentalService;
    private final SecurityService securityService;

    private Uzytkownik currentUser;

    private final TreeGrid<Object> categoryTree = new TreeGrid<>();
    private final Grid<Autor> authorGrid = new Grid<>(Autor.class);
    private final Grid<Ksiazka> bookGrid = new Grid<>(Ksiazka.class);
    private final TextField authorFilter = new TextField();

    public KatalogView(BookService bookService, UserService userService, RentalService rentalService, SecurityService securityService) {
        this.bookService = bookService;
        this.userService = userService;
        this.rentalService = rentalService;
        this.securityService = securityService;

        loadCurrentUser();
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("katalog-view");

        configureCategoryTree();
        configureAuthorGrid();

        Div leftPanelContent = new Div();
        leftPanelContent.setSizeFull();
        leftPanelContent.add(categoryTree);

        Tab tabKategorie = new Tab(VaadinIcon.FOLDER.create(), new Span(" Kategorie"));
        Tab tabAutorzy = new Tab(VaadinIcon.USER.create(), new Span(" Autorzy"));

        Tabs tabs = new Tabs(tabKategorie, tabAutorzy);
        tabs.setWidthFull();
        tabs.addSelectedChangeListener(event -> {
            leftPanelContent.removeAll();
            if (event.getSelectedTab().equals(tabKategorie)) {
                leftPanelContent.add(categoryTree);
                updateBookList(null);
                categoryTree.asSingleSelect().clear();
            } else {
                VerticalLayout authorLayout = new VerticalLayout(authorFilter, authorGrid);
                authorLayout.setPadding(false);
                authorLayout.setSpacing(true);
                authorLayout.setSizeFull();
                authorFilter.setWidthFull();

                leftPanelContent.add(authorLayout);
                updateBookList(null);
                authorGrid.asSingleSelect().clear();
            }
        });

        VerticalLayout leftSide = new VerticalLayout(tabs, leftPanelContent);
        leftSide.setPadding(false);
        leftSide.setSpacing(false);
        leftSide.setHeightFull();
        leftSide.setWidth("350px");
        leftSide.setMinWidth("350px");
        leftSide.setMaxWidth("350px");
        leftSide.getStyle().set("border-right", "1px solid #e0e0e0");

        configureBookGrid();

        HorizontalLayout mainLayout = new HorizontalLayout(leftSide, bookGrid);
        mainLayout.setSizeFull();
        mainLayout.setSpacing(false);
        mainLayout.setFlexGrow(1, bookGrid);

        add(mainLayout);

        updateBookList(null);
    }

    private void loadCurrentUser() {
        UserDetails userDetails = securityService.getAuthenticatedUser();
        if (userDetails != null) {
            this.currentUser = userService.findUzytkownikByEmail(userDetails.getUsername());
        } else {
            this.currentUser = null;
        }
    }

    private void configureCategoryTree() {
        categoryTree.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        categoryTree.setSizeFull();

        TreeData<Object> treeData = new TreeData<>();
        List<Dziedzina> dziedziny = bookService.findAllDziedziny();
        dziedziny.sort(Comparator.comparing(Dziedzina::getNazwa));

        for (Dziedzina d : dziedziny) {
            treeData.addItem(null, d);
            List<Poddziedzina> poddziedziny = d.getPoddziedziny();
            poddziedziny.sort(Comparator.comparing(Poddziedzina::getNazwa));
            for (Poddziedzina p : poddziedziny) {
                treeData.addItem(d, p);
            }
        }
        categoryTree.setDataProvider(new TreeDataProvider<>(treeData));

        categoryTree.addHierarchyColumn(item -> {
            if (item instanceof Dziedzina) return ((Dziedzina) item).getNazwa();
            if (item instanceof Poddziedzina) return ((Poddziedzina) item).getNazwa();
            return item.toString();
        }).setHeader("Wybierz kategorię");

        categoryTree.asSingleSelect().addValueChangeListener(e -> updateBookList(e.getValue()));
    }

    private void configureAuthorGrid() {
        authorGrid.setSizeFull();
        authorGrid.removeAllColumns();
        authorGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
        authorGrid.addColumn(a -> a.getNazwisko() + " " + a.getImie()).setHeader("Nazwisko i Imię");

        List<Autor> allAuthors = bookService.findAllAutorzy();
        allAuthors.sort(Comparator.comparing(Autor::getNazwisko)
                .thenComparing(Autor::getImie));

        authorGrid.setItems(allAuthors);

        authorFilter.setPlaceholder("Szukaj autora...");
        authorFilter.setValueChangeMode(ValueChangeMode.LAZY);
        authorFilter.setPrefixComponent(VaadinIcon.SEARCH.create());
        authorFilter.addValueChangeListener(e -> {
            String searchTerm = e.getValue().toLowerCase();
            List<Autor> filtered = allAuthors.stream()
                    .filter(a -> (a.getNazwisko() + " " + a.getImie()).toLowerCase().contains(searchTerm))
                    .collect(Collectors.toList());
            authorGrid.setItems(filtered);
        });

        authorGrid.asSingleSelect().addValueChangeListener(e -> {
            if (e.getValue() != null) {
                List<Ksiazka> books = bookService.findKsiazkiByAutor(e.getValue());
                bookGrid.setItems(books);
            }
        });
    }

    private void configureBookGrid() {
        bookGrid.addClassName("katalog-books");
        bookGrid.setSizeFull();
        bookGrid.removeAllColumns();
        bookGrid.addComponentColumn(this::createBookCard);
        bookGrid.addThemeVariants(GridVariant.LUMO_NO_ROW_BORDERS, GridVariant.LUMO_NO_BORDER);

        bookGrid.addItemClickListener(event -> {
            Ksiazka clickedBook = event.getItem();
            new KsiazkaDetailsDialog(clickedBook, rentalService, currentUser).open();
        });
    }

    private void updateBookList(Object selection) {
        List<Ksiazka> ksiazki;
        if (selection instanceof Dziedzina) {
            ksiazki = bookService.findKsiazkiByDziedzina((Dziedzina) selection);
        } else if (selection instanceof Poddziedzina) {
            ksiazki = bookService.findKsiazkiByPoddziedzina((Poddziedzina) selection);
        } else {
            ksiazki = bookService.findAllActiveKsiazki();
        }
        bookGrid.setItems(ksiazki);
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
            StreamResource resource = new StreamResource("cover_" + ksiazka.getId(), () -> new ByteArrayInputStream(okladka));
            coverImage = new Image(resource, "Okładka");
        } else {
            coverImage = new Image("https://placehold.co/100x150?text=Brak+okładki", "Brak okładki");
        }
        coverImage.setWidth("120px");
        coverImage.setHeight("180px");
        coverImage.getStyle().set("border-radius", "5px").set("object-fit", "cover");
        coverImage.getStyle().set("box-shadow", "0 4px 6px rgba(0,0,0,0.1)");

        VerticalLayout details = new VerticalLayout();
        details.setSpacing(false);
        details.setPadding(false);

        H3 tytul = new H3(ksiazka.getDaneKsiazki().getTytul());
        tytul.getStyle().set("margin", "0 0 5px 0");

        String autorzyStr = ksiazka.getDaneKsiazki().getAutorzy().stream()
                .map(a -> a.getImie() + " " + a.getNazwisko())
                .collect(Collectors.joining(", "));
        Span autor = new Span(autorzyStr);
        autor.getStyle().set("font-weight", "bold").set("color", "#7f8c8d");

        Span meta = new Span(ksiazka.getDaneKsiazki().getWydawnictwo() + " • " + ksiazka.getDaneKsiazki().getRokWydania());
        meta.getStyle().set("font-size", "0.9em").set("color", "#95a5a6");

        Span isbn = new Span("ISBN: " + ksiazka.getDaneKsiazki().getIsbn());
        isbn.getStyle().set("font-size", "0.9em").set("color", "#95a5a6");

        String katStr = "-";
        if (ksiazka.getPoddziedzina() != null) {
            katStr = ksiazka.getPoddziedzina().getDziedzina().getNazwa() + " > " + ksiazka.getPoddziedzina().getNazwa();
        }
        Span kategoria = new Span(katStr);
        kategoria.getElement().getThemeList().add("badge contrast");
        kategoria.getStyle().set("margin-top", "10px");

        details.add(tytul, autor, meta, isbn, kategoria);

        Div spacer = new Div();
        card.setFlexGrow(1, spacer);

        Span statusBadge = createStatusBadge(ksiazka);
        VerticalLayout rightSide = new VerticalLayout(spacer, statusBadge);
        rightSide.setSpacing(false);
        rightSide.setPadding(false);
        rightSide.setHeight("180px");
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
        badge.getElement().getThemeList().add("badge " + (dostepna ? "success" : "error"));
        badge.getStyle().set("padding", "0.5em 1em");
        return badge;
    }
}