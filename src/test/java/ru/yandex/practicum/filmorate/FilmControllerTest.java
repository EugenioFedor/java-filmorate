package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class FilmControllerTest {

    private FilmController filmController;
    private FilmService filmService;

    @BeforeEach
    void setUp() {
        filmService = new FilmService();
        filmController = new FilmController(filmService);
    }

    @Test
    void addFilm_shouldAddFilm() {
        Film film = new Film();
        film.setName("Inception");
        film.setDescription("A mind-bending thriller");
        film.setReleaseDate(LocalDate.parse("2010-07-16"));
        film.setDuration(148);

        filmController.create(film);

        Collection<Film> films = filmController.getAll();
        assertTrue(films.contains(film));
    }

    @Test
    void getAllFilms_shouldReturnAllFilms() {
        Film film1 = new Film();
        film1.setName("The Matrix");
        film1.setDescription("Sci-Fi classic");
        film1.setReleaseDate(LocalDate.parse("1999-03-31"));
        film1.setDuration(136);

        Film film2 = new Film();
        film2.setName("Interstellar");
        film2.setDescription("Space exploration");
        film2.setReleaseDate(LocalDate.parse("2014-11-07"));
        film2.setDuration(169);

        filmController.create(film1);
        filmController.create(film2);

        Collection<Film> films = filmController.getAll();
        assertEquals(2, films.size());
        assertTrue(films.contains(film1));
        assertTrue(films.contains(film2));
    }
}
