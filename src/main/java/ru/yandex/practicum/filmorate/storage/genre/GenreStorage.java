package ru.yandex.practicum.filmorate.storage.genre;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;


public interface GenreStorage {

    Genre createGenre(Genre genre);

    Genre updateGenre(Genre genre);

    Genre getGenre(int id);

    List<Genre> getAllGenres();

}
