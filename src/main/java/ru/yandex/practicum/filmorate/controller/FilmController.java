package ru.yandex.practicum.filmorate.controller;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/films")
public class FilmController {

    private final FilmService service;

    public FilmController(FilmService service) {
        this.service = service;
    }

    @PostMapping
    public Film create(@RequestBody Film film) {
        return service.create(film);
    }

    @PutMapping
    public Film update(@RequestBody Film film) {
        return service.update(film);
    }

    @GetMapping
    public Collection<Film> getAll() {
        return service.getAll();
    }
}
