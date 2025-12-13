package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController filmController;

    @BeforeEach
    void setUp() {
        filmController = new FilmController();
    }


    @Test
    void shouldCreateFilm_whenValidFilm() {
        Film film = new Film();
        film.setName("Film name");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Film created = filmController.create(film);

        assertNotNull(created.getId());
        assertEquals("Film name", created.getName());
    }


    @Test
    void shouldThrowException_whenNameIsBlank() {
        Film film = new Film();
        film.setName("");
        film.setDuration(100);

        assertThrows(ValidationException.class,
                () -> filmController.create(film));
    }

    @Test
    void shouldThrowException_whenDescriptionTooLong() {
        Film film = new Film();
        film.setName("Film");
        film.setDescription("a".repeat(201));
        film.setDuration(100);

        assertThrows(ValidationException.class,
                () -> filmController.create(film));
    }

    @Test
    void shouldThrowException_whenReleaseDateTooEarly() {
        Film film = new Film();
        film.setName("Film");
        film.setReleaseDate(LocalDate.of(1800, 1, 1));
        film.setDuration(100);

        assertThrows(ValidationException.class,
                () -> filmController.create(film));
    }

    @Test
    void shouldThrowException_whenDurationIsZero() {
        Film film = new Film();
        film.setName("Film");
        film.setDuration(0);

        assertThrows(ValidationException.class,
                () -> filmController.create(film));
    }

    @Test
    void shouldThrowException_whenDurationIsNegative() {
        Film film = new Film();
        film.setName("Film");
        film.setDuration(-10);

        assertThrows(ValidationException.class,
                () -> filmController.create(film));
    }
}
