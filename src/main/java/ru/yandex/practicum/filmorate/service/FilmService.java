package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

import static ru.yandex.practicum.filmorate.constant.Constant.REGEX_DATE;


@Service
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film getFilm(int id) {
        Film film = filmStorage.getFilm(id);
        if (film == null) {
            throw new ValidException("Фильм не найден", HttpStatus.NOT_FOUND);
        }
        return film;
    }


    private void checkId(int id, int userId) {
        if (!userStorage.existsUser(userId)) {
            throw new ValidException("Пользователь с id " + userId + " не найден", HttpStatus.NOT_FOUND);
        }
        if (!filmStorage.existsFilm(id)) {
            throw new ValidException("Фильма с ID: " + id + " не существует", HttpStatus.NOT_FOUND);
        }
    }

    public void addLike(int id, int userId) {
        checkId(id, userId);
        boolean like = filmStorage.addLike(id, userId);
        if (!like) {
            throw new ValidException("Пользователь уже поставил лайк", HttpStatus.BAD_REQUEST);
        }
        log.info("Пользователь поставил лайк");
    }

    public void deleteLike(int id, int userId) {
        checkId(id, userId);
        boolean like = filmStorage.deleteLike(id, userId);
        if (!like) {
            throw new ValidException("У пользователя нет лайка на этом фильме", HttpStatus.BAD_REQUEST);
        }
        log.info("Пользователь удалил лайк");
    }

    public List<Film> getTopTenOfFilms(String count) {
        int count1 = Integer.parseInt(count);
        return filmStorage.getTopTenOfFilms(count1);
    }

    public Film createFilm(Film film) {
        if (film.getReleaseDate().isBefore(REGEX_DATE)) {
            throw new ValidException("Дата релиза не соответствует правилам", HttpStatus.BAD_REQUEST);
        }
        return filmStorage.createFilm(film);
    }


    public Film updateFilm(Film film) {
        Film updatedFilm = filmStorage.updateFilm(film);
        if (updatedFilm == null) {
            throw new ValidException("Фильм не найден", HttpStatus.NOT_FOUND);
        }
        return updatedFilm;
    }


    public List<Film> getALlFilms() {
        return filmStorage.getALlFilms();
    }
}
