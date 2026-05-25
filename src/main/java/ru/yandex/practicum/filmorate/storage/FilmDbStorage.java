package ru.yandex.practicum.filmorate.storage;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.InternalServerException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;

import java.util.*;
import java.util.stream.Collectors;


@Repository
public class FilmDbStorage extends BaseDbStorage<Film> {

    public FilmDbStorage(NamedParameterJdbcTemplate jdbcTemplate, FilmRowMapper mapper) {
        super(jdbcTemplate, mapper);
    }

    public Film create(Film film) {
        String sql = """
                INSERT INTO films (name, description, release_date, duration, mpa_id)
                VALUES (:name, :description, :releaseDate, :duration, :mpaId)
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("mpaId", film.getMpa() != null ? film.getMpa().getId() : null);
        params.put("name", film.getName());
        params.put("description", film.getDescription());
        params.put("releaseDate", film.getReleaseDate());
        params.put("duration", film.getDuration());

        long id = insert(sql, params);

        film.setId(id);
        saveGenres(film);
        saveDirectors(film);

        return getFilmById(id).orElseThrow(
                () -> new InternalServerException("Не удалось получить фильм с id: " + id));
    }

    public Film update(Film film) {

        String sql = """
                UPDATE films
                SET name=:name,
                    description=:description,
                    release_date=:releaseDate,
                    duration=:duration,
                    mpa_id=:mpaId
                WHERE id=:id
                """;

        Map<String, Object> params = new HashMap<>();
        params.put("mpaId", film.getMpa() != null ? film.getMpa().getId() : null);
        params.put("name", film.getName());
        params.put("description", film.getDescription());
        params.put("releaseDate", film.getReleaseDate());
        params.put("duration", film.getDuration());
        params.put("id", film.getId());

        update(sql, params);

        removeFilmGenres(film.getId());
        saveGenres(film);

        removeDirectors(film.getId());
        saveDirectors(film);

        return getFilmById(film.getId()).orElseThrow(
                () -> new InternalServerException("Не удалось получить фильм с id: " + film.getId()));
    }

    public List<Film> getFilms() {

        String sql = """
                SELECT
                f.*,
                m.name AS mpa_name,
                COALESCE(ROUND(AVG(l.rate),2),0) AS overall_rate
                FROM films f
                LEFT JOIN mpa_ratings m ON f.mpa_id = m.id
                LEFT JOIN likes l ON f.id = l.film_id
                GROUP BY f.id
                """;

        List<Film> films = findMany(sql, Map.of());

        loadGenres(films);
        loadDirectors(films);

        return films;
    }

    public Optional<Film> getFilmById(Long id) {

        String sql = """
                SELECT
                f.*,
                m.name AS mpa_name,
                COALESCE(ROUND(AVG(l.rate),2),0) AS overall_rate
                FROM films f
                LEFT JOIN mpa_ratings m ON f.mpa_id = m.id
                LEFT JOIN likes l ON f.id = l.film_id
                WHERE f.id = :id
                GROUP BY f.id
                """;

        Optional<Film> film = findOne(sql, Map.of("id", id));
        film.ifPresent(f -> {
            loadGenres(List.of(f));
            loadDirectors(List.of(f));
        });

        return film;
    }

    public void addMark(Long filmId, Long userId, int mark) {
        String sql = """
                MERGE INTO likes (user_id, film_id, rate)
                KEY(user_id, film_id) VALUES (:userId, :filmId, :rate)
                """;
        Map<String, ?> params = Map.of("userId", userId, "filmId", filmId, "rate", mark);

        update(sql, params);
    }

    public double getRate(Long filmId) {
        String sql = """
                SELECT
                COALESCE(ROUND(AVG(l.rate),2),0) AS overall_rate
                FROM likes l
                WHERE film_id=:filmId
                """;
        Double result = jdbc.queryForObject(sql, Map.of("filmId", filmId), Double.class);

        return result == null ? 0.0 : result;
    }

    public List<Film> getMostPopularFilms(int limit, Integer year, Long genreId) {
        StringBuilder sql = new StringBuilder("""
                SELECT f.*,
                       m.name AS mpa_name,
                       COALESCE(ROUND(AVG(l.rate),2),0) AS overall_rate
                FROM films f
                LEFT JOIN mpa_ratings m ON f.mpa_id = m.id
                LEFT JOIN likes l ON f.id = l.film_id
                WHERE 1 = 1
                """);

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (year != null) {
            sql.append(" AND EXTRACT(YEAR FROM f.release_date) = :year ");
            params.addValue("year", year);
        }

        if (genreId != null) {
            sql.append("""
                    AND EXISTS (
                        SELECT 1
                        FROM film_genres fg
                        WHERE fg.film_id = f.id
                          AND fg.genre_id = :genreId
                    )
                    """);
            params.addValue("genreId", genreId);
        }

        sql.append("""
                GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id
                ORDER BY overall_rate DESC, f.id ASC
                LIMIT :limit
                """);

        params.addValue("limit", limit);

        List<Film> films = findMany(sql.toString(), params.getValues());

        loadGenres(films);
        loadDirectors(films);

        return films;
    }

    public void addLike(Long filmId, Long userId) {
        String sql = """
                MERGE INTO likes (film_id, user_id)
                KEY (film_id, user_id)
                VALUES (:filmId, :userId)
                """;
        Map<String, ?> params = Map.of("filmId", filmId, "userId", userId);
        update(sql, params);

        updateFeed(userId, filmId, "LIKE", "ADD");
    }

    public void removeLike(Long filmId, Long userId) {
        String sql = """
                DELETE FROM likes
                WHERE film_id = :filmId AND user_id = :userId
                """;
        Map<String, ?> params = Map.of("filmId", filmId, "userId", userId);
        update(sql, params);

        updateFeed(userId, filmId, "LIKE", "REMOVE");
    }

    public List<Film> getCommonFilms(Long userId, Long friendId) {
        String sql = """
                SELECT f.*,
                       m.name AS mpa_name,
                       COALESCE(ROUND(AVG(l.rate),2),0) AS overall_rate
                FROM films f
                LEFT JOIN mpa_ratings m ON f.mpa_id = m.id
                LEFT JOIN likes l ON f.id = l.film_id
                WHERE f.id IN (
                       SELECT film_id FROM likes WHERE user_id = :userId
                       INTERSECT
                       SELECT film_id FROM likes WHERE user_id = :friendId
                )
                GROUP BY f.id, m.name
                ORDER BY overall_rate DESC, f.id ASC
                """;
        Map<String, ?> params = Map.of("userId", userId, "friendId", friendId);

        List<Film> films = findMany(sql, params);

        loadGenres(films);
        loadDirectors(films);

        return films;
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO film_genres (film_id, genre_id)
                VALUES (:filmId, :genreId)
                """;

        SqlParameterSource[] batch = film.getGenres().stream()
                .map(genre -> new MapSqlParameterSource()
                        .addValue("filmId", film.getId())
                        .addValue("genreId", genre.getId())
                )
                .toArray(SqlParameterSource[]::new);

        jdbc.batchUpdate(sql, batch);
    }

