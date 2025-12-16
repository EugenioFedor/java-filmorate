package ru.yandex.practicum.filmorate.service;

import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class FilmService {

    private final Map<Long, Film> films = new HashMap<>();
    private long nextId = 1;

    public Film create(Film film) {
        validate(film);
        film.setId(nextId++);
        films.put(film.getId(), film);
        return film;
    }

    public Film update(Film film) {
        validate(film);
        if (!films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм не найден");
        }
        films.put(film.getId(), film);
        return film;
    }

    public Collection<Film> getAll() {
        return films.values();
    }

    private void validate(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Длина описания не может быть больше 200 символов");
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ValidationException("Дата релиза слишком ранняя");
        }
        if (film.getDuration() <= 0) {
            throw new ValidationException("Длительность должна быть положительной");
        }
    }
}
