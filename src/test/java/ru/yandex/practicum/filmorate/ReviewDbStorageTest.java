package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.*;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import({ReviewDbStorage.class, FilmDbStorage.class, UserDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class ReviewDbStorageTest {

    private final ReviewDbStorage reviewDbStorage;
    private final FilmDbStorage filmDbStorage;
    private final UserDbStorage userDbStorage;

    private Film film;
    private User user;


    @BeforeEach
    void setup() {
        Genre drama = new Genre(2L, "Драма");
        MpaRating rating = new MpaRating();
        rating.setId(4L);
        rating.setName("R");

        film = new Film();
        film.setName("Побег из Шоушенка");
        film.setDescription("История одного заключения");
        film.setReleaseDate(LocalDate.of(1994, 9, 10));
        film.setDuration(142);
        film.setMpa(rating);
        film.setGenres(Set.of(drama));

        user = new User();
        user.setName("user");
        user.setLogin("user-login");
        user.setEmail("user@mail.ru");
        user.setBirthday(LocalDate.of(2000, 1, 1));
    }

    @Test
    void addReviewTest() {
        Film savedFilm = filmDbStorage.create(film);
        User savedUser = userDbStorage.create(user);
        Review review = Review.builder()
                .content("This film is sooo good.")
                .isPositive(true)
                .userId(savedUser.getId())
                .filmId(savedFilm.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewDbStorage.addReview(review);
        Optional<Review> reviewOptional = reviewDbStorage.getReviewById(savedReview.getReviewId());

        assertThat(reviewOptional)
                .isPresent()
                .hasValueSatisfying( r -> {
                    assertThat(r).hasFieldOrPropertyWithValue("content", "This film is sooo good.");
                    assertThat(r.isPositive()).isTrue();
                    assertThat(r.getUpdatedAt()).isNull();
                    assertThat(r).hasFieldOrPropertyWithValue("userId", savedReview.getUserId());
                });
    }

    @Test
    void updateReviewTest() {
        Film savedFilm = filmDbStorage.create(film);
        User savedUser = userDbStorage.create(user);
        Review review = Review.builder()
                .content("This film is sooo good.")
                .isPositive(true)
                .userId(savedUser.getId())
                .filmId(savedFilm.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewDbStorage.addReview(review);
        Review update = Review.builder()
                .reviewId(savedReview.getReviewId())
                .content("very bad film")
                .isPositive(false)
                .userId(savedUser.getId())
                .filmId(savedFilm.getId())
                .createdAt(savedReview.getCreatedAt())
                .updatedAt(LocalDateTime.now())
                .build();

        Review updatedReview = reviewDbStorage.updateReview(update);
        Optional<Review> reviewOptional = reviewDbStorage.getReviewById(updatedReview.getReviewId());

        assertThat(reviewOptional)
                .isPresent()
                .hasValueSatisfying( r -> {
                    assertThat(r.getContent()).isEqualTo("very bad film");
                    assertThat(r.isPositive()).isFalse();
                    assertThat(r.getUpdatedAt()).isNotNull();
                    assertThat(r.getReviewId()).isEqualTo(savedReview.getReviewId());
                });
    }

    @Test
    void deleteReviewByIdTest() {
        Film savedFilm = filmDbStorage.create(film);
        User savedUser = userDbStorage.create(user);
        Review review = Review.builder()
                .content("This film is sooo good.")
                .isPositive(true)
                .userId(savedUser.getId())
                .filmId(savedFilm.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewDbStorage.addReview(review);
        reviewDbStorage.deleteReviewById(savedReview.getReviewId());
        Optional<Review> reviewOptional = reviewDbStorage.getReviewById(savedReview.getReviewId());
        assertThat(reviewOptional).isNotPresent();
    }

    @Test
    void getReviewByIdTest() {
        Optional<Review> unfoundedReview = reviewDbStorage.getReviewById(999L);
        assertThat(unfoundedReview).isNotPresent();
    }


    @Test
    void getReviewsByFilmIdTest() {
        User user2 = new User();
        user2.setName("user2");
        user2.setLogin("user2-login");
        user2.setEmail("user2@gmail.com");
        user2.setBirthday(LocalDate.of(1980, 5, 15));

        User user3 = new User();
        user3.setName("user3");
        user3.setLogin("user3-login");
        user3.setEmail("user3@gmail.com");
        user3.setBirthday(LocalDate.of(1970, 7, 12));

        MpaRating rating = new MpaRating();
        rating.setId(3L);
        rating.setName("PG-13");

        Film film2 = new Film();
        film2.setName("film2-name");
        film2.setDescription("film2-description");
        film2.setReleaseDate(LocalDate.of(2000, 1, 1));
        film2.setDuration(150);
        film2.setMpa(rating);
        film2.setGenres(Set.of(new Genre(6L,"Боевик")));

        User savedUser1 = userDbStorage.create(user);
        User savedUser2 = userDbStorage.create(user2);
        User savedUser3 = userDbStorage.create(user3);
        Film savedFilm1 = filmDbStorage.create(film);
        Film savedFilm2 = filmDbStorage.create(film2);


        Review review1Film1 = Review.builder()
                .content("This film is sooo good.")
                .isPositive(true)
                .userId(savedUser1.getId())
                .filmId(savedFilm1.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Review review2Film1 = Review.builder()
                .content("Outstanding film.")
                .isPositive(true)
                .userId(savedUser2.getId())
                .filmId(savedFilm1.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Review review3Film1 = Review.builder()
                .content("I didn't like this film.")
                .isPositive(false)
                .userId(savedUser3.getId())
                .filmId(savedFilm1.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Review review4Film2 = Review.builder()
                .content("Magnificent.")
                .isPositive(true)
                .userId(savedUser1.getId())
                .filmId(savedFilm2.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview1 = reviewDbStorage.addReview(review1Film1);
        Review savedReview2 = reviewDbStorage.addReview(review2Film1);
        Review savedReview3 = reviewDbStorage.addReview(review3Film1);
        Review savedReview4 = reviewDbStorage.addReview(review4Film2);

        List<Review> reviews = reviewDbStorage.getReviewsByFilmId(savedFilm1.getId(),2);
        assertThat(reviews).hasSize(2);
        assertThat(reviews).contains(savedReview1, savedReview2);
        assertThat(reviews).doesNotContain(savedReview3, savedReview4);
    }

    @Test
    void getReviewsTest() {
        User user2 = new User();
        user2.setName("user2");
        user2.setLogin("user2-login");
        user2.setEmail("user2@gmail.com");
        user2.setBirthday(LocalDate.of(1980, 5, 15));

        User user3 = new User();
        user3.setName("user3");
        user3.setLogin("user3-login");
        user3.setEmail("user3@gmail.com");
        user3.setBirthday(LocalDate.of(1970, 7, 12));

        MpaRating rating = new MpaRating();
        rating.setId(3L);
        rating.setName("PG-13");

        Film film2 = new Film();
        film2.setName("film2-name");
        film2.setDescription("film2-description");
        film2.setReleaseDate(LocalDate.of(2000, 1, 1));
        film2.setDuration(150);
        film2.setMpa(rating);
        film2.setGenres(Set.of(new Genre(6L,"Боевик")));

        User savedUser1 = userDbStorage.create(user);
        User savedUser2 = userDbStorage.create(user2);
        User savedUser3 = userDbStorage.create(user3);
        Film savedFilm1 = filmDbStorage.create(film);
        Film savedFilm2 = filmDbStorage.create(film2);


        Review review1Film1 = Review.builder()
                .content("This film is sooo good.")
                .isPositive(true)
                .userId(savedUser1.getId())
                .filmId(savedFilm1.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Review review2Film1 = Review.builder()
                .content("Outstanding film.")
                .isPositive(true)
                .userId(savedUser2.getId())
                .filmId(savedFilm1.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Review review3Film1 = Review.builder()
                .content("I didn't like this film.")
                .isPositive(false)
                .userId(savedUser3.getId())
                .filmId(savedFilm1.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Review review4Film2 = Review.builder()
                .content("Magnificent.")
                .isPositive(true)
                .userId(savedUser1.getId())
                .filmId(savedFilm2.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview1 = reviewDbStorage.addReview(review1Film1);
        Review savedReview2 = reviewDbStorage.addReview(review2Film1);
        Review savedReview3 = reviewDbStorage.addReview(review3Film1);
        Review savedReview4 = reviewDbStorage.addReview(review4Film2);

        List<Review> reviews = reviewDbStorage.getReviews();

        assertThat(reviews).hasSize(4);
        assertThat(reviews).contains(savedReview1, savedReview2, savedReview3, savedReview4);
    }

    @Test
    void addReactionToReviewTest() {
        Film savedFilm = filmDbStorage.create(film);
        User savedUser = userDbStorage.create(user);
        Review review = Review.builder()
                .content("This film is sooo good.")
                .isPositive(true)
                .userId(savedUser.getId())
                .filmId(savedFilm.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewDbStorage.addReview(review);

        User user2 = new User();
        user2.setName("user2");
        user2.setLogin("user2-login");
        user2.setEmail("user2@gmail.com");
        user2.setBirthday(LocalDate.of(1980, 5, 15));

        User user3 = new User();
        user3.setName("user3");
        user3.setLogin("user3-login");
        user3.setEmail("user3@gmail.com");
        user3.setBirthday(LocalDate.of(1970, 7, 12));

        User user4 = new User();
        user4.setName("user4");
        user4.setLogin("user4-login");
        user4.setEmail("user4@gmail.com");
        user4.setBirthday(LocalDate.of(1960, 8, 22));

        User savedUser2 = userDbStorage.create(user2);
        User savedUser3 = userDbStorage.create(user3);
        User savedUser4 = userDbStorage.create(user4);

        reviewDbStorage.addReactionToReview(savedReview.getReviewId(),savedUser2.getId(),true);
        reviewDbStorage.addReactionToReview(savedReview.getReviewId(),savedUser3.getId(),true);
        reviewDbStorage.addReactionToReview(savedReview.getReviewId(),savedUser4.getId(),false);

        Boolean isLikeUser2 = reviewDbStorage.getReactionStatus(savedReview.getReviewId(),savedUser2.getId());
        Boolean isLikeUser3 = reviewDbStorage.getReactionStatus(savedReview.getReviewId(),savedUser3.getId());
        Boolean isLikeUser4 = reviewDbStorage.getReactionStatus(savedReview.getReviewId(),savedUser4.getId());

        assertThat(isLikeUser2).isTrue();
        assertThat(isLikeUser3).isTrue();
        assertThat(isLikeUser4).isFalse();

        int usefulPoints = reviewDbStorage.calculateUseful(savedReview.getReviewId());
        assertThat(usefulPoints).isEqualTo(1);
    }

    @Test
    void removeReactionFromReviewTest() {
        Film savedFilm = filmDbStorage.create(film);
        User savedUser = userDbStorage.create(user);
        Review review = Review.builder()
                .content("This film is sooo good.")
                .isPositive(true)
                .userId(savedUser.getId())
                .filmId(savedFilm.getId())
                .createdAt(LocalDateTime.now())
                .build();

        Review savedReview = reviewDbStorage.addReview(review);

        User user2 = new User();
        user2.setName("user2");
        user2.setLogin("user2-login");
        user2.setEmail("user2@gmail.com");
        user2.setBirthday(LocalDate.of(1980, 5, 15));

        User user3 = new User();
        user3.setName("user3");
        user3.setLogin("user3-login");
        user3.setEmail("user3@gmail.com");
        user3.setBirthday(LocalDate.of(1970, 7, 12));

        User user4 = new User();
        user4.setName("user4");
        user4.setLogin("user4-login");
        user4.setEmail("user4@gmail.com");
        user4.setBirthday(LocalDate.of(1960, 8, 22));

        User savedUser2 = userDbStorage.create(user2);
        User savedUser3 = userDbStorage.create(user3);
        User savedUser4 = userDbStorage.create(user4);

        reviewDbStorage.addReactionToReview(savedReview.getReviewId(),savedUser2.getId(),true);
        reviewDbStorage.addReactionToReview(savedReview.getReviewId(),savedUser3.getId(),true);
        reviewDbStorage.addReactionToReview(savedReview.getReviewId(),savedUser4.getId(),false);

        reviewDbStorage.removeReactionFromReview(savedReview.getReviewId(),savedUser4.getId());

        Boolean isLikeUser2 = reviewDbStorage.getReactionStatus(savedReview.getReviewId(),savedUser2.getId());
        Boolean isLikeUser3 = reviewDbStorage.getReactionStatus(savedReview.getReviewId(),savedUser3.getId());
        Boolean isLikeUser4 = reviewDbStorage.getReactionStatus(savedReview.getReviewId(),savedUser4.getId());

        assertThat(isLikeUser2).isTrue();
        assertThat(isLikeUser3).isTrue();
        assertThat(isLikeUser4).isNull();

        int usefulPoints = reviewDbStorage.calculateUseful(savedReview.getReviewId());
        assertThat(usefulPoints).isEqualTo(2);
    }
}
