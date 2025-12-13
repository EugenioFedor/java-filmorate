package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserControllerTest {

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
    }

    @Test
    void shouldCreateUser_whenValidUser() {
        User user = new User();
        user.setEmail("user@mail.com");
        user.setLogin("login");
        user.setName("User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User created = userController.create(user);

        assertNotNull(created.getId());
        assertEquals("user@mail.com", created.getEmail());
    }

    @Test
    void shouldSetLoginAsName_whenNameIsBlank() {
        User user = new User();
        user.setEmail("user@mail.com");
        user.setLogin("login");
        user.setName("");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User created = userController.create(user);

        assertEquals("login", created.getName());
    }

    @Test
    void shouldThrowException_whenEmailIsBlank() {
        User user = new User();
        user.setEmail("");
        user.setLogin("login");

        assertThrows(ValidationException.class,
                () -> userController.create(user));
    }

    @Test
    void shouldThrowException_whenEmailWithoutAt() {
        User user = new User();
        user.setEmail("usermail.com");
        user.setLogin("login");

        assertThrows(ValidationException.class,
                () -> userController.create(user));
    }

    @Test
    void shouldThrowException_whenLoginIsBlank() {
        User user = new User();
        user.setEmail("user@mail.com");
        user.setLogin("");

        assertThrows(ValidationException.class,
                () -> userController.create(user));
    }

    @Test
    void shouldThrowException_whenLoginContainsSpaces() {
        User user = new User();
        user.setEmail("user@mail.com");
        user.setLogin("bad login");

        assertThrows(ValidationException.class,
                () -> userController.create(user));
    }

    @Test
    void shouldThrowException_whenBirthdayInFuture() {
        User user = new User();
        user.setEmail("user@mail.com");
        user.setLogin("login");
        user.setBirthday(LocalDate.now().plusDays(1));

        assertThrows(ValidationException.class,
                () -> userController.create(user));
    }

    @Test
    void shouldThrowException_whenEmailAlreadyExists() {
        User first = new User();
        first.setEmail("user@mail.com");
        first.setLogin("login1");

        userController.create(first);

        User second = new User();
        second.setEmail("user@mail.com");
        second.setLogin("login2");

        assertThrows(DuplicatedDataException.class,
                () -> userController.create(second));
    }
}
