package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.*;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


@Repository
@Primary
@RequiredArgsConstructor
public class UserDbStorage {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<User> userMapper = this::mapRowToUser;

    public User create(User user) {

        String sql = """
                INSERT INTO users (email, login, name, birthday)
                VALUES (?, ?, ?, ?)
                """;

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});

            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());

            if (user.getBirthday() != null) {
                ps.setDate(4, Date.valueOf(user.getBirthday()));
            } else {
                ps.setNull(4, java.sql.Types.DATE);
            }

            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key != null) {
            user.setId(key.longValue());
        }

        return user;
    }

    public User update(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";

        int rowsUpdated = jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);

            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());

            // 🔥 ОБРАБОТКА NULL (ключевой фикс)
            if (user.getBirthday() != null) {
                ps.setDate(4, Date.valueOf(user.getBirthday()));
            } else {
                ps.setNull(4, java.sql.Types.DATE);
            }

            ps.setLong(5, user.getId());

            return ps;
        });

        if (rowsUpdated == 0) {
            throw new NotFoundException("Пользователь с id " + user.getId() + " не найден");
        }

        return user;
    }

    public Collection<User> getAll() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, userMapper);
    }

    public Optional<User> getById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        List<User> users = jdbcTemplate.query(sql, userMapper, id);

        if (users.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(users.get(0));
    }

    public void addFriend(Long userId, Long friendId) {
        String sql = """
                MERGE INTO friendships (user_id, friend_id, status)
                KEY (user_id, friend_id)
                VALUES (?, ?, 'CONFIRMED')
                """;
        jdbcTemplate.update(sql, userId, friendId);

        jdbcTemplate.update(
                "INSERT INTO feed (user_id, entity_id, event_type, operation, timestamp) VALUES (?, ?, ?, ?, ?)",
                userId, friendId, "FRIEND", "ADD", System.currentTimeMillis()
        );
    }

    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);

        jdbcTemplate.update(
                "INSERT INTO feed (user_id, entity_id, event_type, operation, timestamp) VALUES (?, ?, ?, ?, ?)",
                userId, friendId, "FRIEND", "REMOVE", System.currentTimeMillis()
        );
    }

    public Collection<User> getFriends(Long userId) {
        String sql = """
                SELECT u.*
                FROM users u
                JOIN friendships f ON u.id = f.friend_id
                WHERE f.user_id = ?
                """;

        return jdbcTemplate.query(sql, userMapper, userId);
    }

    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        String sql = """
                SELECT u.*
                FROM users u
                JOIN friendships f1 ON u.id = f1.friend_id
                JOIN friendships f2 ON u.id = f2.friend_id
                WHERE f1.user_id = ? AND f2.user_id = ?
                """;

        return jdbcTemplate.query(sql, userMapper, userId, otherId);
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));

        Date date = rs.getDate("birthday");
        if (date != null) {
            user.setBirthday(date.toLocalDate());
        } else {
            user.setBirthday(null);
        }

        return user;
    }

    public Set<Long> getFriendsIds(Long id) {

        String sql = """
                SELECT friend_id
                FROM friendships
                WHERE user_id = ?
                """;

        return new HashSet<>(jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getLong("friend_id"), id));
    }

    public List<Film> getRecommendations(Long userId) {
        String findSimilarUserSql = """
                SELECT l2.user_id, COUNT(*) as common_likes
                FROM likes l1
                JOIN likes l2 ON l1.film_id = l2.film_id
                WHERE l1.user_id = ? AND l2.user_id != ?
                GROUP BY l2.user_id
                ORDER BY common_likes DESC
                LIMIT 1
                """;

        List<Long> similarUsers = jdbcTemplate.query(
                findSimilarUserSql,
                (rs, rowNum) -> rs.getLong("user_id"),
                userId,
                userId
        );

        if (similarUsers.isEmpty()) {
            return new ArrayList<>();
        }

        Long similarUserId = similarUsers.get(0);

        String recommendationsSql = """
                SELECT f.*, m.name as mpa_name
                FROM films f
                JOIN mpa_ratings m ON f.mpa_id = m.id
                WHERE f.id IN (
                    SELECT film_id
                    FROM likes
                    WHERE user_id = ?
                )
                AND f.id NOT IN (
                    SELECT film_id
                    FROM likes
                    WHERE user_id = ?
                )
                """;

        List<Film> recommendations = jdbcTemplate.query(
                recommendationsSql,
                this::mapRowToFilm,
                similarUserId,
                userId
        );

        loadGenresForFilms(recommendations);
        loadDirectorsForFilms(recommendations);

        return recommendations;
    }

    private void loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) return;

        List<Long> filmIds = films.stream().map(Film::getId).toList();

        String sql = """
                SELECT fg.film_id, g.id, g.name
                FROM film_genres fg
                JOIN genres g ON fg.genre_id = g.id
                WHERE fg.film_id IN (?)
                """;

        // Используем простой подход с циклом для совместимости с JdbcTemplate
        Map<Long, Set<Genre>> genresByFilmId = new HashMap<>();

        for (Long filmId : filmIds) {
            List<Genre> genres = jdbcTemplate.query(
                    """
                            SELECT g.id, g.name
                            FROM film_genres fg
                            JOIN genres g ON fg.genre_id = g.id
                            WHERE fg.film_id = ?
                            """,
                    (rs, rowNum) -> {
                        Genre genre = new Genre();
                        genre.setId(rs.getLong("id"));
                        genre.setName(rs.getString("name"));
                        return genre;
                    },
                    filmId
            );
            genresByFilmId.put(filmId, new LinkedHashSet<>(genres));
        }

        for (Film film : films) {
            film.setGenres(genresByFilmId.getOrDefault(film.getId(), Collections.emptySet()));
        }
    }

    private void loadDirectorsForFilms(List<Film> films) {
        if (films.isEmpty()) return;

        List<Long> filmIds = films.stream().map(Film::getId).toList();

        Map<Long, Set<Director>> directorsByFilmId = new HashMap<>();

        for (Long filmId : filmIds) {
            List<Director> directors = jdbcTemplate.query(
                    """
                            SELECT d.id, d.name
                            FROM film_directors fd
                            JOIN directors d ON fd.director_id = d.id
                            WHERE fd.film_id = ?
                            """,
                    (rs, rowNum) -> {
                        Director director = new Director();
                        director.setId(rs.getLong("id"));
                        director.setName(rs.getString("name"));
                        return director;
                    },
                    filmId
            );
            directorsByFilmId.put(filmId, new LinkedHashSet<>(directors));
        }

        for (Film film : films) {
            film.setDirectors(directorsByFilmId.getOrDefault(film.getId(), Collections.emptySet()));
        }
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
        try {
            mpa.setName(rs.getString("mpa_name"));
        } catch (SQLException e) {
            mpa = getMpaRating(mpa.getId());
        }
        film.setMpa(mpa);

        return film;
    }

    private MpaRating getMpaRating(Long mpaId) {
        return jdbcTemplate.queryForObject(
                "SELECT id, name FROM mpa_ratings WHERE id = ?",
                (rs, rowNum) -> {
                    MpaRating mpa = new MpaRating();
                    mpa.setId(rs.getLong("id"));
                    mpa.setName(rs.getString("name"));
                    return mpa;
                },
                mpaId
        );
    }

    public void delete(Long userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        int deleted = jdbcTemplate.update(sql, userId);

        if (deleted == 0) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
    }

    public List<Event> getFeed(Long userId) {
        String sql = """
                SELECT event_id, user_id, entity_id, event_type, operation, timestamp
                FROM feed
                WHERE user_id = ?
                ORDER BY timestamp ASC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Event event = new Event();
            event.setEventId(rs.getLong("event_id"));
            event.setUserId(rs.getLong("user_id"));
            event.setEntityId(rs.getLong("entity_id"));
            event.setEventType(rs.getString("event_type"));
            event.setOperation(rs.getString("operation"));
            event.setTimestamp(rs.getLong("timestamp"));
            return event;
        }, userId);
    }
}
