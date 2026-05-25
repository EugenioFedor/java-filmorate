package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.MpaDbStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {

    private final FilmDbStorage filmStorage;
    private final GenreDbStorage genreStorage;
    private final MpaDbStorage mpaStorage;
    private final UserService userService;
    private final DirectorService directorService;

    public Film addFilm(Film film) {
        validateFilm(film, "new");

        Film storedFilm = filmStorage.create(film);
        log.info("Новый фильм: {}", storedFilm);

        return storedFilm;
    }

    public Film updateFilm(Film film) {
        validateFilm(film, "update");

        Film updatedFilm = filmStorage.update(film);
        log.info("Обновлены данные фильма: {}", updatedFilm);

        return updatedFilm;
    }

    public List<Film> getFilms() {
        return filmStorage.getFilms();
    }

    public Film getFilmById(Long id) {
        return filmStorage.getFilmById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id " + id + " не найден"));
    }

    public void addLike(Long filmId, Long userId) {
        Film storedFilm = getFilmById(filmId);
        User storedUser = userService.getById(userId);

        filmStorage.addLike(storedFilm.getId(), storedUser.getId());
        loggingFilmLikes(storedFilm.getId(), storedUser.getId(), null, "Добавлен лайк:");
    }

    public void addMark(Long filmId, Long userId, String mark) {
        Film storedFilm = getFilmById(filmId);
        User storedUser = userService.getById(userId);
        int rate = validateMark(mark);

        filmStorage.addMark(storedFilm.getId(), storedUser.getId(), rate);
        loggingFilmLikes(storedFilm.getId(), storedUser.getId(), rate, "Добавлена оценка:");
    }

    public void removeLike(Long filmId, Long userId) {
        Film storedFilm = getFilmById(filmId);
        User storedUser = userService.getById(userId);

        filmStorage.removeLike(storedFilm.getId(), storedUser.getId());
        loggingFilmLikes(storedFilm.getId(), storedUser.getId(), null, "Удален лайк:");
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

    public List<Film> getFilmsByDirectorId(Long directorId, SortBy sortBy) {
        Director director = directorService.getById(directorId);

        return filmStorage.getFilmsByDirector(director.getId(), sortBy);
    }

    public void deleteFilmById(Long filmId) {
        getFilmById(filmId);
        filmStorage.deleteFilmById(filmId);
    }

    private void validateFilm(Film film, String by) {
        if (by.equals("update")) {
            getFilmById(film.getId());
        }

        if (isWrongDate(film)) {
            throw new ValidationException("Дата релиза слишком ранняя");
        }

        validateGenres(film);
        validateRating(film);
    }

    private boolean isWrongDate(Film film) {
        return film.getReleaseDate() == null
                || film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28));
    }

    private void validateGenres(Film film) {
        List<Genre> genres = genreStorage.getGenres();

        Set<Long> incomeGenreIds = film.getGenres()
                .stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());

        Set<Long> existsGenreIds = genres.stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());

        Set<Long> missingIds = incomeGenreIds.stream()
                .filter(id -> !existsGenreIds.contains(id))
                .collect(Collectors.toSet());

        if (!missingIds.isEmpty()) {
            throw new NotFoundException("Жанры c id: " + missingIds + " не найдены.");
        }
    }

    private void validateRating(Film film) {
        if (film.getMpa() != null) {
            checkRatingExistsOrThrow(film.getMpa().getId());
        }
    }

    private void checkRatingExistsOrThrow(Long ratingId) {
        mpaStorage.getById(ratingId).orElseThrow(() ->
                new NotFoundException("Рейтинг с id = " + ratingId + " не найден.")
        );
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

    public int validateMark(String mark) {
        int result = 0;

        if (mark.contains(".")) {
            mark = mark.substring(0, mark.indexOf("."));
        }

        if (isNumeric(mark)) {
            result = Integer.parseInt(mark);
        }

        if (result < 1 || result > 10) {
            throw new ValidationException("Минимальная оценка: 1, максимальная: 10, было подано: " + mark
                    + " в result попало " + result);
        }

        return result;
    }

    private static boolean isNumeric(String str) {
        return str != null && str.matches("\\d+");
    }

    private void loggingFilmLikes(Long filmId, Long userId, Integer rate, String action) {
        double overallRate = getRate(filmId);
        if (rate != null) {
            log.info("{} film={}, user={}, rate={}, film overallrate={}", action, filmId, userId, rate, overallRate);
            return;
        }
        log.info("{} film={}, user={}, film overallrate={}", action, filmId, userId, overallRate);
    }

    private double getRate(Long filmId) {
        return filmStorage.getRate(filmId);
    }
}