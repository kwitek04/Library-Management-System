package com.example.application.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@Entity
public class DaneKsiazki {

    @Id
    @NotEmpty
    @Size(min = 10, max = 20)
    private String isbn;

    @NotEmpty
    @Size(max = 100)
    private String tytul;

    @NotEmpty
    @Size(max = 25)
    private String wydawnictwo;

    private Integer rokWydania;

    @Column(length = 2000)
    private String opis;

    @Lob
    @Column(length = 10000000)
    private byte[] okladka;

    private Double cena = 0.0;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "autor_ksiazka",
            joinColumns = @JoinColumn(name = "ksiazka_id"),
            inverseJoinColumns = @JoinColumn(name = "autor_id")
    )
    private Set<Autor> autorzy = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "tlumacz_ksiazka",
            joinColumns = @JoinColumn(name = "ksiazka_isbn"),
            inverseJoinColumns = @JoinColumn(name = "tlumacz_id")
    )
    private Set<Tlumacz> tlumacze = new HashSet<>();

    public Set<Tlumacz> getTlumacze() { return tlumacze; }
    public void setTlumacze(Set<Tlumacz> tlumacze) { this.tlumacze = tlumacze; }

    public DaneKsiazki() {}

    public DaneKsiazki(String isbn, String tytul, String wydawnictwo, int rokWydania) {
        this.isbn = isbn;
        this.tytul = tytul;
        this.wydawnictwo = wydawnictwo;
        this.rokWydania = rokWydania;
    }

    // Gettery i Settery
    public String getOpis() { return opis; }
    public void setOpis(String opis) { this.opis = opis; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTytul() { return tytul; }
    public void setTytul(String tytul) { this.tytul = tytul; }

    public String getWydawnictwo() { return wydawnictwo; }
    public void setWydawnictwo(String wydawnictwo) { this.wydawnictwo = wydawnictwo; }

    public Integer getRokWydania() { return rokWydania; }
    public void setRokWydania(Integer rokWydania) { this.rokWydania = rokWydania; }

    public byte[] getOkladka() { return okladka; }
    public void setOkladka(byte[] okladka) { this.okladka = okladka; }

    public Set<Autor> getAutorzy() { return autorzy; }
    public void setAutorzy(Set<Autor> autorzy) { this.autorzy = autorzy; }

    public Double getCena() {
        return cena;
    }

    public void setCena(Double cena) {
        this.cena = cena;
    }
}