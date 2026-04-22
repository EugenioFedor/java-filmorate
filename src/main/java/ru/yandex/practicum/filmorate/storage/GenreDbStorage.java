package ru.yandex.practicum.filmorate.storage;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class GenreDbStorage extends BaseDbStorage<Genre> {

    public GenreDbStorage(NamedParameterJdbcTemplate jdbcTemplate, GenreRowMapper mapper) {
        super(jdbcTemplate, mapper);
    }

    public List<Genre> getGenres() {
        String sql = "SELECT * FROM genres ORDER BY id";
        return findMany(sql, Map.of());
    }

    public Optional<Genre> getGenreById(Long id) {
        String sql = """
                SELECT * FROM genres
                WHERE id=:genreId
                """;
        return findOne(sql, Map.of("genreId", id));
    }
}