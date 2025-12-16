package ru.yandex.practicum.filmorate.controller;

import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.*;

public class FilmController {

    private final Map<Long, Film> films = new HashMap<>();
    private long nextId = 1L;

    public Collection<Film> findAll() {
        return films.values();
    }

    public Film findById(Long id) {
        Film film = films.get(id);
        if (film == null) throw new NoSuchElementException("Фильм не найден");
        return film;
    }

    public Film create(Film film) {
        validateFilm(film);
        film.setId(nextId++);
        films.put(film.getId(), film);
        return film;
    }

    public Film update(Film film) {
        validateFilm(film);
        if (!films.containsKey(film.getId())) {
            throw new NoSuchElementException("Фильм не найден");
        }
        films.put(film.getId(), film);
        return film;
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Описание слишком длинное");
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ValidationException("Дата релиза слишком ранняя");
        }
        if (film.getDuration() <= 0) {
            throw new ValidationException("Длительность должна быть положительной");
        }
    }
}
