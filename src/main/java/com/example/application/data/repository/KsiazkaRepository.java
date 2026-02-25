package com.example.application.data.repository;

import com.example.application.data.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KsiazkaRepository extends JpaRepository<Ksiazka, Long> {

    @Query("select distinct k from Ksiazka k " +
            "join k.daneKsiazki d " +
            "left join d.autorzy a " +
            "where lower(d.tytul) like lower(concat('%', :searchTerm, '%')) " +
            "or lower(d.isbn) like lower(concat('%', :searchTerm, '%')) " +
            "or lower(a.imie) like lower(concat('%', :searchTerm, '%')) " +
            "or lower(a.nazwisko) like lower(concat('%', :searchTerm, '%'))")
    List<Ksiazka> searchAll(@Param("searchTerm") String searchTerm);

    @Query("select distinct k from Ksiazka k " +
            "join k.daneKsiazki d " +
            "left join d.autorzy a " +
            "where (k.status <> :excludedStatus) and " +
            "(lower(d.tytul) like lower(concat('%', :searchTerm, '%')) " +
            "or lower(d.isbn) like lower(concat('%', :searchTerm, '%')) " +
            "or lower(a.imie) like lower(concat('%', :searchTerm, '%')) " +
            "or lower(a.nazwisko) like lower(concat('%', :searchTerm, '%')))")
    List<Ksiazka> searchWithExclusion(@Param("searchTerm") String searchTerm, @Param("excludedStatus") StatusKsiazki excludedStatus);

    List<Ksiazka> findByStatusNot(StatusKsiazki status);

    List<Ksiazka> findByPoddziedzinaAndStatusNot(Poddziedzina poddziedzina, StatusKsiazki status);

    @Query("select k from Ksiazka k where k.poddziedzina.dziedzina = :dziedzina and k.status <> :excludedStatus")
    List<Ksiazka> findByDziedzinaAndStatusNot(@Param("dziedzina") Dziedzina dziedzina, @Param("excludedStatus") StatusKsiazki excludedStatus);

    @Query("select k from Ksiazka k join k.daneKsiazki d join d.autorzy a where a = :autor and k.status <> :excludedStatus")
    List<Ksiazka> findByAutorAndStatusNot(@Param("autor") Autor autor, @Param("excludedStatus") StatusKsiazki excludedStatus);

    List<Ksiazka> findByWymagaKontroliTrueAndStatus(StatusKsiazki status);

    List<Ksiazka> findByStanFizycznyAndStatusNot(StanFizyczny stan, StatusKsiazki status);

    List<Ksiazka> findByStatus(StatusKsiazki status);

    List<Ksiazka> findTop10ByStatusNotOrderByLicznikWypozyczenDesc(StatusKsiazki status);

    long countByStatus(com.example.application.data.entity.StatusKsiazki status);
}