package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1L;

    public Collection<User> findAll() {
        return users.values();
    }

    public User findById(Long id) {
        validateId(id);
        User user = users.get(id);
        if (user == null) throw new NoSuchElementException("Пользователь с id=" + id + " не найден");
        return user;
    }

    public User create(User user) {
        validateUser(user);
        user.setId(nextId++);
        users.put(user.getId(), user);
        return user;
    }

    public User update(User user) {
        validateUser(user);
        validateId(user.getId());

        if (!users.containsKey(user.getId())) {
            throw new NoSuchElementException("Пользователь не найден");
        }

        users.put(user.getId(), user);
        return user;
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new IllegalArgumentException("Некорректный email");
        }
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new IllegalArgumentException("Логин не может быть пустым");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        if (user.getBirthday().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Дата рождения не может быть в будущем");
        }
    }

    private void validateId(Long id) {
        if (id <= 0) throw new IllegalArgumentException("Id должен быть положительным");
    }
}
