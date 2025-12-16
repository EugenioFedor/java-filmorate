package ru.yandex.practicum.filmorate.controller;

import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;

public class UserController {

    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1L;

    public Collection<User> findAll() {
        return users.values();
    }

    public User findById(Long id) {
        User user = users.get(id);
        if (user == null) throw new NoSuchElementException("Пользователь не найден");
        return user;
    }

    public User create(User user) {
        validateUser(user);

        if (users.values().stream().anyMatch(u -> u.getEmail().equals(user.getEmail()))) {
            throw new DuplicatedDataException("Email уже существует");
        }

        user.setId(nextId++);
        users.put(user.getId(), user);
        return user;
    }

    public User update(User user) {
        validateUser(user);
        if (!users.containsKey(user.getId())) {
            throw new NoSuchElementException("Пользователь не найден");
        }
        users.put(user.getId(), user);
        return user;
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            throw new ValidationException("Некорректный email");
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            throw new ValidationException("Некорректный login");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения в будущем");
        }
    }
}
