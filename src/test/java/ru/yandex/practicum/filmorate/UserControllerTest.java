package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.yandex.practicum.filmorate.exception.DuplicatedDataException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldReturnAllUsers() {
        when(userService.findAll()).thenReturn(Collections.emptyList());

        assertNotNull(userController.findAll());
        verify(userService, times(1)).findAll();
    }

    @Test
    void shouldCreateUser_whenValidUser() {
        User user = new User();
        user.setEmail("user@mail.com");
        user.setLogin("login");
        user.setName("User");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        when(userService.create(user)).thenReturn(user);

        User created = userController.create(user);

        assertEquals("user@mail.com", created.getEmail());
        verify(userService, times(1)).create(user);
    }

    @Test
    void shouldThrowException_whenInvalidUser() {
        User user = new User();
        user.setEmail("");
        user.setLogin("login");

        when(userService.create(user)).thenThrow(ValidationException.class);

        assertThrows(ValidationException.class, () -> userController.create(user));
        verify(userService, times(1)).create(user);
    }

    @Test
    void shouldThrowException_whenEmailAlreadyExists() {
        User user = new User();
        user.setEmail("user@mail.com");
        user.setLogin("login2");

        when(userService.create(user)).thenThrow(DuplicatedDataException.class);

        assertThrows(DuplicatedDataException.class, () -> userController.create(user));
        verify(userService, times(1)).create(user);
    }
}
