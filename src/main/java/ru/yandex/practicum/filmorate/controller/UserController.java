package ru.yandex.practicum.filmorate.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/users")
public class UserController {
    private final Map<Long, User> users = new HashMap<>();

    private long getNextId() {
        return users.keySet()
                .stream()
                .mapToLong(id -> id)
                .max()
                .orElse(0) + 1;
    }

    @GetMapping
    public Collection<User> findAll() {
        return users.values();
    }

    private void validate(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Ошибка валидации: некорректный email {}", user.getEmail());
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать '@'");
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Ошибка валидации: некорректный логин {}", user.getLogin());
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Ошибка валидации: дата рождения в будущем {}", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    @PostMapping
    public User create(@RequestBody User user) throws DuplicatedDataException {
        validate(user);

        boolean emailExists = users.values()
                .stream()
                .anyMatch(u -> u.getEmail().equals(user.getEmail()));
        if (emailExists) {
            log.warn("Ошибка валидации: email {} уже используется", user.getEmail());
            throw new DuplicatedDataException("Этот имейл уже используется");
        }

        user.setId(getNextId());
        users.put(user.getId(), user);
        log.info("Добавлен пользователь: {}", user);
        return user;
    }

    @PutMapping
    public User update(@RequestBody User user) throws DuplicatedDataException {
        if (!users.containsKey(user.getId())) {
            log.warn("Ошибка обновления: пользователь с id {} не найден", user.getId());
            throw new RuntimeException("Пользователь с таким id не найден");
        }
        validate(user);

        boolean emailUsed = users.values()
                .stream()
                .anyMatch(u -> u.getEmail().equals(user.getEmail()) &&
                        !u.getId().equals(user.getId())); // исключаем самого себя
        if (emailUsed) {
            log.warn("Ошибка обновления: email {} уже используется", user.getEmail());
            throw new DuplicatedDataException("Этот имейл уже используется");
        }

        users.put(user.getId(), user);
        log.info("Обновлён пользователь: {}", user);
        return user;
    }


}
