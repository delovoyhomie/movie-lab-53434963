package ru.itmo.movielab.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import java.util.List;
import ru.itmo.movielab.model.*;

@ApplicationScoped
public class SpecialRepository {

    @PersistenceContext(unitName = "movies")
    EntityManager entityManager;

    public Object deleteTagline(String text) {
        Object id = entityManager
            .createNativeQuery("select delete_one_by_tagline(:text)")
            .setParameter("text", text)
            .getSingleResult();
        entityManager.clear();
        return id;
    }

    @SuppressWarnings("unchecked")
    public List<Movie> contains(String text) {
        return entityManager
            .createNativeQuery("select * from movies_by_tagline(:text)", Movie.class)
            .setParameter("text", text)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Movie> less(String genre) {
        return entityManager
            .createNativeQuery("select * from movies_genre_less(:genre)", Movie.class)
            .setParameter("genre", genre)
            .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Person> writers() {
        return entityManager
            .createNativeQuery("select * from screenwriters_without_oscars()", Person.class)
            .getResultList();
    }

    public Object redistribute(String from, String to) {
        Object result = entityManager
            .createNativeQuery("select redistribute_oscars(:source,:target)")
            .setParameter("source", from)
            .setParameter("target", to)
            .getSingleResult();
        entityManager.clear();
        return result;
    }
}
