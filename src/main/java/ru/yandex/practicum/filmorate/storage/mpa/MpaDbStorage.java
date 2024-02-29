package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ValidException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class MpaDbStorage implements MpaStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Mpa createMpa(Mpa mpa) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO MPA (MPA_NAME)" +
                    " VALUES (?)", new String[]{"MPA_ID"});
            preparedStatement.setString(1, mpa.getName());
            return preparedStatement;
        }, keyHolder);
        int id = Objects.requireNonNull(keyHolder.getKey()).intValue();
        mpa.setId(id);
        return mpa;
    }

    @Override
    public Mpa getMpa(int id) {
        if (!existsMpa(id)) {
            throw new ValidException("MPA не найден.", HttpStatus.NOT_FOUND);
        } else {
            String s = "SELECT * FROM MPA WHERE MPA_ID = ?";
            return jdbcTemplate.queryForObject(s, new MpaMapper(), id);
        }
    }

    @Override
    public List<Mpa> getAllMpa() {
        String s = "SELECT * FROM MPA ORDER BY MPA_ID";
        return jdbcTemplate.query(s, new MpaMapper());
    }

    @Override
    public Mpa updateMpa(Mpa mpa) {
        if (!existsMpa(mpa.getId())) {
            throw new ValidException("MPA не найден.", HttpStatus.NOT_FOUND);
        } else {
            String sqlQuery = "UPDATE MPA " + "SET MPA_NAME = ? WHERE MPA_ID = ?";
            jdbcTemplate.update(sqlQuery, mpa.getName(), mpa.getId());
            return mpa;
        }
    }

    private static class MpaMapper implements RowMapper<Mpa> {
        @Override
        public Mpa mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Mpa.builder()
                    .id(rs.getInt("MPA_ID"))
                    .name(rs.getString("MPA_NAME"))
                    .build();
        }
    }

    boolean existsMpa(int id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM MPA WHERE MPA_ID = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, id));
    }
}