    private void loadGenres(List<Film> films) {
        List<Long> ids = films.stream().map(Film::getId).toList();

        if (ids.isEmpty()) return;

        String getGenresSql = """
                SELECT fg.film_id, g.id, g.name
                FROM film_genres fg
                JOIN genres g ON fg.genre_id = g.id
                WHERE fg.film_id IN (:ids)
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource("ids", ids);
        Map<Long, Set<Genre>> genresByFilmId = new HashMap<>();

        jdbc.query(getGenresSql, parameters, rs -> {
            long filmId = rs.getLong("film_id");
            Genre genre = new Genre(rs.getLong("id"), rs.getString("name"));
            genresByFilmId.computeIfAbsent(filmId, k -> new HashSet<>()).add(genre);
        });

        for (Film film : films) {
            film.setGenres(genresByFilmId.getOrDefault(film.getId(), Collections.emptySet()));
        }
    }

    private void removeFilmGenres(long filmId) {
        String sql = """
                DELETE FROM film_genres
                WHERE film_id=:filmId
                """;

        jdbc.update(sql, Map.of("filmId", filmId));
    }

    private void saveDirectors(Film film) {
        if (film.getDirectors() == null || film.getDirectors().isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO film_directors (film_id, director_id)
                VALUES (:filmId, :directorId)
                """;

        SqlParameterSource[] batch = film.getDirectors().stream()
                .map(director -> new MapSqlParameterSource()
                        .addValue("filmId", film.getId())
                        .addValue("directorId", director.getId()))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(sql, batch);
    }

