package ru.yandex.practicum.filmorate.storage.mapper;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;

@Component
public class FilmRowMapper implements RowMapper<Film> {

    @Override
    public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Film(){{
            setId(rs.getLong("id"));
            setName(rs.getString("name"));
            setDescription(rs.getString("description"));
            setReleaseDate(rs.getDate("release_date").toLocalDate());
            setDuration(rs.getInt("duration"));
            setMpa(new MpaRating(rs.getLong("mpa_id"),rs.getString("mpa_name")));
            setGenres(new HashSet<>());
            setOverallRate(rs.getDouble("overall_rate"));
        }};
    }
}
