package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;

public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new HashMap<>();
    private final Map<Long, Set<Long>> likes = new HashMap<>();

    private long nextId = 1;

    @Override
    public Film create(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) {
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public List<Film> getAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Optional<Film> getById(Long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        likes.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        Set<Long> filmLikes = likes.get(filmId);
        if (filmLikes != null) {
            filmLikes.remove(userId);
        }
    }

    @Override
    public List<Film> getPopular(int count) {
        return films.values().stream()
                .sorted((f1, f2) -> Integer.compare(
                        likes.getOrDefault(f2.getId(), Set.of()).size(),
                        likes.getOrDefault(f1.getId(), Set.of()).size()))
                .limit(count)
                .toList();
    }
}