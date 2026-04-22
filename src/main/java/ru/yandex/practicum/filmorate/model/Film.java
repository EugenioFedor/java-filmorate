package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class Film {
    private Long id;
    @NotBlank(message = "Название фильма не может быть пустым.")
    private String name;
    @NotBlank(message = "Описание обязательно к заполнению.")
    @Size(max = 200, message = "Описание не должно превышать 200 символов.")
    private String description;
    private LocalDate releaseDate;
    @NotNull(message = "Длительность фильма должна быть указана.")
    @Positive(message = "Продолжительность фильма должна быть положительной.")
    private int duration;
    private Set<Genre> genres = new LinkedHashSet<>();
    private MpaRating mpa;
    @JsonAlias("director")
    private Set<Director> directors = new LinkedHashSet<>();
    @JsonProperty("rate")
    private double overallRate;

    public void setGenres(Set<Genre> genres) {
        if (genres != null) {
            this.genres = genres.stream()
                    .sorted(Comparator.comparingLong(Genre::getId))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public void setDirectors(Set<Director> directors) {
        if (directors != null) {
            this.directors = directors.stream()
                    .sorted(Comparator.comparingLong(Director::getId))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}