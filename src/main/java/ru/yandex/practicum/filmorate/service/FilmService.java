package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserService userService;
    private final DirectorService directorService;

    public Film create(Film film) {
        validateFilm(film);
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        validateFilm(film);

        Film existingFilm = filmStorage.getById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм не найден"));

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
        getById(filmId);
        userService.getById(userId);
        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        getById(filmId);
        userService.getById(userId);
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getMostPopularFilms(int count, Integer year, Long genreId) {
        return filmStorage.getMostPopularFilms(count, year, genreId);
    }

    public List<Film> getCommonFilms(Long userId, Long friendId) {
        if (userId == null || friendId == null) {
            return List.of();
        }

        userService.getById(userId);
        userService.getById(friendId);
        return filmStorage.getCommonFilms(userId, friendId);
    }

    public List<Film> getFilmsByDirector(Long directorId, String sortBy) {
        Director director = directorService.getById(directorId);

        if (!sortBy.equals("year") && !sortBy.equals("likes")) {
            throw new ValidationException("sortBy должен быть 'year' или 'likes'");
        }

        return filmStorage.getFilmsByDirector(director.getId(), sortBy);
    }

    public void deleteFilmById(Long filmId) {
        getById(filmId);
        filmStorage.delete(filmId);
    }

    private void validateFilm(Film film) {

        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Описание не может быть больше 200 символов");
        }

        LocalDate minDate = LocalDate.of(1895, 12, 28);

        if (film.getReleaseDate().isBefore(minDate)) {
            throw new ValidationException("Дата релиза слишком ранняя");
        }

        if (film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность должна быть положительной");
        }

        if (film.getGenres() != null) {
            Set<Genre> sortedGenres = new TreeSet<>(Comparator.comparing(Genre::getId));
            sortedGenres.addAll(film.getGenres());
            film.setGenres(new HashSet<>(sortedGenres));
        }
    }

    public List<Film> searchFilms(String query, String by) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        if (by == null || by.isBlank()) {
            throw new ValidationException("Параметр by не должен быть пустым");
        }

        Set<String> searchParams = Arrays.stream(by.toLowerCase().split(","))
                .map(String::trim)
                .filter(param -> !param.isBlank())
                .collect(Collectors.toSet());

        if (searchParams.isEmpty()) {
            throw new ValidationException("Параметр by не должен быть пустым");
        }

        if (!searchParams.stream().allMatch(param ->
                param.equals("title") || param.equals("director"))) {
            throw new ValidationException("Параметр by может содержать только title и/или director");
        }

        return filmStorage.searchFilms(query.trim(), by.toLowerCase());
    }
}