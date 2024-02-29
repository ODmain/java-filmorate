package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository(value = "userDbStorage")
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<User> getFriends(int id) {
        String sql = "SELECT u.*," +
                "       (SELECT GROUP_CONCAT(" +
                "                       CASE" +
                "                           WHEN F.USER_ID = u.ID THEN F.FRIEND_ID" +
                "                           WHEN F.FRIEND_ID = u.ID AND F.STATUS = true THEN F.USER_ID END SEPARATOR ','" +
                "               )" +
                "        FROM FRIENDSHIPS as F) as friends_id " +
                "FROM USERS as u " +
                "WHERE u.ID in (SELECT (" +
                "                          CASE" +
                "                              WHEN F.USER_ID = ? THEN F.FRIEND_ID" +
                "                              WHEN F.FRIEND_ID = ? AND F.STATUS = true THEN F.USER_ID END" +
                "                          )" +
                "               FROM FRIENDSHIPS as F);";
        return jdbcTemplate.query(sql, new UserMapper(), id, id);
    }

    @Override
    public boolean addFriend(int id, int friendId) {
        String sql = "INSERT INTO FRIENDSHIPS (USER_ID, FRIEND_ID)" +
                " SELECT ?, ?" +
                " FROM dual" +
                " WHERE NOT EXISTS(" +
                "SELECT 1" +
                " FROM FRIENDSHIPS" +
                " WHERE (USER_ID = ? AND FRIEND_ID = ?)" +
                " OR (USER_ID = ? AND FRIEND_ID = ?))";

        return jdbcTemplate.update(sql, id, friendId, id, friendId, friendId, id) > 0;
    }

    @Override
    public void deleteFriend(int id, int friendId) {
        String sqlQuery = "DELETE FROM friendships " +
                "WHERE (USER_ID = ? AND FRIEND_ID = ?);";
        jdbcTemplate.update(sqlQuery, id, friendId);
    }

    @Override
    public List<User> getCommonFriends(int id, int otherId) {
        String sqlQuery = "SELECT u.*," +
                "       (SELECT GROUP_CONCAT(" +
                "                       CASE" +
                "                           WHEN F.USER_ID = u.ID THEN F.FRIEND_ID" +
                "                           WHEN F.FRIEND_ID = u.ID AND F.STATUS = true THEN F.USER_ID END SEPARATOR ','" +
                "               )" +
                "        FROM FRIENDSHIPS as F) as friends_id " +
                "FROM USERS as u " +
                "WHERE ID IN (" +
                "    SELECT f.friend_id" +
                "    FROM (SELECT USER_ID AS user_id, FRIEND_ID AS friend_id" +
                "          FROM FRIENDSHIPS" +
                "          UNION ALL" +
                "          SELECT FRIEND_ID AS user_id, USER_ID AS friend_id" +
                "          FROM FRIENDSHIPS" +
                "          WHERE STATUS = true) AS f" +
                "    WHERE f.user_id = ?" +
                "    INTERSECT" +
                "    SELECT f.friend_id" +
                "    FROM (SELECT USER_ID AS user_id, FRIEND_ID AS friend_id" +
                "          FROM FRIENDSHIPS" +
                "          UNION ALL" +
                "          SELECT FRIEND_ID AS user_id, USER_ID AS friend_id" +
                "          FROM FRIENDSHIPS" +
                "          WHERE STATUS = true) AS f" +
                "    WHERE f.user_id = ?);";
        return jdbcTemplate.query(sqlQuery, new UserMapper(), id, otherId);
    }

    @Override
    public User createUser(User user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO USERS (EMAIL, LOGIN, NAME, BIRTHDAY) VALUES (?, ?, ?, ?)",
                    new String[]{"ID"}
            );
            preparedStatement.setString(1, user.getEmail());
            preparedStatement.setString(2, user.getLogin());
            preparedStatement.setString(3, user.getName());
            preparedStatement.setDate(4, Date.valueOf(user.getBirthday()));
            return preparedStatement;
        }, keyHolder);

        Integer userId = Objects.requireNonNull(keyHolder.getKey()).intValue();
        return getUser(userId);
    }

    @Override
    public User updateUser(User user) {
        if (!existsUser(user.getId())) {
            return null;
        }
        jdbcTemplate.update("UPDATE USERS SET EMAIL = ?, LOGIN = ?, NAME = ?, BIRTHDAY = ? WHERE ID = ?",
                user.getEmail(), user.getLogin(), user.getName(), user.getBirthday(), user.getId());
        return user;
    }

    @Override
    public List<User> getALlUsers() {
        String sqlQuery = "SELECT u.*, GROUP_CONCAT(f.friend_id) AS friends_id " +
                "FROM USERS AS u" +
                "         LEFT JOIN (" +
                "    SELECT USER_ID AS user_id, FRIEND_ID AS friend_id" +
                "    FROM FRIENDSHIPS" +
                "    UNION ALL" +
                "    SELECT FRIEND_ID AS user_id, USER_ID AS friend_id" +
                "    FROM FRIENDSHIPS" +
                "    WHERE STATUS = true" +
                ") AS f ON u.ID = f.user_id " +
                "GROUP BY u.ID;";
        return jdbcTemplate.query(sqlQuery, new UserMapper());
    }

    @Override
    public User getUser(int id) {
        if (!existsUser(id)) {
            return null;
        }
        String sql = "SELECT u.*, GROUP_CONCAT(f.friend_id) AS friends_id " +
                "FROM USERS AS u" +
                "         LEFT JOIN (" +
                "    SELECT USER_ID AS user_id, FRIEND_ID AS friend_id" +
                "    FROM FRIENDSHIPS" +
                "    UNION ALL" +
                "    SELECT FRIEND_ID AS user_id, USER_ID AS friend_id" +
                "    FROM FRIENDSHIPS" +
                "    WHERE STATUS = true" +
                ") AS f ON u.ID = f.user_id " +
                "WHERE u.ID = ?group by ID, EMAIL, LOGIN, NAME, BIRTHDAY;";
        return jdbcTemplate.queryForObject(sql, new UserMapper(), id);
    }

    @Override
    public boolean existsUser(int userId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM USERS WHERE ID = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, userId));
    }

    private static class UserMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            Integer userId = rs.getInt("ID");
            return User.builder()
                    .id(userId)
                    .email(rs.getString("EMAIL"))
                    .login(rs.getString("LOGIN"))
                    .name(rs.getString("NAME"))
                    .birthday(rs.getDate("BIRTHDAY").toLocalDate())
                    .friends(getFriendsId(rs))
                    .build();
        }

        public Set<Integer> getFriendsId(ResultSet rs) throws SQLException {
            Set<Integer> friendsId = new HashSet<>();
            String stringIds = rs.getString("friends_id");
            if (stringIds != null) {
                String[] split = stringIds.split(",");
                for (String splitId : split) {
                    friendsId.add(Integer.parseInt(splitId));
                }
            }
            return friendsId;
        }
    }
}
