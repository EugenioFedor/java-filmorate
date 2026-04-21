package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserStorage {

    User create(User user);

    User update(User user);

    void delete(Long userId);

    Set<Long> getFriendsIds(Long id);

    Collection<User> getAll();

    Optional<User> getById(Long id);

    void addFriend(Long id, Long friendId);

    void removeFriend(Long id, Long friendId);

    Collection<User> getFriends(Long id);

    Collection<User> getCommonFriends(Long id, Long otherId);

    List<Film> getRecommendations(Long userId);

    List<Event> getFeed(Long userId);
}