package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Primary
@Repository
public class FilmDbStorage implements FilmStorage {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public Film create(Film film) {

        if (film.getMpa() != null && !mpaExists(film.getMpa().getId())) {
            throw new NotFoundException("MPA not found");
        }

        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {
                if (!genreExists(genre.getId())) {
                    throw new NotFoundException("Genre not found");
                }
            }
        }

        // ← ДОБАВИТЬ: проверка режиссёров
        if (film.getDirectors() != null) {
            for (Director director : film.getDirectors()) {
                if (!directorExists(director.getId())) {
                    throw new NotFoundException("Director with id " + director.getId() + " not found");
                }
            }
        }

        String sql = """
                INSERT INTO films (name, description, release_date, duration, mpa_id)
                VALUES (:name, :description, :releaseDate, :duration, :mpaId)
                """;

        Map<String, Object> params = Map.of(
                "name", film.getName(),
                "description", film.getDescription(),
                "releaseDate", Date.valueOf(film.getReleaseDate()),
                "duration", film.getDuration(),
                "mpaId", film.getMpa().getId()
        );

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(sql,
                new MapSqlParameterSource(params),
                keyHolder);

        film.setId(keyHolder.getKey().longValue());
        film.setMpa(getMpa(film.getMpa().getId()));

        saveGenres(film);
        saveDirectors(film);  // ← ДОБАВИТЬ

        loadGenres(List.of(film));
        loadDirectors(List.of(film));  // ← ДОБАВИТЬ

