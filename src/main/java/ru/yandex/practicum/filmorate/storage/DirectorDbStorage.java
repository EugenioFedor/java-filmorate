package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.InternalServerException;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DirectorDbStorage {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<Director> mapper = this::mapRowToDirector;

    public List<Director> getAll() {
        String sql = "SELECT * FROM directors ORDER BY id";
        return jdbcTemplate.query(sql, mapper);
    }

    public Optional<Director> getById(Long id) {
        String sql = "SELECT * FROM directors WHERE id = :id";
        try {
            Director director = jdbcTemplate.queryForObject(sql, Map.of("id", id), mapper);
            return Optional.ofNullable(director);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Director create(Director director) {
        String sql = "INSERT INTO directors (name) VALUES (:name)";

        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource(Map.of("name", director.getName())),
                keyHolder, new String[]{"id"}
        );
        Long id = keyHolder.getKeyAs(Long.class);

        if (id == null) {
            throw new InternalServerException("Не удалось сохранить данные");
        }

        director.setId(id);

        return director;
    }

    public Director update(Director director) {
        String sql = "UPDATE directors SET name =:name WHERE id =:id";

        Map<String, ?> params = Map.of("name", director.getName(), "id", director.getId());

        int rowsUpdated = jdbcTemplate.update(sql, params);

        if (rowsUpdated == 0) {
            throw new NotFoundException("Режиссёр с id " + director.getId() + " не найден");
        }

        return director;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM directors WHERE id =:id";

        int rowsDeleted = jdbcTemplate.update(sql, Map.of("id", id));

        if (rowsDeleted == 0) {
            throw new NotFoundException("Режиссёр с id " + id + " не найден");
        }
    }

    private Director mapRowToDirector(ResultSet rs, int rowNum) throws SQLException {
        Director director = new Director();
        director.setId(rs.getLong("id"));
        director.setName(rs.getString("name"));
        return director;
    }
}
