package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.UserDAO;
import co.id.model.Teacher;
import co.id.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class UserDAOImpl extends DatabaseConfiguration implements UserDAO{
    private static final String SELECT_JOIN =
            "SELECT u.*, g.nip, g.nama_guru "
            + "FROM users u "
            + "LEFT JOIN mst_guru g ON u.id_guru = g.id_guru ";

    @Override
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                users.add(mapBasicResultSetToUser(resultSet));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return users;
    }

    @Override
    public List<User> getUsers(int page, int size) {
        List<User> users = new ArrayList<>();
        String sql = SELECT_JOIN + "ORDER BY u.user_id LIMIT ? OFFSET ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, size);
            preparedStatement.setInt(2, (page - 1) * size);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapResultSetToUser(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return users;
    }

    @Override
    public int countUsers() {
        String sql = "SELECT COUNT(*) AS total FROM users";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (resultSet.next()) {
                return resultSet.getInt("total");
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return 0;
    }

    @Override
    public User getById(int id) {
        String sql = SELECT_JOIN + "WHERE u.user_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public User getByUsername(String username) {
        String sql = SELECT_JOIN + "WHERE u.username = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, username);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public List<User> getByKeyword(String keyword) {
        List<User> users = new ArrayList<>();
        String sql = SELECT_JOIN + "WHERE u.username LIKE ? OR u.full_name LIKE ? OR g.nama_guru LIKE ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            String param = "%" + keyword + "%";
            preparedStatement.setString(1, param);
            preparedStatement.setString(2, param);
            preparedStatement.setString(3, param);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapResultSetToUser(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return users;
    }

    @Override
    public void saveOrUpdate(User user) {
        String sqlInsert = "INSERT INTO users(username, password, full_name, role, id_guru, photo_path, diubah_oleh) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlUpdate = "UPDATE users SET username = ?, full_name = ?, role = ?, id_guru = ?, photo_path = ?, diubah_oleh = ? WHERE user_id = ?";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (user.getId() == 0) {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                        preparedStatement.setString(1, user.getUsername());
                        preparedStatement.setString(2, user.getPassword());
                        preparedStatement.setString(3, user.getFullName());
                        preparedStatement.setString(4, user.getRole());

                        if (user.getTeacher() != null) {
                            preparedStatement.setInt(5, user.getTeacher().getId());
                        } else {
                            preparedStatement.setNull(5, Types.INTEGER);
                        }

                        preparedStatement.setString(6, user.getPhotoPath());
                        preparedStatement.setString(7, user.getUpdatedBy());

                        preparedStatement.executeUpdate();

                        try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                user.setId(generatedKeys.getInt(1));
                            }
                        }
                    }
                } else {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sqlUpdate)) {
                        preparedStatement.setString(1, user.getUsername());
                        preparedStatement.setString(2, user.getFullName());
                        preparedStatement.setString(3, user.getRole());

                        if (user.getTeacher() != null) {
                            preparedStatement.setInt(4, user.getTeacher().getId());
                        } else {
                            preparedStatement.setNull(4, Types.INTEGER);
                        }

                        preparedStatement.setString(5, user.getPhotoPath());
                        preparedStatement.setString(6, user.getUpdatedBy());
                        preparedStatement.setInt(7, user.getId());

                        preparedStatement.executeUpdate();
                    }
                }

                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void updatePassword(int id, String hashedPassword) {
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, hashedPassword);
            preparedStatement.setInt(2, id);

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean isUsernameTaken(String username, int excludeId) {
        String sql = "SELECT COUNT(*) AS total FROM users WHERE username = ? AND user_id <> ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, username);
            preparedStatement.setInt(2, excludeId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total") > 0;
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }

    @Override
    public boolean isTeacherLinked(int teacherId, int excludeId) {
        String sql = "SELECT COUNT(*) AS total FROM users WHERE id_guru = ? AND user_id <> ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, teacherId);
            preparedStatement.setInt(2, excludeId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total") > 0;
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return false;
    }
    
    // Helper method untuk mapping ResultSet ke object User (tanpa data relasi)
    private User mapBasicResultSetToUser(ResultSet resultSet) throws Exception {
        User user = new User();
        user.setId(resultSet.getInt("user_id"));
        user.setUsername(resultSet.getString("username"));
        user.setPassword(resultSet.getString("password"));
        user.setFullName(resultSet.getString("full_name"));
        user.setRole(resultSet.getString("role"));
        user.setPhotoPath(resultSet.getString("photo_path"));

        Timestamp createdAt = resultSet.getTimestamp("tanggal_buat");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("tanggal_update");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        user.setUpdatedBy(resultSet.getString("diubah_oleh"));

        return user;
    }
    
    // Helper method untuk mapping ResultSet ke object User (lengkap dengan data relasi Teacher)
    private User mapResultSetToUser(ResultSet resultSet) throws Exception {
        User user = mapBasicResultSetToUser(resultSet);

        int teacherId = resultSet.getInt("id_guru");
        if (!resultSet.wasNull()) {
            Teacher teacher = new Teacher();
            teacher.setId(teacherId);
            teacher.setNip(resultSet.getString("nip"));
            teacher.setName(resultSet.getString("nama_guru"));
            user.setTeacher(teacher);
        }

        return user;
    }
}
