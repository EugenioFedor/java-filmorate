package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Film create(Film film) {
        if (film.getMpa() != null) {

            if (!mpaExists(film.getMpa().getId())) {
                throw new NotFoundException("MPA not found");
            }

        }
        if (film.getGenres() != null) {
            for (Genre genre : film.getGenres()) {

                Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM genres WHERE id = ?",
                        Integer.class,
                        genre.getId()
                );

                if (count == null || count == 0) {
                    throw new NotFoundException("Genre not found");
                }
            }
        }

        String sql = """
                INSERT INTO films (name, description, release_date, duration, mpa_id)
                VALUES (?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setLong(5, film.getMpa().getId());
            return ps;
        }, keyHolder);

        film.setId(keyHolder.getKey().longValue());
        saveGenres(film);
        return film;
    }

    @Override
    public Film update(Film film) {
        if (film.getMpa() != null) {

            if (!mpaExists(film.getMpa().getId())) {
                throw new NotFoundException("MPA not found");
            }

        }
        String sql = """
                UPDATE films
                SET name=?, description=?, release_date=?, duration=?, mpa_id=?
                WHERE id=?
                """;

        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id=?", film.getId());

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

        String sql = "SELECT * FROM films WHERE id=?";

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, id);

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
                LIMIT ?
                """;

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, count);

        for (Film film : films) {
            film.setGenres(getGenres(film.getId()));
            film.setMpa(getMpa(film.getMpa().getId()));
        }

        return films;
    }

    private void saveGenres(Film film) {

        if (film.getGenres() == null) return;

        Set<Long> uniqueGenres = new HashSet<>();

        for (Genre genre : film.getGenres()) {

            if (uniqueGenres.add(genre.getId())) {

                jdbcTemplate.update(
                        "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)",
                        film.getId(),
                        genre.getId());
            }
        }
    }

    private Set<Genre> getGenres(Long filmId) {

        List<Genre> genres = jdbcTemplate.query(
                "SELECT g.id, g.name " +
                        "FROM film_genres fg " +
                        "JOIN genres g ON fg.genre_id = g.id " +
                        "WHERE fg.film_id = ?",
                (rs, rowNum) -> {
                    Genre genre = new Genre();
                    genre.setId(rs.getLong("id"));
                    genre.setName(rs.getString("name"));
                    return genre;
                },
                filmId
        );

        return genres.stream()
                .sorted(Comparator.comparing(Genre::getId))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private MpaRating getMpa(Long mpaId) {

        String sql = "SELECT * FROM mpa_ratings WHERE id=?";

        return jdbcTemplate.queryForObject(sql, this::mapRowToMpa, mpaId);
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
    public void addLike(Long filmId, Long userId) {

        String sql = """
                INSERT INTO likes (film_id, user_id)
                VALUES (?, ?)
                """;

        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {

        String sql = """
                DELETE FROM likes
                WHERE film_id = ? AND user_id = ?
                """;

        jdbcTemplate.update(sql, filmId, userId);
    }

    private boolean mpaExists(Long id) {

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mpa_ratings WHERE id = ?",
                Integer.class,
                id
        );

        return count != null && count > 0;
    }

    @Override
    public Collection<Film> findAll() {
        String sql = "SELECT * FROM films ORDER BY id ASC";
        return jdbcTemplate.query(sql, this::mapRowToFilm);
    }
}