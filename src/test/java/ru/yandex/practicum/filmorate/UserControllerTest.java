package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerTest {

    private UserController userController;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        userController = new UserController(userService);
    }

    @Test
    void addUser_shouldAddUser() {
        User user = new User();
        user.setLogin("john_doe");
        user.setEmail("john@mail.com");
        user.setName("John Doe");
        user.setBirthday(LocalDate.parse("1990-01-01"));

        userController.create(user);

        Collection<User> users = userController.getAll();
        assertTrue(users.contains(user));
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        User user1 = new User();
        user1.setLogin("alice");
        user1.setEmail("alice@mail.com");
        user1.setName("Alice");
        user1.setBirthday(LocalDate.parse("1985-05-05"));

        User user2 = new User();
        user2.setLogin("bob");
        user2.setEmail("bob@mail.com");
        user2.setName("Bob");
        user2.setBirthday(LocalDate.parse("1980-12-12"));

        userController.create(user1);
        userController.create(user2);

        Collection<User> users = userController.getAll();
        assertEquals(2, users.size());
        assertTrue(users.contains(user1));
        assertTrue(users.contains(user2));
    }
}
