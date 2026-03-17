package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserStorage userStorage;

    public User create(User user) {

        if (user.getEmail() == null || !user.getEmail().contains("@")) {
            throw new ValidationException("Invalid email");
        }

        if (user.getLogin() == null || user.getLogin().contains(" ")) {
            throw new ValidationException("Invalid login");
        }

        if (user.getBirthday() == null) {
            throw new ValidationException("Birthday cannot be null");
        }

        if (user.getBirthday().isAfter(LocalDate.now())) {
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

    public Collection<User> findAll() {
        return userStorage.getAll();
    }

    public User getById(Long id) {
        return userStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public void addFriend(Long id, Long friendId) {

        userStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        userStorage.getById(friendId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        userStorage.addFriend(id, friendId);
    }

    public void removeFriend(Long id, Long friendId) {

        getById(id);
        getById(friendId);

        userStorage.removeFriend(id, friendId);
    }

    public List<User> getFriends(Long id) {

        getById(id);

        return userStorage.getFriendsIds(id).stream()
                .map(this::getById)
                .toList();
    }

    public List<User> getCommonFriends(Long id, Long otherId) {

        Set<Long> friends1 = userStorage.getFriendsIds(id);
        Set<Long> friends2 = userStorage.getFriendsIds(otherId);

        return friends1.stream()
                .filter(friends2::contains)
                .map(userStorage::getById)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public Collection<User> getAll() {
        return userStorage.getAll();
    }
}