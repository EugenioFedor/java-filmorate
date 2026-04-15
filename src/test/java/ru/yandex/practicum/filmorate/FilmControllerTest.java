package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@SpringBootTest
@AutoConfigureTestDatabase
@Transactional
@ActiveProfiles("test")
public class FilmControllerTest {

    @Autowired
    private FilmController filmController;

    @Autowired
    private FilmService filmService;

    @Autowired
    private UserService userService;

    @Test
    void getCommonFilmsTestShouldReturnOneFilm() {
        MpaRating mpa = new MpaRating();
        mpa.setId(3L);
        Genre genre1 = new Genre();
        genre1.setId(1L);
        Genre genre2 = new Genre();
        genre2.setId(2L);

        Film film1 = new Film();
        film1.setName("film1");
        film1.setDescription("Sci-fi film");
        film1.setMpa(mpa);
        film1.setReleaseDate(LocalDate.of(1997, 1, 15));
        film1.setDuration(136);

        Film film2 = new Film();
        film2.setName("film2");
        film2.setDescription("Sci-fi film");
        film2.setMpa(mpa);
        film2.setGenres(Set.of(genre1, genre2));
        film2.setReleaseDate(LocalDate.of(1998, 2, 15));
        film2.setDuration(136);

        Film film3 = new Film();
        film3.setName("film3");
        film3.setMpa(mpa);
        film3.setDescription("Sci-fi film");
        film3.setReleaseDate(LocalDate.of(1999, 3, 15));
        film3.setDuration(136);

        Film storedFilm1 = filmService.create(film1);
        Film storedFilm2 = filmService.create(film2);
        Film storedFilm3 = filmService.create(film3);
        System.out.println("storedFilm2: " + storedFilm2);
        User user1 = new User();
        user1.setLogin("user1-login");
        user1.setEmail("user1@mail.com");
        user1.setName("user1-name");
        user1.setBirthday(LocalDate.parse("1990-01-01"));

        User user2 = new User();
        user2.setLogin("user2-login");
        user2.setEmail("user2@mail.com");
        user2.setName("user2-name");
        user2.setBirthday(LocalDate.parse("1995-10-10"));

        User storedUser1 = userService.create(user1);
        User storedUser2 = userService.create(user2);

        filmService.addLike(storedFilm2.getId(), storedUser1.getId());
        filmService.addLike(storedFilm1.getId(), storedUser2.getId());
        filmService.addLike(storedFilm2.getId(), storedUser2.getId());
        filmService.addLike(storedFilm3.getId(), storedUser2.getId());

        List<Film> films = filmController.getCommonFilms(storedUser1.getId(), storedUser2.getId());

        assertEquals(1, films.size());
        assertTrue(films.contains(storedFilm2));
    }

    @Test
    void getCommonFilmsTestShouldReturnEmptyList() {
        MpaRating mpa = new MpaRating();
        mpa.setId(3L);
        Genre genre1 = new Genre();
        genre1.setId(1L);
        Genre genre2 = new Genre();
        genre2.setId(2L);

        Film film1 = new Film();
        film1.setName("film1");
        film1.setDescription("Sci-fi film");
        film1.setMpa(mpa);
        film1.setReleaseDate(LocalDate.of(1997, 1, 15));
        film1.setDuration(136);

        Film film2 = new Film();
        film2.setName("film2");
        film2.setDescription("Sci-fi film");
        film2.setMpa(mpa);
        film2.setGenres(Set.of(genre1, genre2));
        film2.setReleaseDate(LocalDate.of(1998, 2, 15));
        film2.setDuration(136);

        Film film3 = new Film();
        film3.setName("film3");
        film3.setMpa(mpa);
        film3.setDescription("Sci-fi film");
        film3.setReleaseDate(LocalDate.of(1999, 3, 15));
        film3.setDuration(136);

        Film storedFilm1 = filmService.create(film1);
        Film storedFilm2 = filmService.create(film2);
        Film storedFilm3 = filmService.create(film3);
        System.out.println("storedFilm2: " + storedFilm2);
        User user1 = new User();
        user1.setLogin("user1-login");
        user1.setEmail("user1@mail.com");
        user1.setName("user1-name");
        user1.setBirthday(LocalDate.parse("1990-01-01"));

        User user2 = new User();
        user2.setLogin("user2-login");
        user2.setEmail("user2@mail.com");
        user2.setName("user2-name");
        user2.setBirthday(LocalDate.parse("1995-10-10"));

        User storedUser1 = userService.create(user1);
        User storedUser2 = userService.create(user2);

        filmService.addLike(storedFilm2.getId(), storedUser1.getId());
        filmService.addLike(storedFilm1.getId(), storedUser2.getId());
        filmService.addLike(storedFilm2.getId(), storedUser2.getId());
        filmService.addLike(storedFilm3.getId(), storedUser2.getId());
        filmService.removeLike(storedFilm2.getId(), storedUser2.getId());

        List<Film> films = filmController.getCommonFilms(storedUser1.getId(), storedUser2.getId());
        assertTrue(films.isEmpty());
    }
}
