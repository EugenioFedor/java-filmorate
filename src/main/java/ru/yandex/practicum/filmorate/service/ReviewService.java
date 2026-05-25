package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.ReviewDbStorage;

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
        filmService.getFilmById(review.getFilmId());

        review.setCreatedAt(LocalDateTime.now().withNano(0));
        review.setUseful(0);
        Review processedReview = reviewStorage.addReview(review);
        log.info("Добавлен новый отзыв: {}", processedReview);

        return processedReview;
    }

    public Review updateFilmReview(Review review) {
        Review storedReview = findReviewOrThrow(review.getReviewId());
        review.setUpdatedAt(LocalDateTime.now().withNano(0));
        review.setUseful(storedReview.getUseful());
        review.setCreatedAt(storedReview.getCreatedAt());
        review.setFilmId(storedReview.getFilmId());
        review.setUserId(storedReview.getUserId());
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
            return reviewStorage.getReviews(limit);
        }

        return reviewStorage.getReviewsByFilmId(filmId, limit);
    }

    public void addLikeToFilmReview(long reviewId, long userId) {
        Review storedReview = findReviewOrThrow(reviewId);
        userService.getById(userId);

        Boolean currentStatus = reviewStorage.getReactionStatus(reviewId, userId);

        if (currentStatus != null && !currentStatus) {
            reviewStorage.removeReactionFromReview(reviewId, userId);
        }

        reviewStorage.addReactionToReview(reviewId, userId, true);
        log.info("Поставлен лайк на отзыв: {}", storedReview);
    }

    public void addDislikeToFilmReview(long reviewId, long userId) {
        Review review = findReviewOrThrow(reviewId);
        userService.getById(userId);

        Boolean currentStatus = reviewStorage.getReactionStatus(reviewId, userId);

        if (currentStatus != null && currentStatus) {
            reviewStorage.removeReactionFromReview(reviewId, userId);
        }

        reviewStorage.addReactionToReview(reviewId, userId, false);
        log.info("Поставлен дизлайк на отзыв: {}", review);
    }

    public void deleteLikeFromFilmReview(long reviewId, long userId) {
        Review review = findReviewOrThrow(reviewId);
        Boolean currentStatus = reviewStorage.getReactionStatus(reviewId, userId);
        checkIfStatusExists(currentStatus, reviewId, userId);

        if (!currentStatus) {
            throw new ValidationException("Попытка удалить дизлайк вместо лайка.");
        }

        reviewStorage.removeReactionFromReview(reviewId, userId);
        log.info("Удален лайк на отзыв: {}", review);
    }

    public void deleteDislikeFromFilmReview(long reviewId, long userId) {
        Review review = findReviewOrThrow(reviewId);
        Boolean currentStatus = reviewStorage.getReactionStatus(reviewId, userId);
        checkIfStatusExists(currentStatus, reviewId, userId);

        if (currentStatus) {
            throw new ValidationException("Попытка удалить лайк вместо дизлайка.");
        }

        reviewStorage.removeReactionFromReview(reviewId, userId);
        log.info("Удален дизлайк на отзыв: {}", review);
    }

    private void checkIfStatusExists(Boolean status, long reviewId, long userId) {
        if (status == null) {
            throw new NotFoundException("Реакция: reviewId=" + reviewId + ", userId=" + userId);
        }
    }

    private Review findReviewOrThrow(Long id) {
        return reviewStorage.getReviewById(id).orElseThrow(() ->
                new NotFoundException("Отзыв с id = " + id)
        );
    }
}
