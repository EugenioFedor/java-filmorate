package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private Long reviewId;
    @NotNull(message = "ID фильма не может быть пустым")
    private Long filmId;
    @NotNull(message = "ID пользователя не может быть пустым")
    private Long userId;
    @NotNull
    private Boolean isPositive;
    @NotBlank(message = "Текст отзыва не может быть пустым")
    private String content;
    private int useful;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