    private void loadDirectors(List<Film> films) {
        List<Long> ids = films.stream().map(Film::getId).toList();

        if (ids.isEmpty()) return;

        String getDirectorsSql = """
                SELECT fd.film_id, d.id, d.name
                FROM film_directors fd
                JOIN directors d ON fd.director_id = d.id
                WHERE fd.film_id IN (:ids)
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource("ids", ids);
        Map<Long, Set<Director>> directorsByFilmId = new HashMap<>();

        jdbc.query(getDirectorsSql, parameters, rs -> {
            long filmId = rs.getLong("film_id");
            Director director = new Director(rs.getLong("id"), rs.getString("name"));
            directorsByFilmId.computeIfAbsent(filmId, k -> new HashSet<>()).add(director);
        });

        for (Film film : films) {
            film.setDirectors(directorsByFilmId.getOrDefault(film.getId(), Collections.emptySet()));
        }
    }

    private void removeDirectors(long filmId) {
        String sql = """
                DELETE FROM film_directors
                WHERE film_id= :filmId
                """;
        jdbc.update(sql, Map.of("filmId", filmId));
    }

    public List<Film> getFilmsByDirector(Long directorId, SortBy sortBy) {
        String orderBy = getSortBy(sortBy);

        String sql = String.format("""
                SELECT f.*,
                       m.name AS mpa_name,
                       COALESCE(ROUND(AVG(l.rate),2),0) AS overall_rate
                FROM films f
                JOIN film_directors fd ON f.id = fd.film_id
                LEFT JOIN mpa_ratings m ON f.mpa_id = m.id
                LEFT JOIN likes l ON f.id = l.film_id
                WHERE fd.director_id = :directorId
                GROUP BY f.id, m.name
                ORDER BY %s, f.id ASC
                """, orderBy);

        List<Film> films = jdbc.query(
                sql,
                Map.of("directorId", directorId),
                new FilmRowMapper()
        );

        loadGenres(films);
        loadDirectors(films);

        return films;
    }

    private String getSortBy(SortBy sortBy) {
        return switch (sortBy) {
            case likes -> "COUNT(l.user_id) DESC";
            case rate -> "overall_rate DESC";
            case year -> "EXTRACT(YEAR FROM f.release_date)";
        };
    }

    public void deleteFilmById(Long filmId) {
        int rowsDeleted = jdbc.update("DELETE FROM films WHERE id = :filmId", Map.of("filmId", filmId));

        if (rowsDeleted == 0) {
            throw new NotFoundException("Фильм с id " + filmId + " не найден");
        }
    }

    public List<Film> searchFilms(String query, String by) {
        Set<String> searchParams = Arrays.stream(by.toLowerCase().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());

        List<String> conditions = new ArrayList<>();

        if (searchParams.contains("title")) {
            conditions.add("LOWER(f.name) LIKE LOWER(:query)");
        }

        if (searchParams.contains("director")) {
            conditions.add("LOWER(d.name) LIKE LOWER(:query)");
        }

        String sql = """
                SELECT f.*,
                       m.name AS mpa_name,
                       COALESCE(ROUND(AVG(l.rate),2),0) AS overall_rate
                FROM films f
                LEFT JOIN mpa_ratings m ON f.mpa_id = m.id
                LEFT JOIN film_directors fd ON f.id = fd.film_id
                LEFT JOIN directors d ON fd.director_id = d.id
                LEFT JOIN likes l ON f.id = l.film_id
                WHERE %s
                GROUP BY f.id, m.name
                ORDER BY COUNT(DISTINCT l.user_id) DESC, f.id ASC
                """.formatted(String.join(" OR ", conditions));

        Map<String, Object> params = Map.of(
                "query", "%" + query + "%"
        );

        List<Film> films = findMany(sql, params);
        loadGenres(films);
        loadDirectors(films);

        return films;
    }
}