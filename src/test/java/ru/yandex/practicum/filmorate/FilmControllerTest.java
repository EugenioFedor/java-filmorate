package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FilmControllerTest {

    @Mock
    private FilmService filmService;

    @InjectMocks
    private FilmController filmController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnAllFilms() {
        when(filmService.findAll()).thenReturn(Collections.emptyList());

        assertNotNull(filmController.findAll());
        verify(filmService, times(1)).findAll();
    }

    @Test
    void shouldCreateFilm_whenValidFilm() {
        Film film = new Film();
        film.setName("Film name");
        film.setDescription("Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        when(filmService.create(film)).thenReturn(film);

        Film created = filmController.create(film);

        assertEquals("Film name", created.getName());
        verify(filmService, times(1)).create(film);
    }

    @Test
    void shouldThrowException_whenInvalidFilm() {
        Film film = new Film();
        film.setName("");
        film.setDuration(100);

        when(filmService.create(film)).thenThrow(ValidationException.class);

        assertThrows(ValidationException.class, () -> filmController.create(film));
        verify(filmService, times(1)).create(film);
    }
}
