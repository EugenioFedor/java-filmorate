package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final FilmService filmService;
    private final UserService userService;
    private final ReviewDbStorage reviewStorage;

    public Review addFilmReview(Review review) {
        userService.getById(review.getUserId());
        filmService.getById(review.getFilmId());

        review.setCreatedAt(LocalDateTime.now());
        Review processedReview = reviewStorage.addReview(review);
        log.info("Добавлен новый отзыв: {}", processedReview);

        return processedReview;
    }

    public Review updateFilmReview(Review review) {
        userService.getById(review.getUserId());
        filmService.getById(review.getFilmId());

        review.setUpdatedAt(LocalDateTime.now());
        Review processedReview = reviewStorage.updateReview(review);
        log.info("Отзыв обновлен: {}", processedReview);

        return processedReview;
    }

    public void deleteFilmReview(long reviewId) {
        Review review = findReviewOrThrow(reviewId);

        reviewStorage.deleteReviewById(reviewId);
        log.info("Отзыв удален: {}", review);
    }

    public Review getFilmReview(long reviewId) {
        return findReviewOrThrow(reviewId);
    }

    public List<Review> getFilmReviews(Long filmId, int limit) {
        if (filmId == null) {
            return reviewStorage.getReviews();
        }

        filmService.getById(filmId);
        return reviewStorage.getReviewsByFilmId(filmId, limit);
    }

    public void addLikeToFilmReview(long reviewId, long userId) {
        Review review = findReviewOrThrow(reviewId);
        userService.getById(userId);

        reviewStorage.addReactionToReview(reviewId, userId, true);
        int usefulPoints = reviewStorage.calculateUseful(reviewId);
        review.setUseful(usefulPoints);
        log.info("Поставлен лайк на отзыв: {}", review);
    }

    public void addDislikeToFilmReview(long reviewId, long userId) {
        Review review = findReviewOrThrow(reviewId);
        userService.getById(userId);

        reviewStorage.addReactionToReview(reviewId, userId, false);
        int usefulPoints = reviewStorage.calculateUseful(reviewId);
        review.setUseful(usefulPoints);
        log.info("Поставлен дизлайк на отзыв: {}", review);
    }

    public void deleteLikeFromFilmReview(long reviewId, long userId) {
        Review review = findReviewOrThrow(reviewId);
        Boolean currentStatus = reviewStorage.getReactionStatus(reviewId, userId);
        checkIfStatusExists(currentStatus, reviewId, userId);

        if (currentStatus == false) {
            log.error("Ошибка - попытка удалить дизлайк вместо лайка.");
            throw new ValidationException("Попытка удалить дизлайк.");
        }

        reviewStorage.removeReactionFromReview(reviewId, userId);
        int usefulPoints = reviewStorage.calculateUseful(reviewId);
        review.setUseful(usefulPoints);
        log.info("Удален лайк на отзыв: {}", review);
    }

    public void deleteDislikeFromFilmReview(long reviewId, long userId) {
        Review review = findReviewOrThrow(reviewId);
        Boolean currentStatus = reviewStorage.getReactionStatus(reviewId, userId);
        checkIfStatusExists(currentStatus, reviewId, userId);

        if (currentStatus == true) {
            log.error("Ошибка - попытка удалить лайк вместо дизлайка.");
            throw new ValidationException("Попытка удалить лайк вместо дизлайка.");
        }

        reviewStorage.removeReactionFromReview(reviewId, userId);
        int usefulPoints = reviewStorage.calculateUseful(reviewId);
        review.setUseful(usefulPoints);
        log.info("Удален дизлайк на отзыв: {}", review);
    }

    private void checkIfStatusExists(Boolean status, long reviewId, long userId) {
        if (status == null) {
            log.error("Ошибка - не найдена реакция: reviewId={}, userId={}", reviewId, userId);
            throw new NotFoundException("Реакция не найдена: reviewId=" + reviewId + ", userId=" + userId);
        }
    }

    private Review findReviewOrThrow(Long id) {
        return reviewStorage.getReviewById(id).orElseThrow(() -> {
            log.error("Ошибка - не найден отзыв с id: {}", id);
            return new NotFoundException("Отзыв с id = " + id + " не найден.");
        });
    }
}
