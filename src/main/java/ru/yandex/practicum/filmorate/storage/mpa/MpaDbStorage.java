package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MpaDbStorage implements MpaStorage {

    private final JdbcTemplate jdbcTemplate;

    public List<MpaRating> getAll() {
        return jdbcTemplate.query("SELECT * FROM mpa_ratings ORDER BY id", (rs, rowNum) -> {
            MpaRating mpa = new MpaRating();
            mpa.setId(rs.getLong("id"));
            mpa.setName(rs.getString("name"));
            return mpa;
        });
    }

    public Optional<MpaRating> getById(Long id) {
        List<MpaRating> list = jdbcTemplate.query("SELECT * FROM mpa_ratings WHERE id = ?", (rs, rowNum) -> {
            MpaRating mpa = new MpaRating();
            mpa.setId(rs.getLong("id"));
            mpa.setName(rs.getString("name"));
            return mpa;
        }, id);
        return list.stream().findFirst();
    }
}