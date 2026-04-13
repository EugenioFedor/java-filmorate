package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    private long reviewId;
    private long filmId;
    private long userId;
    private boolean isPositive;
    private String content;
    private int useful;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
