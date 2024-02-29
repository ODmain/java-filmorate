package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Service
@Slf4j
public class UserService {
    private final UserStorage userStorage;

    public UserService(@Qualifier("userDbStorage") UserDbStorage userDbStorage) {
        this.userStorage = userDbStorage;
    }

    public void addFriend(int id, int friendId) {
        checkId(id, friendId);
        boolean isAdded = userStorage.addFriend(id, friendId);
        if (!isAdded) {
            throw new ValidException("Пользователь уже добавлен в друзья", HttpStatus.BAD_REQUEST);
        }
        log.info("Пользователь добавлен в друзья");
    }

    public void deleteFriend(int id, int friendId) {
        userStorage.deleteFriend(id, friendId);
    }

    public List<User> getCommonFriends(int id, int otherId) {
        checkId(id, otherId);
        return userStorage.getCommonFriends(id, otherId);
    }

    public List<User> getFriends(int id) {
        if (!userStorage.existsUser(id)) {
            throw new ValidException("Пользователь с таким id не найден", HttpStatus.NOT_FOUND);
        }
        log.info("Список друзей отправлен.");
        return userStorage.getFriends(id);
    }

    public User createUser(User user) {
        if (user.getName().isEmpty() || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        return userStorage.createUser(user);
    }

    public User updateUser(User user) {
        User updatedUser = userStorage.updateUser(validName(user));
        if (updatedUser == null) {
            throw new ValidException("Пользователь с указанным id не найден", HttpStatus.NOT_FOUND);
        }
        return updatedUser;
    }

    public User getUser(int id) {
        User user = userStorage.getUser(id);
        if (user == null) {
            throw new ValidException("Пользователь с таким id не найден", HttpStatus.NOT_FOUND);
        }
        return user;
    }

    public List<User> getALlUsers() {
        return userStorage.getALlUsers();
    }

    private void checkId(int id, int otherId) {
        if (!userStorage.existsUser(id)) {
            throw new ValidException("Пользователь с таким id не найден", HttpStatus.NOT_FOUND);
        }
        if (!userStorage.existsUser(otherId)) {
            throw new ValidException("Пользователь с таким id не найден", HttpStatus.NOT_FOUND);
        }
    }

    private User validName(User user) {
        String userName = user.getName();
        if (userName == null || userName.isEmpty()) {
            user.setName(user.getLogin());
        }
        return user;
    }
}
