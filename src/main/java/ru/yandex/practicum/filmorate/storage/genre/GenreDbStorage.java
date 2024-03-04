package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ValidException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;


@Repository
@RequiredArgsConstructor
public class GenreDbStorage implements GenreStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Genre createGenre(Genre genre) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO GENRES (GENRE_NAME)" +
                    " VALUES (?)", new String[]{"GENRE_ID"});
            preparedStatement.setString(1, genre.getName());
            return preparedStatement;
        }, keyHolder);
        int genreId = Objects.requireNonNull(keyHolder.getKey()).intValue();
        genre.setId(genreId);
        return genre;
    }

    @Override
    public Genre updateGenre(Genre genre) {
        if (!existsGenre(genre.getId())) {
            throw new ValidException("Жанр не найден.", HttpStatus.NOT_FOUND);
        } else {
            String s = "UPDATE GENRES " + "SET GENRE_NAME = ? WHERE GENRE_ID = ?";
            jdbcTemplate.update(s, genre.getName(), genre.getId());
            return genre;
        }
    }

    @Override
    public Genre getGenre(int id) {
        if (!existsGenre(id)) {
            throw new ValidException("Жанр не найден.", HttpStatus.NOT_FOUND);
        } else {
            String sqlQuery = "SELECT * FROM GENRES WHERE GENRE_ID = ?";
            return jdbcTemplate.queryForObject(sqlQuery, new GenreMapper(), id);
        }
    }

    @Override
    public List<Genre> getAllGenres() {
        String s = "SELECT * FROM GENRES ORDER BY GENRE_ID";
        return jdbcTemplate.query(s, new GenreMapper());
    }

    private static class GenreMapper implements RowMapper<Genre> {
        @Override
        public Genre mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Genre.builder()
                    .id(rs.getInt("GENRE_ID"))
                    .name(rs.getString("GENRE_NAME"))
                    .build();
        }
    }

    boolean existsGenre(int id) {
        String s = "SELECT EXISTS(SELECT 1 FROM GENRES WHERE GENRE_ID = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(s, Boolean.class, id));
    }
}
