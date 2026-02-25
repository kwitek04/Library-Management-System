# Wypożyczalnia książek z wolnym dostępem

System zarządzania wypożyczalnią książek z wolnym dostępem stworzony w technologii Java (Spring Boot) oraz Vaadin. System usprawnia i automatyzuje działanie wypożyczalni książek. Aplikacja pozwala na rezerwację, wypożyczanie i zwrot książek. Dzięki zaimplementowanej logice biznesowej system automatycznie kontroluje limity wypożyczeń i rezerwacji, zarządza terminami oddania oraz nalicza kary za opóźnienia i zagubienia książek. Aplikacja wyróżnia się rozbudowanym podziałem na role, oraz nowoczesnym interfejsie użytkownika zaprojektowanym z myślą o wygodzie i czytelności.  

## Zrzuty ekranu

<div align="center">
  <img src="docs/images/zrzut_ekranu_strona_glowna.jpeg" alt="Strona Główna" />
  <p><i>Widok strony głównej z wyszukiwarką, oraz przewijaną listą najpopularniejszych książek.</i></p>
</div>

<br/>

<div align="center">
  <img src="docs/images/zrzut_ekranu_statystyki.jpeg" alt="Statystyki" />
  <p><i>Panel statystyk i wykresów dostępny dla kierownika.</i></p>
</div>

## Technologie

Projekt wykorzystuje następujące technologie:
* **Backend:** Java 25, Spring Boot 3.5, Spring Security, Spring Data JPA
* **Frontend:** Vaadin 24 Flow, CSS, JavaScript
* **Baza danych:** PostgreSQL
* **Migracje bazy:** Flyway
* **Konteneryzacja:** Docker & Docker Compose
* **Testy:** JUnit 5, Mockito

## Główne funkcjonalności

System posiada podział na role z różnymi uprawnieniami:

* **Użytkownik niezalogowany:** Przeglądanie katalogu, wyszukiwanie książek.
* **Czytelnik:** Wypożyczanie, rezerwacja i zwroty książek, podgląd historii wypożyczeń i kar.
* **Bibliotekarz:** Zarządzanie użytkownikami, aktywacja kont, obsługa zwrotów, zarządzanie książkami.
* **Magazynier:** Kontrola fizycznego stanu książek, odkładanie zwróconych książek na półkę.
* **Kierownik:** Zakładanie kont pracowników, zarządzanie pracownikami, dostęp do statystyk globalnych, decyzje o wycofaniu zniszczonych książek.

## Uruchomienie (Docker)

Aby uruchomić aplikację w kontenerze (wraz z bazą danych):

1. Upewnij się, że masz zainstalowany Docker.
2. W katalogu głównym projektu wykonaj polecenie:

```bash
docker-compose up --build
```
3. Aplikacja będzie dostępna pod adresem: http://localhost:8080/

### Domyślne dane logowania (Kierownik):
* **Email:** `admin@admin.pl`
* **Hasło:** `admin`

## Testy

Aplikacja posiada pokrycie testami jednostkowymi dla kluczowej logiki biznesowej. Aby uruchomić testy:

```bash
./mvnw test