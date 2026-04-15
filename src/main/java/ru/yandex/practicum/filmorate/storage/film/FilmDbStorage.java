package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
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
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

@RequiredArgsConstructor
@Repository
public class FilmDbStorage implements FilmStorage {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final DirectorStorage directorStorage;

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

        saveDirectors(film);

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

        jdbcTemplate.update(
                "DELETE FROM film_directors WHERE film_id = :id",
                Map.of("id", film.getId())
        );

        saveGenres(film);
        saveDirectors(film);

        return getById(film.getId()).orElseThrow();
    }

    @Override
    public List<Film> getAll() {

        String sql = "SELECT * FROM films";

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);

        for (Film film : films) {
            film.setGenres(getGenres(film.getId()));
            film.setDirectors(getDirectors(film.getId()));
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
        film.setDirectors(getDirectors(id));
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
            film.setDirectors(getDirectors(film.getId()));
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

    @Override
    public List<Film> getFilmsByDirector(Long directorId, String sortBy) {
        // Проверяем существование режиссёра
        directorStorage.getById(directorId)
                .orElseThrow(() -> new ru.yandex.practicum.filmorate.exception.NotFoundException(
                        "Режиссёр с id " + directorId + " не найден"));

        String sql;

        if ("year".equals(sortBy)) {
            sql = """
                    SELECT f.*
                    FROM films f
                    JOIN film_directors fd ON f.id = fd.film_id
                    WHERE fd.director_id = :directorId
                    ORDER BY f.release_date ASC
                    """;
        } else if ("likes".equals(sortBy)) {
            sql = """
                    SELECT f.*
                    FROM films f
                    JOIN film_directors fd ON f.id = fd.film_id
                    LEFT JOIN (
                        SELECT film_id, COUNT(user_id) AS likes_count
                        FROM likes
                        GROUP BY film_id
                    ) l ON f.id = l.film_id
                    WHERE fd.director_id = :directorId
                    ORDER BY COALESCE(l.likes_count, 0) DESC
                    """;
        } else {
            throw new ru.yandex.practicum.filmorate.exception.ValidationException(
                    "Параметр sortBy должен быть 'year' или 'likes'");
        }

        List<Film> films = jdbcTemplate.query(sql,
                Map.of("directorId", directorId),
                this::mapRowToFilm);

        for (Film film : films) {
            film.setGenres(getGenres(film.getId()));
            film.setDirectors(getDirectors(film.getId()));
            film.setMpa(getMpa(film.getMpa().getId()));
        }

        return films;
    }

    private void saveDirectors(Film film) {
        if (film.getDirectors() == null) return;

        Set<Long> uniqueDirectors = new HashSet<>();

        for (Director director : film.getDirectors()) {
            if (uniqueDirectors.add(director.getId())) {
                // Проверяем существование режиссёра
                directorStorage.getById(director.getId())
                        .orElseThrow(() -> new ru.yandex.practicum.filmorate.exception.NotFoundException(
                                "Режиссёр с id " + director.getId() + " не найден"));

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

    private Director mapRowToDirector(ResultSet rs, int rowNum) throws SQLException {
        Director director = new Director();
        director.setId(rs.getLong("id"));
        director.setName(rs.getString("name"));
        return director;
    }
}