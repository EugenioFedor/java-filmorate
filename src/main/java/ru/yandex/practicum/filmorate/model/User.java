package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Data
public class User {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthday;

    private Long id;
    private String email;
    private String login;
    private String name;

    private Set<Long> friends = new HashSet<>();
}