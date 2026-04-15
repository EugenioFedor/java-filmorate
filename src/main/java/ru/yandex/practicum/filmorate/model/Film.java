package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class Film {
    private Long id;
    private String name;
    private String description;
    private LocalDate releaseDate;
    private int duration;
    private Set<Genre> genres = new LinkedHashSet<>();
    private Set<Director> directors = new LinkedHashSet<>();

    private MpaRating mpa;

    public void setGenres(Set<Genre> genres) {
        if (genres != null) {
            this.genres = genres.stream()
                    .sorted(Comparator.comparingLong(Genre::getId))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}