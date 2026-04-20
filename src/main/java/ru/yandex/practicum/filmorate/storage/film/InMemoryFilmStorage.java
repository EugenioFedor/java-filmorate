package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

@Component
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
        if (!films.containsKey(film.getId())) {
            throw new NotFoundException("Film not found");
        }
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
        if (!films.containsKey(filmId)) {
            throw new NotFoundException("Film not found");
        }
        likes.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        if (!films.containsKey(filmId)) {
            throw new NotFoundException("Film not found");
        }
        Set<Long> filmLikes = likes.get(filmId);
        if (filmLikes != null) {
            filmLikes.remove(userId);
        }
    }

    @Override
    public List<Film> getMostPopularFilms(int count, Integer year, Long genreId) {
        return films.values().stream()
                .sorted((f1, f2) -> Integer.compare(
                        getLikesCount(f2.getId()),
                        getLikesCount(f1.getId())))
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        return List.of();
    }

    private int getLikesCount(Long filmId) {
        Set<Long> filmLikes = likes.get(filmId);
        return filmLikes != null ? filmLikes.size() : 0;
    }

    private Set<Long> getUserLikes(Long userId) {
        Set<Long> userLikes = new HashSet<>();
        for (Map.Entry<Long, Set<Long>> entry : likes.entrySet()) {
            if (entry.getValue().contains(userId)) {
                userLikes.add(entry.getKey());
            }
        }
        return userLikes;
    }

    @Override
    public List<Film> getFilmsByDirector(Long directorId, String sortBy) {
        return new ArrayList<>();
    }


    @Override
    public void delete(Long filmId) {
        Film removed = films.remove(filmId);

        if (removed == null) {
            throw new NotFoundException("Film not found");
        }

        likes.remove(filmId);
    }

    @Override
    public List<Film> searchFilms(String query, String by) {
        return List.of();
    }
}