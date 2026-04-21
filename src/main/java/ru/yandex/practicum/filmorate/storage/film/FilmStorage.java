package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    List<Film> getAll();

    Optional<Film> getById(Long id);

    Film create(Film film);

    Film update(Film film);

    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);

    List<Film> getMostPopularFilms(int count, Integer year, Long genreId);

    List<Film> getCommonFilms(Long userId, Long friendId);

    List<Film> getFilmsByDirector(Long directorId, String sortBy);

    void delete(Long filmId);

    List<Film> searchFilms(String query, String by);
}
