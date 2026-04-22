package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserDbStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDbStorage userStorage;

    public User create(User user) {
        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new ValidationException("Invalid email");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new ValidationException("Login cannot be empty");
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем");
        }

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        return userStorage.create(user);
    }

    public User update(User user) {
        return userStorage.update(user);
    }

    public Collection<User> getAll() {
        return userStorage.getAll();
    }

    public User getById(Long id) {
        return userStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public void deleteUserById(Long id) {
        getById(id);
        userStorage.delete(id);
    }

    public void addFriend(Long id, Long friendId) {
        getById(id);
        getById(friendId);
        userStorage.addFriend(id, friendId);
    }

    public void removeFriend(Long id, Long friendId) {
        getById(id);
        getById(friendId);
        userStorage.removeFriend(id, friendId);
    }

    public List<User> getFriends(Long id) {
        getById(id);
        return new ArrayList<>(userStorage.getFriends(id));
    }

    public List<User> getCommonFriends(Long id, Long otherId) {
        getById(id);
        getById(otherId);

        Set<Long> friends1 = userStorage.getFriendsIds(id);
        Set<Long> friends2 = userStorage.getFriendsIds(otherId);

        return friends1.stream()
                .filter(friends2::contains)
                .map(this::getById)
                .collect(Collectors.toList());
    }

    public List<Film> getRecommendations(Long userId) {
        getById(userId);
        return userStorage.getRecommendations(userId);
    }

    public List<Event> getFeed(Long userId) {
        getById(userId);
        return userStorage.getFeed(userId);
    }
}