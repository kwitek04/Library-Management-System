package com.example.application.data.service;

import com.example.application.data.entity.*;
import com.example.application.data.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serwis odpowiedzialny za zarządzanie katalogiem bibliotecznym.
 * Zawiera logikę biznesową dotyczącą książek, autorów, dziedzin oraz tłumaczy.
 */
@Service
@Transactional
public class BookService {

    private final KsiazkaRepository ksiazkaRepository;
    private final AutorRepository autorRepository;
    private final DziedzinaRepository dziedzinaRepository;
    private final PoddziedzinaRepository poddziedzinaRepository;
    private final DaneKsiazkiRepository daneKsiazkiRepository;
    private final TlumaczRepository tlumaczRepository;

    public BookService(KsiazkaRepository ksiazkaRepository,
                       AutorRepository autorRepository,
                       DziedzinaRepository dziedzinaRepository,
                       PoddziedzinaRepository poddziedzinaRepository,
                       DaneKsiazkiRepository daneKsiazkiRepository,
                       TlumaczRepository tlumaczRepository) {
        this.ksiazkaRepository = ksiazkaRepository;
        this.autorRepository = autorRepository;
        this.dziedzinaRepository = dziedzinaRepository;
        this.poddziedzinaRepository = poddziedzinaRepository;
        this.daneKsiazkiRepository = daneKsiazkiRepository;
        this.tlumaczRepository = tlumaczRepository;
    }

    // Podstawowe operacje na książkach i wyszukiwanie

    /**
     * Pobiera listę wszystkich książek lub filtruje je na podstawie podanej frazy.
     * Wyszukiwanie obejmuje tytuł, ISBN oraz autora.
     */
    public List<Ksiazka> findAllKsiazki(String stringFilter) {
        if (stringFilter == null || stringFilter.isEmpty()) {
            return ksiazkaRepository.findAll();
        } else {
            return ksiazkaRepository.searchAll(stringFilter);
        }
    }

    /**
     * Zwraca listę wszystkich książek, które są aktywne w systemie (nie zostały wycofane).
     */
    public List<Ksiazka> findAllActiveKsiazki() {
        return ksiazkaRepository.findByStatusNot(StatusKsiazki.WYCOFANA);
    }

    /**
     * Wyszukuje aktywne książki pasujące do podanej frazy.
     * Pomija książki o statusie wycofana.
     */
    public List<Ksiazka> findKsiazkiBySearch(String searchTerm) {
        if (searchTerm == null || searchTerm.isEmpty()) {
            return ksiazkaRepository.findByStatusNot(StatusKsiazki.WYCOFANA);
        } else {
            return ksiazkaRepository.searchWithExclusion(searchTerm, StatusKsiazki.WYCOFANA);
        }
    }

    /**
     * Zapisuje nową książkę lub aktualizuje istniejącą.
     * Aktualizuje również powiązane dane szczegółowe.
     */
    public void saveKsiazka(Ksiazka ksiazka) {
        if (ksiazka == null) return;

        if (ksiazka.getDaneKsiazki() != null) {
            daneKsiazkiRepository.save(ksiazka.getDaneKsiazki());
        }
        ksiazkaRepository.save(ksiazka);
    }

    /**
     * Usuwa książkę z bazy danych.
     */
    public void deleteKsiazka(Ksiazka ksiazka) {
        if (ksiazka != null && ksiazka.getId() != null) {
            ksiazkaRepository.delete(ksiazka);
        }
    }

    // Metody filtrujące książki według kategorii i autorów

    public List<Ksiazka> findKsiazkiByPoddziedzina(Poddziedzina p) {
        return ksiazkaRepository.findByPoddziedzinaAndStatusNot(p, StatusKsiazki.WYCOFANA);
    }

    public List<Ksiazka> findKsiazkiByDziedzina(Dziedzina d) {
        return ksiazkaRepository.findByDziedzinaAndStatusNot(d, StatusKsiazki.WYCOFANA);
    }

    public List<Ksiazka> findKsiazkiByAutor(Autor autor) {
        return ksiazkaRepository.findByAutorAndStatusNot(autor, StatusKsiazki.WYCOFANA);
    }

    // Zarządzanie danymi katalogowymi takimi jak autorzy, kategorie i tłumacze

    public List<Autor> findAllAutorzy() {
        return autorRepository.findAll();
    }

    public void saveAutor(Autor autor) {
        if (autor != null) {
            autorRepository.save(autor);
        }
    }

    public List<Dziedzina> findAllDziedziny() {
        return dziedzinaRepository.findAll();
    }

    public void saveDziedzina(Dziedzina dziedzina) {
        dziedzinaRepository.save(dziedzina);
    }

    public void deleteDziedzina(Dziedzina dziedzina) {
        dziedzinaRepository.delete(dziedzina);
    }

    public List<Poddziedzina> findAllPoddziedziny() {
        return poddziedzinaRepository.findAll();
    }

    public List<Poddziedzina> findPoddziedzinyByDziedzina(Dziedzina dziedzina) {
        return poddziedzinaRepository.findAllByDziedzina(dziedzina);
    }

    public void savePoddziedzina(Poddziedzina poddziedzina) {
        poddziedzinaRepository.save(poddziedzina);
    }

    /**
     * Usuwa poddziedzinę, aktualizując wcześniej jej rodzica, aby zachować spójność danych.
     */
    @Transactional
    public void deletePoddziedzina(Poddziedzina poddziedzina) {
        if (poddziedzina == null) return;

        Dziedzina parent = dziedzinaRepository.findById(poddziedzina.getDziedzina().getId()).orElse(null);
        if (parent != null) {
            parent.getPoddziedziny().removeIf(p -> p.getId().equals(poddziedzina.getId()));
            dziedzinaRepository.save(parent);
        }
        poddziedzinaRepository.delete(poddziedzina);
    }

    public List<DaneKsiazki> findAllDaneKsiazki() {
        return daneKsiazkiRepository.findAll();
    }

    public List<Tlumacz> findAllTlumacze() {
        return tlumaczRepository.findAll();
    }

    public void saveTlumacz(Tlumacz tlumacz) {
        if (tlumacz != null) {
            tlumaczRepository.save(tlumacz);
        }
    }

    // Metody statystyczne i magazynowe (tylko odczyt)

    public long countAllBooks() {
        return ksiazkaRepository.count();
    }

    public long countKsiazkiByStatus(StatusKsiazki status) {
        return ksiazkaRepository.countByStatus(status);
    }

    public List<Ksiazka> findKsiazkiDoKontroli() {
        return ksiazkaRepository.findByWymagaKontroliTrueAndStatus(StatusKsiazki.W_KONTROLI);
    }

    public List<Ksiazka> findKsiazkiDoOdlozenia() {
        return ksiazkaRepository.findByStatus(StatusKsiazki.DO_ODLOZENIA);
    }

    public List<Ksiazka> findKsiazkiDoDecyzjiWycofania() {
        return ksiazkaRepository.findByStanFizycznyAndStatusNot(
                StanFizyczny.DO_WYCOFANIA,
                StatusKsiazki.WYCOFANA
        );
    }

    public List<Ksiazka> findBestsellers() {
        return ksiazkaRepository.findTop10ByStatusNotOrderByLicznikWypozyczenDesc(StatusKsiazki.WYCOFANA);
    }
}