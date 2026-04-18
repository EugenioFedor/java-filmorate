package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1;

    @Override
    public User create(User user) {
        user.setId(nextId++);

        if (user.getFriends() == null) {
            user.setFriends(new HashSet<>());
        }

        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User user) {
        User existing = users.get(user.getId());

        if (existing == null) {
            throw new NotFoundException("User not found");
        }

        user.setFriends(existing.getFriends());

        users.put(user.getId(), user);
        return user;
    }

    @Override
    public Collection<User> getAll() {
        return users.values();
    }

    @Override
    public Optional<User> getById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        User user = getById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.getFriends().add(friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        User user = getById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        user.getFriends().remove(friendId);
    }

    @Override
    public Collection<User> getFriends(Long userId) {
        return new ArrayList<>();
    }

    @Override
    public Collection<User> getCommonFriends(Long userId, Long otherId) {
        return new ArrayList<>();
    }

    @Override
    public Set<Long> getFriendsIds(Long id) {
        User user = getById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        return new HashSet<>(user.getFriends());
    }

    @Override
    public void delete(Long userId) {
        User removed = users.remove(userId);

        if (removed == null) {
            throw new NotFoundException("User not found");
        }

        for (User user : users.values()) {
            user.getFriends().remove(userId);
        }
    }

    @Override
    public List<Event> getFeed(Long userId) {
        return new ArrayList<>();
    }
}