package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.InternalServerException;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@RequiredArgsConstructor
public class BaseDbStorage<T> {
    protected final NamedParameterJdbcTemplate jdbc;
    protected final RowMapper<T> mapper;

    protected Optional<T> findOne(String query, Map<String, ?> params) {
        try {
            List<T> objects = jdbc.query(query, params, mapper);
            return objects.stream().findFirst();
        } catch (EmptyResultDataAccessException ignored) {
            return Optional.empty();
        }
    }

    protected List<T> findMany(String query, Map<String, ?> params) {
        return jdbc.query(query, params, mapper);
    }

    protected int update(String query, Map<String, ?> params) {
        return jdbc.update(query, params);
    }

    protected long insert(String query, Map<String, ?> params) {
        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(query,new MapSqlParameterSource(params), keyHolder,new String[]{"id"});
        Long id = keyHolder.getKeyAs(Long.class);

        if (id == null) {
            throw new InternalServerException("Не удалось сохранить данные");
        }
        return id;
    }

    public void updateFeed(Long userId, Long entityId, String eventType, String operation) {
        String insertFeedSql = """
                INSERT INTO feed (user_id, entity_id, event_type, operation, timestamp)
                VALUES (:userId, :entityId, :eventType, :operation, :timestamp)
                """;

        Map<String, ?> feedParams = Map.of(
                "userId",userId,
                "timestamp",System.currentTimeMillis(),
                "eventType",eventType,
                "operation",operation,
                "entityId",entityId
        );

        jdbc.update(insertFeedSql, feedParams);
    }
}
