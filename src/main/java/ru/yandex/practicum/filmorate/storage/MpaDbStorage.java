package ru.yandex.practicum.filmorate.storage;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.mapper.MpaRowMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class MpaDbStorage extends BaseDbStorage<MpaRating> {

    public MpaDbStorage(NamedParameterJdbcTemplate jdbcTemplate, MpaRowMapper rowMapper) {
        super(jdbcTemplate, rowMapper);
    }

    public List<MpaRating> getAll() {
        String sql = "SELECT * FROM mpa_ratings ORDER BY id";
        return findMany(sql, Map.of());
    }

    public Optional<MpaRating> getById(Long id) {
        String sql = "SELECT * FROM mpa_ratings WHERE id = :id";
        return findOne(sql, Map.of("id", id));
    }
}