package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
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

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    private final Map<Long, Set<Long>> likesByFilm = new HashMap<>();

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public FilmService() {
        throw new IllegalStateException("FilmService должен создаваться Spring через DI-конструктор");
    }

    public Film create(Film film) {
        validateFilm(film);
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        validateFilm(film);
        getById(film.getId()); // 404 если фильма нет
        return filmStorage.update(film);
    }

    public Collection<Film> getAll() {
        return filmStorage.getAll();
    }

    public Film getById(Long id) {
        return filmStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id " + id + " не найден"));
    }

    public void addLike(Long filmId, Long userId) {
        getById(filmId);
        userStorage.getById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        likesByFilm.computeIfAbsent(filmId, id -> new HashSet<>()).add(userId);
    }

    public void removeLike(Long filmId, Long userId) {
        getById(filmId);
        userStorage.getById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + userId + " не найден"));

        Set<Long> likes = likesByFilm.getOrDefault(filmId, Collections.emptySet());
        if (!likes.contains(userId)) {
            throw new NotFoundException("Лайк от пользователя " + userId + " для фильма " + filmId + " не найден");
        }

        likes.remove(userId);
        if (likes.isEmpty()) {
            likesByFilm.remove(filmId);
        }
    }

    public List<Film> getPopularFilms(Integer count) {
        int limit = (count == null ? 10 : count);
        if (limit <= 0) {
            limit = 10;
        }

        return filmStorage.getAll().stream()
                .sorted(
                        Comparator.<Film>comparingInt(f ->
                                        likesByFilm.getOrDefault(f.getId(), Collections.emptySet()).size()
                                ).reversed()
                                .thenComparing(Film::getId)
                )
                .limit(limit)
                .collect(Collectors.toList());
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не должно быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Описание не должно быть больше 200 символов");
        }

        if (film.getReleaseDate() == null) {
            throw new ValidationException("Дата релиза обязательна");
        }

        LocalDate minDate = LocalDate.of(1895, 12, 28);
        if (film.getReleaseDate().isBefore(minDate)) {
            throw new ValidationException("Дата релиза не может быть раньше 28.12.1895");
        }

        if (film.getDuration() <= 0) {
            throw new ValidationException("Длительность должна быть положительной");
        }
    }
}