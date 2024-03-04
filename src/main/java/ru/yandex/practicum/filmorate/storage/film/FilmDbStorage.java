package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository(value = "filmDbStorage")
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public Film getFilm(int id) {
        if (!existsFilm(id)) {
            return null;
        }
        String sql = "SELECT " +
                "f.*, M.MPA_NAME AS mpa_name, " +
                "GROUP_CONCAT(G2.GENRE_ID ORDER BY G2.GENRE_ID) AS GENRES_ID_LIST, " +
                "GROUP_CONCAT(G2.GENRE_NAME ORDER BY G2.GENRE_ID) AS genres_list, " +
                "(SELECT GROUP_CONCAT(USER_ID) FROM PUBLIC.FILMS_LIKES WHERE FILM_ID = f.ID) AS FILMS_LIKES " +
                "FROM FILMS AS f " +
                "LEFT JOIN PUBLIC.MPA M on M.MPA_ID = f.MPA_ID " +
                "LEFT JOIN PUBLIC.FILMS_GENRES FG on f.ID = FG.FILM_ID " +
                "LEFT JOIN PUBLIC.GENRES G2 on G2.GENRE_ID = FG.GENRE_ID " +
                "WHERE f.id = ? " +
                "GROUP BY f.ID";

        return jdbcTemplate.queryForObject(sql, new FilmMapper(), id);
    }

    @Override
    public List<Film> getTopTenOfFilms(int count) {
        String sql = "SELECT f.*," +
                "       M.MPA_NAME AS mpa_name," +
                "       (SELECT GROUP_CONCAT(GENRE_ID) FROM FILMS_GENRES WHERE FILM_ID = f.id) AS GENRES_ID_LIST," +
                "       (SELECT GROUP_CONCAT(GENRE_NAME)" +
                "        FROM GENRES" +
                "        WHERE GENRE_ID IN (SELECT GENRE_ID FROM FILMS_GENRES WHERE FILM_ID = f.id)) AS genres_list," +
                "       (SELECT GROUP_CONCAT(USER_ID)" +
                "        FROM PUBLIC.FILMS_LIKES" +
                "        WHERE FILM_ID = f.ID)                                                AS FILMS_LIKES " +
                "FROM FILMS AS f" +
                "         LEFT JOIN PUBLIC.MPA M on M.MPA_ID = f.MPA_ID" +
                "         LEFT JOIN PUBLIC.FILMS_LIKES FL on F.ID = FL.FILM_ID " +
                "GROUP BY f.ID " +
                "ORDER BY COUNT(FL.USER_ID) DESC, F.ID " +
                "LIMIT ?";
        return jdbcTemplate.query(sql, new FilmMapper(), count);
    }

    @Override
    public boolean addLike(int id, int userId) {
        String sql = "INSERT INTO FILMS_LIKES(FILM_ID, USER_ID) " +
                "SELECT ?, ? " +
                "FROM dual " +
                "WHERE NOT EXISTS(" +
                "    SELECT 1" +
                "    FROM FILMS_LIKES" +
                "    WHERE (FILM_ID = ? AND USER_ID = ?));";
        return jdbcTemplate.update(sql, id, userId, id, userId) > 0;
    }

    @Override
    public boolean deleteLike(int filmId, int userId) {
        String sqlQuery = "DELETE FROM FILMS_LIKES WHERE FILM_ID = ? AND USER_ID = ?";
        return jdbcTemplate.update(sqlQuery, filmId, userId) > 0;
    }

    @Override
    public Film createFilm(Film film) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO FILMS (NAME, DESCRIPTION, RELEASE_DATE, DURATION, MPA_ID)" +
                            " VALUES (?, ?, ?, ?, ?)",
                    new String[]{"ID"}
            );
            preparedStatement.setString(1, film.getName());
            preparedStatement.setString(2, film.getDescription());
            preparedStatement.setDate(3, Date.valueOf(film.getReleaseDate()));
            preparedStatement.setInt(4, film.getDuration());
            preparedStatement.setInt(5, film.getMpa().getId());
            return preparedStatement;
        }, keyHolder);

        int filmId = Objects.requireNonNull(keyHolder.getKey()).intValue();
        film.setId(filmId);
        insertFilmGenres(film);
        return getFilm(filmId);
    }

    @Override
    public Film updateFilm(Film film) {
        if (!existsFilm(film.getId())) {
            return null;
        }
        jdbcTemplate.update("UPDATE FILMS" +
                        " SET NAME = ?, DESCRIPTION = ?, RELEASE_DATE = ?, DURATION = ?, MPA_ID = ? WHERE ID = ?",
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());
        jdbcTemplate.update("DELETE FROM FILMS_GENRES WHERE FILM_ID = ?", film.getId());

        insertFilmGenres(film);
        return getFilm(film.getId());
    }

    @Override
    public List<Film> getALlFilms() {
        String sql = "SELECT f.*," +
                "       M.MPA_NAME  AS mpa_name," +
                "       (SELECT GROUP_CONCAT(GENRE_ID) FROM FILMS_GENRES WHERE FILM_ID = f.id) AS GENRES_ID_LIST," +
                "       (SELECT GROUP_CONCAT(GENRE_NAME)" +
                "        FROM GENRES" +
                "        WHERE GENRE_ID IN (SELECT GENRE_ID FROM FILMS_GENRES WHERE FILM_ID = f.id)) AS genres_list," +
                "       (SELECT GROUP_CONCAT(USER_ID)" +
                "        FROM PUBLIC.FILMS_LIKES" +
                "        WHERE FILM_ID = f.ID)                                                AS FILMS_LIKES " +
                "FROM FILMS AS f" +
                "         LEFT JOIN PUBLIC.MPA M on M.MPA_ID = f.MPA_ID";
        return jdbcTemplate.query(sql, new FilmMapper());
    }

    private void insertFilmGenres(Film film) {
        if (film.getGenres() == null) {
            return;
        }
        List<Object[]> batchArgs = new ArrayList<>();
        for (Genre genre : film.getGenres()) {
            batchArgs.add(new Object[]{film.getId(), genre.getId()});
        }
        jdbcTemplate.batchUpdate("INSERT INTO FILMS_GENRES (FILM_ID, GENRE_ID) VALUES (?, ?)", batchArgs);
    }

    @Override
    public boolean existsFilm(int id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM FILMS WHERE ID = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, id));
    }

    private static class FilmMapper implements RowMapper<Film> {
        @Override
        public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Film.builder()
                    .id(rs.getInt("ID"))
                    .name(rs.getString("NAME"))
                    .description(rs.getString("DESCRIPTION"))
                    .releaseDate(rs.getDate("RELEASE_DATE").toLocalDate())
                    .duration(rs.getInt("DURATION"))
                    .mpa(getMpa(rs))
                    .genres(getGenres(rs))
                    .likes(getLikes(rs))
                    .build();
        }

        private Mpa getMpa(ResultSet rs) throws SQLException {
            return Mpa.builder()
                    .id(rs.getInt("MPA_ID"))
                    .name(rs.getString("MPA_NAME"))
                    .build();
        }

        private Set<Genre> getGenres(ResultSet rs) throws SQLException {
            Set<Genre> genres = new HashSet<>();

            String genresIdList = rs.getString("GENRES_ID_LIST");
            String genresList = rs.getString("GENRES_LIST");

            if (genresIdList != null && genresList != null) {
                String[] genreIds = genresIdList.split(",");
                String[] genreNames = genresList.split(",");

                for (int i = 0; i < genreIds.length; i++) {
                    genres.add(Genre.builder()
                            .id(Integer.parseInt(genreIds[i]))
                            .name(genreNames[i])
                            .build());
                }
            }
            return genres;
        }

        private Set<Integer> getLikes(ResultSet rs) throws SQLException {
            Set<Integer> likes = new HashSet<>();

            String likesIdList = rs.getString("FILMS_LIKES");
            if (likesIdList != null) {
                String[] likeIds = likesIdList.split(",");

                for (String likeId : likeIds) {
                    likes.add(Integer.parseInt(likeId));
                }
            }
            return likes;
        }
    }
}

