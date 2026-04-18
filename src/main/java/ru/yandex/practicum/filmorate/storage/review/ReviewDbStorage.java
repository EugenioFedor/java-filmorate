package ru.yandex.practicum.filmorate.storage.review;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.InternalServerException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.*;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewDbStorage {

    private final JdbcTemplate jdbc;
    private final RowMapper<Review> mapper = this::mapRowToReview;

    public Review addReview(Review review) {
        String sql = """
                INSERT INTO reviews(user_id, film_id, is_positive, useful, review, created_at)
                VALUES (?,?,?,?,?,?)
                """;

        long id = insert(sql,
                review.getUserId(),
                review.getFilmId(),
                review.getIsPositive(),
                review.getUseful(),
                review.getContent(),
                review.getCreatedAt()
        );

        review.setReviewId(id);

        String insertFeedSql = "INSERT INTO feed (user_id, timestamp, event_type, operation, entity_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        long timestamp = System.currentTimeMillis();
        jdbc.update(insertFeedSql, review.getUserId(), timestamp, "REVIEW", "ADD", id);

        return review;
    }

    public Review updateReview(Review review) {
        String sql = """
                UPDATE reviews SET
                is_positive=?,
                useful=?,
                review=?,
                updated_at=?
                WHERE id=?
                """;

        update(sql,
                review.getIsPositive(),
                review.getUseful(),
                review.getContent(),
                review.getUpdatedAt(),
                review.getReviewId()
        );

        String insertFeedSql = "INSERT INTO feed (user_id, timestamp, event_type, operation, entity_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        long timestamp = System.currentTimeMillis();
        jdbc.update(insertFeedSql, review.getUserId(), timestamp, "REVIEW", "UPDATE", review.getReviewId());

        return review;
    }

    public void deleteReviewById(long reviewId) {
        Review review = getReviewById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        String removeReviewSql = "DELETE FROM reviews WHERE id=?";
        update(removeReviewSql, reviewId);

        String insertFeedSql = "INSERT INTO feed (user_id, timestamp, event_type, operation, entity_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        long timestamp = System.currentTimeMillis();
        jdbc.update(insertFeedSql, review.getUserId(), timestamp, "REVIEW", "REMOVE", reviewId);
    }

    public Optional<Review> getReviewById(long reviewId) {
        String sql = "SELECT * FROM reviews WHERE id=?";
        return findOne(sql, reviewId);
    }

    public List<Review> getReviewsByFilmId(long filmId, int count) {
        String sql = """
                SELECT * FROM reviews
                WHERE film_id=?
                ORDER BY useful DESC, id ASC
                LIMIT ?
                """;
        return findMany(sql, filmId, count);
    }

    public List<Review> getReviews() {
        String sql = "SELECT * FROM reviews ORDER BY useful DESC, id ASC";
        return findMany(sql);
    }

    public void addReactionToReview(long reviewId, long userId, boolean isPositive) {
        String sql = """
                INSERT INTO review_likes(review_id, user_id, is_like)
                VALUES (?,?,?)
                """;

        int rowsUpdated = jdbc.update(sql, reviewId, userId, isPositive);

        if (rowsUpdated == 0) {
            throw new InternalServerException("Не удалось сохранить данные.");
        }
    }

    public void removeReactionFromReview(long reviewId, long userId) {
        String sql = "DELETE FROM review_likes WHERE review_id=? AND user_id=?";
        update(sql, reviewId, userId);
    }

    public int calculateUseful(long reviewId) {
        String sql = """
                SELECT
                COUNT(CASE WHEN is_like = TRUE THEN 1 END) -
                COUNT(CASE WHEN is_like = FALSE THEN 1 END)
                FROM review_likes
                WHERE review_id = ?
                """;
        try {
            return jdbc.queryForObject(sql, Integer.class, reviewId);
        } catch (EmptyResultDataAccessException ignored) {
            return 0;
        }

    }

    public Boolean getReactionStatus(long reviewId, long userId) {
        String sql = """
                SELECT is_like FROM review_likes WHERE review_id=? AND user_id=?;
                """;
        try {
            return jdbc.queryForObject(sql, Boolean.class, reviewId, userId);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        }
    }

    private Optional<Review> findOne(String query, Object... args) {
        try {
            Review obj = jdbc.queryForObject(query, mapper, args);
            return Optional.ofNullable(obj);
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    private List<Review> findMany(String query, Object... args) {
        return jdbc.query(query, mapper, args);
    }

    private void update(String query, Object... args) {
        int rowsUpdated = jdbc.update(query, args);
        if (rowsUpdated == 0) {
            throw new InternalServerException("Не удалось обновить данные.");
        }
    }

    private long insert(String query, Object... args) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> getPreparedStatement(con, query, args), keyHolder);
        Long id = keyHolder.getKeyAs(Long.class);

        if (id == null) {
            throw new InternalServerException("Не удалось сохранить данные.");
        }
        return id;
    }

    private PreparedStatement getPreparedStatement(
            Connection connection, String query, Object... args) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

        for (int i = 0; i < args.length; i++) {
            statement.setObject(i + 1, args[i]);
        }
        return statement;
    }

    private Review mapRowToReview(ResultSet rs, int rowNum) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getLong("id"));
        review.setFilmId(rs.getLong("film_id"));
        review.setUserId(rs.getLong("user_id"));
        review.setIsPositive(rs.getBoolean("is_positive"));
        review.setUseful(rs.getInt("useful"));
        review.setContent(rs.getString("review"));
        review.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        review.setUpdatedAt(Optional.ofNullable(rs.getTimestamp("updated_at"))
                .map(Timestamp::toLocalDateTime)
                .orElse(null));
        return review;
    }
}
