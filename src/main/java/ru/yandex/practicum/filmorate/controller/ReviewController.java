package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public Review addFilmReview(@RequestBody Review review) {
        return reviewService.addFilmReview(review);
    }

    @PutMapping
    public Review updateFilmReview(@RequestBody Review review) {
        return reviewService.updateFilmReview(review);
    }

    @DeleteMapping("/{id}")
    public void deleteFilmReview(@PathVariable long id) {
        reviewService.deleteFilmReview(id);
    }

    @GetMapping("/{id}")
    public Review getFilmReview(@PathVariable long id) {
        return reviewService.getFilmReview(id);
    }

    @GetMapping
    public List<Review> getFilmReviews(
            @RequestParam(required = false) Long filmId,
            @RequestParam(defaultValue = "10") int count) {
        return reviewService.getFilmReviews(filmId, count);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLikeToFilmReview(@PathVariable long id, @PathVariable long userId) {
        reviewService.addLikeToFilmReview(id, userId);
    }

    @PutMapping("/{id}/dislike/{userId}")
    public void addDislikeToFilmReview(@PathVariable long id, @PathVariable long userId) {
        reviewService.addDislikeToFilmReview(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void deleteLikeFromFilmReview(@PathVariable long id, @PathVariable long userId) {
        reviewService.deleteLikeFromFilmReview(id, userId);
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public void deleteDislikeFromFilmReview(@PathVariable long id, @PathVariable long userId) {
        reviewService.deleteDislikeFromFilmReview(id, userId);
    }
}
