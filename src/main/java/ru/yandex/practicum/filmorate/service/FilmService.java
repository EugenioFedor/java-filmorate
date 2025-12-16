package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class FilmService {

    private final Map<Long, Film> films = new HashMap<>();
    private long nextId = 1L;

    public Collection<Film> findAll() {
        return films.values();
    }

    public Film findById(Long id) {
        validateId(id);
        Film film = films.get(id);
        if (film == null) throw new NoSuchElementException("Фильм с id=" + id + " не найден");
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
        validateId(film.getId());

        if (!films.containsKey(film.getId())) {
            throw new NoSuchElementException("Фильм не найден");
        }

        films.put(film.getId(), film);
        return film;
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new IllegalArgumentException("Название фильма не может быть пустым");
        }
        if (film.getReleaseDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Дата релиза не может быть в будущем");
        }
        if (film.getDuration() <= 0) {
            throw new IllegalArgumentException("Длительность должна быть положительной");
        }
    }

    private void validateId(Long id) {
        if (id <= 0) throw new IllegalArgumentException("Id должен быть положительным");
    }
}
