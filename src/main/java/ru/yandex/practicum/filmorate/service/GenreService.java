package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;

import java.util.List;

@RequiredArgsConstructor
@Service
public class GenreService {
    private final GenreStorage genreStorage;

    public Genre createGenre(Genre genre) {
        return genreStorage.createGenre(genre);
    }

    public List<Genre> getAllGenres() {
        return genreStorage.getAllGenres();
    }

    public Genre getGenre(int id) {
        Genre genre = genreStorage.getGenre(id);
        if (genre == null) {
            throw new ValidException("Жанр не найден", HttpStatus.NOT_FOUND);
        }
        return genre;
    }

    public Genre updateGenre(Genre genre) {
        Genre updatedGenre = genreStorage.updateGenre(genre);
        if (updatedGenre == null) {
            throw new ValidException("Жанр не найден", HttpStatus.NOT_FOUND);
        }
        return updatedGenre;
    }

}
