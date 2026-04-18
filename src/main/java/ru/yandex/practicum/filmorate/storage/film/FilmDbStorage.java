package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
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

        saveGenres(film);

        return film;
    }

    @Override
    public Film update(Film film) {

        if (film.getMpa() != null && !mpaExists(film.getMpa().getId())) {
            throw new NotFoundException("MPA not found");
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

        saveGenres(film);

        return getById(film.getId()).orElseThrow();
    }

    @Override
    public List<Film> getAll() {

        String sql = "SELECT * FROM films";

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);

        for (Film film : films) {
            film.setGenres(getGenres(film.getId()));
            film.setMpa(getMpa(film.getMpa().getId()));
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

        return Optional.of(film);
    }

    @Override
    public List<Film> getPopular(int count) {

        String sql = """
                SELECT f.*
                FROM films f
                LEFT JOIN (
                    SELECT film_id, COUNT(user_id) AS likes_count
                    FROM likes
                    GROUP BY film_id
                ) l ON f.id = l.film_id
                ORDER BY COALESCE(l.likes_count, 0) DESC, f.id ASC
                LIMIT :count
                """;

        List<Film> films = jdbcTemplate.query(sql,
                Map.of("count", count),
                this::mapRowToFilm);

        for (Film film : films) {
            film.setGenres(getGenres(film.getId()));
            film.setMpa(getMpa(film.getMpa().getId()));
        }

        return films;
    }

    @Override
    public void addLike(Long filmId, Long userId) {

        jdbcTemplate.update(
                "INSERT INTO likes (film_id, user_id) VALUES (:filmId, :userId)",
                Map.of(
                        "filmId", filmId,
                        "userId", userId
                )
        );

        long timestamp = System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO feed (user_id, timestamp, event_type, operation, entity_id) " +
                        "VALUES (:userId, :timestamp, :eventType, :operation, :filmId)",
                Map.of(
                        "userId", userId,
                        "timestamp", timestamp,
                        "eventType", "LIKE",
                        "operation", "ADD",
                        "filmId", filmId

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

        long timestamp = System.currentTimeMillis();
        jdbcTemplate.update(
                "INSERT INTO feed (user_id, timestamp, event_type, operation, entity_id) " +
                        "VALUES (:userId, :timestamp, :eventType, :operation, :filmId)",
                Map.of(
                        "userId", userId,
                        "timestamp", timestamp,
                        "eventType", "LIKE",
                        "operation", "REMOVE",
                        "filmId", filmId

                )
        );
    }

    @Override
    public Collection<Film> findAll() {
        return getAll();
    }

    // ---------- PRIVATE ----------

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

        MpaRating mpa = new MpaRating();
        mpa.setId(rs.getLong("mpa_id"));

        film.setMpa(mpa);

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
}