        return film;
    }

    @Override
    public Film update(Film film) {

        if (film.getMpa() != null && !mpaExists(film.getMpa().getId())) {
            throw new NotFoundException("MPA not found");
        }

        // ← ДОБАВИТЬ: проверка режиссёров
        if (film.getDirectors() != null) {
            for (Director director : film.getDirectors()) {
                if (!directorExists(director.getId())) {
                    throw new NotFoundException("Director with id " + director.getId() + " not found");
                }
            }
        }

        String sql = """
                UPDATE films
                SET name=:name,
                    description=:description,
                    release_date=:releaseDate,
                    duration=:duration,
                    mpa_id=:mpaId
                WHERE id=:id
                """;

        Map<String, Object> params = Map.of(
                "name", film.getName(),
                "description", film.getDescription(),
                "releaseDate", Date.valueOf(film.getReleaseDate()),
                "duration", film.getDuration(),
                "mpaId", film.getMpa().getId(),
                "id", film.getId()
        );

        int updated = jdbcTemplate.update(sql, params);

        if (updated == 0) {
            throw new NotFoundException("Film not found");
        }

        jdbcTemplate.update(
                "DELETE FROM film_genres WHERE film_id = :id",
                Map.of("id", film.getId())
        );

        // ← ДОБАВИТЬ: удаление старых режиссёров
        jdbcTemplate.update(
                "DELETE FROM film_directors WHERE film_id = :id",
                Map.of("id", film.getId())
        );

        saveGenres(film);
        saveDirectors(film);  // ← ДОБАВИТЬ

        return getById(film.getId()).orElseThrow();
    }

    @Override
    public List<Film> getAll() {

        String sql = "SELECT * FROM films";

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);

        for (Film film : films) {
            film.setGenres(getGenres(film.getId()));
            film.setMpa(getMpa(film.getMpa().getId()));
            film.setDirectors(getDirectors(film.getId()));
        }

        return films;
    }

    @Override
    public Optional<Film> getById(Long id) {

        String sql = "SELECT * FROM films WHERE id = :id";

        List<Film> films = jdbcTemplate.query(sql,
                Map.of("id", id),
                this::mapRowToFilm);

        if (films.isEmpty()) {
            return Optional.empty();
        }

        Film film = films.get(0);

        film.setGenres(getGenres(id));
        film.setMpa(getMpa(film.getMpa().getId()));
        film.setDirectors(getDirectors(id));

        return Optional.of(film);
    }

    @Override
    public List<Film> getMostPopularFilms(int limit, Integer year, Long genreId) {
        StringBuilder sql = new StringBuilder("""
                SELECT f.id,
                       f.name,
                       f.description,
                       f.release_date,
                       f.duration,
                       f.mpa_id
                FROM films f
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
                ORDER BY COUNT(l.user_id) DESC, f.id ASC
                LIMIT :limit
                """);

        params.addValue("limit", limit);

        List<Film> films = jdbcTemplate.query(sql.toString(), params, this::mapRowToFilm);
        loadGenres(films);
        loadDirectors(films);

        return films;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        jdbcTemplate.update(
                """
                        MERGE INTO likes (film_id, user_id)
                        KEY (film_id, user_id)
                        VALUES (:filmId, :userId)
                        """,
                Map.of(
                        "filmId", filmId,
                        "userId", userId
                )
        );

        jdbcTemplate.update(
                "INSERT INTO feed (user_id, entity_id, event_type, operation, timestamp) VALUES (:userId, :filmId, 'LIKE', 'ADD', :ts)",
                Map.of(
                        "userId", userId,
                        "filmId", filmId,
                        "ts", System.currentTimeMillis()
                )
        );
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        jdbcTemplate.update(
                "DELETE FROM likes WHERE film_id = :filmId AND user_id = :userId",
                Map.of(
                        "filmId", filmId,
                        "userId", userId
                )
        );

        jdbcTemplate.update(
                "INSERT INTO feed (user_id, entity_id, event_type, operation, timestamp) VALUES (:userId, :filmId, 'LIKE', 'REMOVE', :ts)",
                Map.of(
                        "userId", userId,
                        "filmId", filmId,
                        "ts", System.currentTimeMillis()
                )
        );
    }

    @Override
    public List<Film> getCommonFilms(Long userId, Long friendId) {
        String sql = """
                SELECT f.id,
                       f.name,
                       f.description,
                       f.release_date,
                       f.duration,
                       f.mpa_id
                FROM films f
                LEFT JOIN likes l ON f.id = l.film_id
                WHERE f.id IN (
                       SELECT film_id FROM likes WHERE user_id = :userId
                       INTERSECT
                       SELECT film_id FROM likes WHERE user_id = :friendId
                )
                GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id
                ORDER BY COUNT(l.user_id) DESC, f.id ASC
                """;

        List<Film> films = jdbcTemplate.query(
                sql,
                Map.of("userId", userId, "friendId", friendId),
                this::mapRowToFilm
        );

        loadGenres(films);
        loadDirectors(films);

        return films;
    }

    private void saveGenres(Film film) {

        if (film.getGenres() == null) return;

        Set<Long> uniqueGenres = new HashSet<>();

        for (Genre genre : film.getGenres()) {
            if (uniqueGenres.add(genre.getId())) {

                jdbcTemplate.update(
                        "INSERT INTO film_genres (film_id, genre_id) VALUES (:filmId, :genreId)",
                        Map.of(
                                "filmId", film.getId(),
                                "genreId", genre.getId()
                        )
                );
            }
        }
    }

    private Set<Genre> getGenres(Long filmId) {

        List<Genre> genres = jdbcTemplate.query(
                """
                        SELECT g.id, g.name
                        FROM film_genres fg
                        JOIN genres g ON fg.genre_id = g.id
                        WHERE fg.film_id = :filmId
                        """,
                Map.of("filmId", filmId),
                this::mapRowToGenre
        );

        return genres.stream()
                .sorted(Comparator.comparing(Genre::getId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private MpaRating getMpa(Long mpaId) {

        return jdbcTemplate.queryForObject(
                "SELECT * FROM mpa_ratings WHERE id = :id",
                Map.of("id", mpaId),
                this::mapRowToMpa
        );
    }

    private boolean mpaExists(Long id) {

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mpa_ratings WHERE id = :id",
                Map.of("id", id),
                Integer.class
        );

        return count != null && count > 0;
    }

    private boolean genreExists(Long id) {

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM genres WHERE id = :id",
                Map.of("id", id),
                Integer.class
        );

        return count != null && count > 0;
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {

        Film film = new Film();

        film.setId(rs.getLong("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));
        film.setMpa(getMpa(rs.getLong("mpa_id")));

        return film;
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {

        Genre genre = new Genre();
        genre.setId(rs.getLong("id"));
        genre.setName(rs.getString("name"));

        return genre;
    }

    private MpaRating mapRowToMpa(ResultSet rs, int rowNum) throws SQLException {

        MpaRating mpa = new MpaRating();
        mpa.setId(rs.getLong("id"));
        mpa.setName(rs.getString("name"));

        return mpa;
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

        jdbcTemplate.query(getGenresSql, parameters, rs -> {
            long filmId = rs.getLong("film_id");
            Genre genre = new Genre(rs.getLong("id"), rs.getString("name"));
            genresByFilmId.computeIfAbsent(filmId, k -> new HashSet<>()).add(genre);
        });

        for (Film film : films) {
            film.setGenres(genresByFilmId.getOrDefault(film.getId(), Collections.emptySet()));
        }
    }

    private void saveDirectors(Film film) {
        if (film.getDirectors() == null) return;

        Set<Long> uniqueDirectors = new HashSet<>();

        for (Director director : film.getDirectors()) {
            if (uniqueDirectors.add(director.getId())) {
                jdbcTemplate.update(
                        "INSERT INTO film_directors (film_id, director_id) VALUES (:filmId, :directorId)",
                        Map.of(
                                "filmId", film.getId(),
                                "directorId", director.getId()
                        )
                );
            }
        }
    }

    // Метод для получения режиссёров одного фильма
    private Set<Director> getDirectors(Long filmId) {
        List<Director> directors = jdbcTemplate.query(
                """
                        SELECT d.id, d.name
                        FROM film_directors fd
                        JOIN directors d ON fd.director_id = d.id
                        WHERE fd.film_id = :filmId
                        """,
                Map.of("filmId", filmId),
                this::mapRowToDirector
        );

        return directors.stream()
                .sorted(Comparator.comparing(Director::getId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // Метод для загрузки режиссёров для списка фильмов (оптимизация N+1)
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

        jdbcTemplate.query(getDirectorsSql, parameters, rs -> {
            long filmId = rs.getLong("film_id");
            Director director = new Director(rs.getLong("id"), rs.getString("name"));
            directorsByFilmId.computeIfAbsent(filmId, k -> new HashSet<>()).add(director);
        });

        for (Film film : films) {
            film.setDirectors(directorsByFilmId.getOrDefault(film.getId(), Collections.emptySet()));
        }
    }

    // Маппер для режиссёра
    private Director mapRowToDirector(ResultSet rs, int rowNum) throws SQLException {
        Director director = new Director();
        director.setId(rs.getLong("id"));
        director.setName(rs.getString("name"));
        return director;
    }

    // Проверка существования режиссёра
    private boolean directorExists(Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM directors WHERE id = :id",
                Map.of("id", id),
                Integer.class
        );
        return count != null && count > 0;
    }

    @Override
    public List<Film> getFilmsByDirector(Long directorId, String sortBy) {
        String orderBy = sortBy.equals("year")
                ? "f.release_date ASC, f.id ASC"
                : "COUNT(l.user_id) DESC, f.id ASC";

        String sql = String.format("""
                SELECT f.id,
                       f.name,
                       f.description,
                       f.release_date,
                       f.duration,
                       f.mpa_id
                FROM films f
                JOIN film_directors fd ON f.id = fd.film_id
                LEFT JOIN likes l ON f.id = l.film_id
                WHERE fd.director_id = :directorId
                GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id
                ORDER BY %s
                """, orderBy);

        List<Film> films = jdbcTemplate.query(
                sql,
                Map.of("directorId", directorId),
                this::mapRowToFilm
        );

        loadGenres(films);
        loadDirectors(films);

        return films;
    }

    @Override
    public void delete(Long filmId) {
        int rowsDeleted = jdbcTemplate.update("DELETE FROM films WHERE id = :filmId", Map.of("filmId", filmId));

        if (rowsDeleted == 0) {
            throw new NotFoundException("Фильм с id " + filmId + " не найден");
        }
    }

    @Override
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
                SELECT f.id,
                       f.name,
                       f.description,
                       f.release_date,
                       f.duration,
                       f.mpa_id
                FROM films f
                LEFT JOIN film_directors fd ON f.id = fd.film_id
                LEFT JOIN directors d ON fd.director_id = d.id
                LEFT JOIN likes l ON f.id = l.film_id
                WHERE %s
                GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id
                ORDER BY COUNT(DISTINCT l.user_id) DESC, f.id ASC
                """.formatted(String.join(" OR ", conditions));

        Map<String, Object> params = Map.of(
                "query", "%" + query + "%"
        );

        List<Film> films = jdbcTemplate.query(sql, params, this::mapRowToFilm);
        loadGenres(films);
        loadDirectors(films);

        return films;
    }
}