package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(FilmDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;

    @Test
    void testFindFilmById() {

        Film film = new Film();
        film.setName("Matrix");
        film.setDescription("Sci-fi film");
        film.setReleaseDate(LocalDate.of(1999, 3, 31));
        film.setDuration(136);

        MpaRating mpa = new MpaRating();
        mpa.setId(1L);
        mpa.setName("G");

        film.setMpa(mpa);

        Film savedFilm = filmStorage.create(film);

        Optional<Film> filmOptional = filmStorage.getById(savedFilm.getId());

        assertThat(filmOptional)
                .isPresent()
                .hasValueSatisfying(f ->
                        assertThat(f).hasFieldOrPropertyWithValue("name", "Matrix")
                );
    }
}