package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserStorage userStorage;

    private final Map<Long, Set<Long>> friendsByUser = new HashMap<>();

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public UserService() {
        this.userStorage = new InMemoryUserStorage();
    }

    public User create(User user) {
        validate(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        return userStorage.create(user);
    }

    public User update(User user) {
        validate(user);

        getById(user.getId());

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        return userStorage.update(user);
    }

    public Collection<User> getAll() {
        return userStorage.getAll();
    }

    public User getById(Long id) {
        return userStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id " + id + " не найден"));
    }

    public void addFriend(Long id, Long friendId) {
        getById(id);
        getById(friendId);

        if (Objects.equals(id, friendId)) {
            throw new ValidationException("Нельзя добавить себя в друзья");
        }

        friendsByUser.computeIfAbsent(id, k -> new HashSet<>()).add(friendId);
        friendsByUser.computeIfAbsent(friendId, k -> new HashSet<>()).add(id);
    }

    public void removeFriend(Long id, Long friendId) {
        getById(id);
        getById(friendId);

        Set<Long> userFriends = friendsByUser.get(id);
        Set<Long> friendFriends = friendsByUser.get(friendId);

        if (userFriends == null || !userFriends.remove(friendId)) {
            throw new NotFoundException("Пользователь " + friendId + " не является другом пользователя " + id);
        }

        if (friendFriends != null) {
            friendFriends.remove(id);
            if (friendFriends.isEmpty()) {
                friendsByUser.remove(friendId);
            }
        }

        if (userFriends.isEmpty()) {
            friendsByUser.remove(id);
        }
    }

    public List<User> getFriends(Long id) {
        getById(id);

        Set<Long> friendIds = friendsByUser.getOrDefault(id, Collections.emptySet());
        return friendIds.stream()
                .map(this::getById)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(Long id, Long otherId) {
        getById(id);
        getById(otherId);

        Set<Long> first = friendsByUser.getOrDefault(id, Collections.emptySet());
        Set<Long> second = friendsByUser.getOrDefault(otherId, Collections.emptySet());

        return first.stream()
                .filter(second::contains)
                .map(this::getById)
                .collect(Collectors.toList());
    }

    private void validate(User user) {
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new ValidationException("Некорректный email");
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может быть пустым или содержать пробелы");
        }
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }
}