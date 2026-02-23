package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FilmService {

    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    // filmId -> set(userId)
    private final Map<Long, Set<Long>> likesByFilm = new HashMap<>();

    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film create(Film film) {
        validateFilm(film);
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        validateFilm(film);
        Long id = film.getId();
        if (id == null || filmStorage.getById(id).isEmpty()) {
            throw new NotFoundException("Фильм с id " + id + " не найден");
        }
        return filmStorage.update(film);
    }

    public List<Film> getAll() {
        return filmStorage.getAll();
    }

    public Film getById(Long id) {
        return filmStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id " + id + " не найден"));
    }

    public void addLike(Long filmId, Long userId) {
        // важно: проверяем, что пользователь существует в ТОМ ЖЕ хранилище (через DI)
        userStorage.getById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        filmStorage.getById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id " + filmId + " не найден"));

        likesByFilm.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
    }

    public void removeLike(Long filmId, Long userId) {
        userStorage.getById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));
        filmStorage.getById(filmId)
                .orElseThrow(() -> new NotFoundException("Фильм с id " + filmId + " не найден"));

        Set<Long> likes = likesByFilm.get(filmId);
        if (likes != null) {
            likes.remove(userId);
        }
    }

    public List<Film> getPopularFilms(Integer count) {
        int limit = (count == null) ? 10 : count;

        return filmStorage.getAll().stream()
                .sorted(Comparator
                        .comparingInt((Film f) -> likesByFilm.getOrDefault(f.getId(), Set.of()).size()).reversed()
                        .thenComparingLong(Film::getId))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void validateFilm(Film film) {
        if (film == null) {
            throw new ValidationException("Фильм не должен быть null");
        }
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не должно быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Описание не должно быть больше 200 символов");
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            throw new ValidationException("Дата релиза не может быть раньше 28.12.1895");
        }
        if (film.getDuration() <= 0) {
            throw new ValidationException("Длительность должна быть положительной");
        }
    }
}