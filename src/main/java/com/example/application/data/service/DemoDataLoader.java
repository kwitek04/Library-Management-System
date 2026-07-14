package com.example.application.data.service;

import com.example.application.data.entity.*;
import com.example.application.data.repository.KsiazkaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Wypełnia bazę przykładowymi danymi przy pierwszym uruchomieniu (gdy katalog jest pusty).
 */
@Component
@Order(2)
public class DemoDataLoader implements CommandLineRunner {

    private static final Map<String, String> COVER_BY_ISBN = Map.of(
            "9788381150310", "lalka.jpg",
            "9788324012345", "zbrodnia.jpg",
            "9788328729123", "1984.jpg",
            "9788373014567", "hobbit.jpg",
            "9788373017890", "wiedzmin.jpg",
            "9788380089123", "pan-tadeusz.jpg",
            "9788324590789", "mistrz.jpg"
    );

    private final BookService bookService;
    private final KsiazkaRepository ksiazkaRepository;

    public DemoDataLoader(BookService bookService, KsiazkaRepository ksiazkaRepository) {
        this.bookService = bookService;
        this.ksiazkaRepository = ksiazkaRepository;
    }

    @Override
    public void run(String... args) {
        if (ksiazkaRepository.count() == 0) {
            seedDemoData();
            System.out.println(">>> Załadowano przykładowe książki do katalogu demo");
        } else {
            backfillMissingCovers();
        }
    }

    private void seedDemoData() {
        Poddziedzina powiescPolska = createCategory("Literatura piękna", "Powieść polska");
        Poddziedzina powiescObca = createCategory("Literatura piękna", "Powieść obca");
        Poddziedzina fantasy = createCategory("Fantasy i sci-fi", "Fantasy");
        Poddziedzina scifi = createCategory("Fantasy i sci-fi", "Science fiction");

        createBook("9788381150310", "Lalka", "Wydawnictwo MG", 2009,
                "Powieść realistyczna o miłości i społeczeństwie XIX-wiecznej Warszawy.",
                45.0, "Bolesław", "Prus", powiescPolska, 42);

        createBook("9788324012345", "Zbrodnia i kara", "Znak", 2014,
                "Klasyczna powieść psychologiczna o winie, sumieniu i moralności.",
                39.99, "Fiodor", "Dostojewski", powiescObca, 35);

        createBook("9788328729123", "1984", "Muza", 2019,
                "Antyutopia o totalitarnym świecie, inwigilacji i manipulacji prawdą.",
                42.50, "George", "Orwell", scifi, 51);

        createBook("9788373014567", "Hobbit, czyli tam i z powrotem", "Iskry", 2013,
                "Przygodowa opowieść o podróży Bilba Bagginsa i smoku Smaugu.",
                38.00, "J.R.R.", "Tolkien", fantasy, 38);

        createBook("9788373017890", "Wiedźmin: Ostatnie życzenie", "SuperNowa", 2015,
                "Zbiór opowiadań wprowadzających do świata Geralta z Rivii.",
                44.99, "Andrzej", "Sapkowski", fantasy, 47);

        createBook("9788380089123", "Pan Tadeusz", "Zielona Sowa", 2018,
                "Narodowa epopeja w wierszu, obraz szlachty polskiej na Litwie.",
                32.00, "Adam", "Mickiewicz", powiescPolska, 22);

        createBook("9788324590789", "Mistrz i Małgorzata", "Muza", 2020,
                "Mistyczna opowieść osadzona w sowieckiej Moskwie lat 30.",
                41.00, "Michaił", "Bułhakow", powiescObca, 18);
    }

    private void backfillMissingCovers() {
        List<Ksiazka> updated = bookService.findAllKsiazki("").stream()
                .filter(this::needsCover)
                .peek(this::attachCoverIfAvailable)
                .filter(k -> k.getDaneKsiazki().getOkladka() != null)
                .toList();

        updated.forEach(bookService::saveKsiazka);

        if (!updated.isEmpty()) {
            System.out.println(">>> Uzupełniono okładki dla " + updated.size() + " książek demo");
        }
    }

    private boolean needsCover(Ksiazka ksiazka) {
        if (ksiazka.getDaneKsiazki() == null) {
            return false;
        }
        byte[] okladka = ksiazka.getDaneKsiazki().getOkladka();
        return okladka == null || okladka.length < 1000;
    }

    private void attachCoverIfAvailable(Ksiazka ksiazka) {
        String isbn = ksiazka.getDaneKsiazki().getIsbn();
        String coverFile = COVER_BY_ISBN.get(isbn);
        if (coverFile == null) {
            return;
        }
        loadCover(coverFile).ifPresent(bytes -> ksiazka.getDaneKsiazki().setOkladka(bytes));
    }

    private Poddziedzina createCategory(String dziedzinaName, String poddziedzinaName) {
        return bookService.findAllDziedziny().stream()
                .filter(d -> d.getNazwa().equals(dziedzinaName))
                .findFirst()
                .flatMap(d -> d.getPoddziedziny().stream()
                        .filter(p -> p.getNazwa().equals(poddziedzinaName))
                        .findFirst())
                .orElseGet(() -> {
                    Dziedzina dziedzina = bookService.findAllDziedziny().stream()
                            .filter(d -> d.getNazwa().equals(dziedzinaName))
                            .findFirst()
                            .orElseGet(() -> {
                                Dziedzina nowa = new Dziedzina(dziedzinaName);
                                bookService.saveDziedzina(nowa);
                                return nowa;
                            });

                    Poddziedzina poddziedzina = new Poddziedzina(poddziedzinaName, dziedzina);
                    dziedzina.getPoddziedziny().add(poddziedzina);
                    bookService.saveDziedzina(dziedzina);
                    return poddziedzina;
                });
    }

    private void createBook(String isbn, String tytul, String wydawnictwo, int rokWydania,
                            String opis, double cena, String imieAutora, String nazwiskoAutora,
                            Poddziedzina poddziedzina, int licznikWypozyczen) {
        Autor autor = bookService.findAllAutorzy().stream()
                .filter(a -> a.getImie().equals(imieAutora) && a.getNazwisko().equals(nazwiskoAutora))
                .findFirst()
                .orElseGet(() -> {
                    Autor nowy = new Autor(imieAutora, nazwiskoAutora);
                    bookService.saveAutor(nowy);
                    return nowy;
                });

        DaneKsiazki dane = new DaneKsiazki(isbn, tytul, wydawnictwo, rokWydania);
        dane.setOpis(opis);
        dane.setCena(cena);
        Set<Autor> autorzy = new HashSet<>();
        autorzy.add(autor);
        dane.setAutorzy(autorzy);

        loadCover(COVER_BY_ISBN.get(isbn)).ifPresent(dane::setOkladka);

        Ksiazka ksiazka = new Ksiazka(StanFizyczny.BARDZO_DOBRY, StatusKsiazki.DOSTEPNA, dane);
        ksiazka.setPoddziedzina(poddziedzina);
        ksiazka.setLicznikWypozyczen(licznikWypozyczen);
        bookService.saveKsiazka(ksiazka);
    }

    private Optional<byte[]> loadCover(String fileName) {
        if (fileName == null) {
            return Optional.empty();
        }
        try (InputStream in = new ClassPathResource("demo/covers/" + fileName).getInputStream()) {
            return Optional.of(in.readAllBytes());
        } catch (IOException e) {
            System.out.println(">>> Nie udało się wczytać okładki: " + fileName);
            return Optional.empty();
        }
    }
}
