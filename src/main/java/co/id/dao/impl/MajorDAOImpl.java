package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.MajorDAO;
import co.id.model.Major;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class MajorDAOImpl extends DatabaseConfiguration implements MajorDAO {

    @Override
    public List<Major> getAllMajors() {
        List<Major> majors = new ArrayList<>();
        String sql = "SELECT * FROM mst_jurusan";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                majors.add(mapResultSetToMajor(resultSet));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return majors;
    }

    @Override
    public List<Major> getMajorBy(String keyword) {
        List<Major> majors = new ArrayList<>();
        String sql = "SELECT * FROM mst_jurusan WHERE nama_jurusan LIKE ? OR kode_jurusan LIKE ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, "%" + keyword + "%");
            preparedStatement.setString(2, "%" + keyword + "%");

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    majors.add(mapResultSetToMajor(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return majors;
    }

    @Override
    public List<Major> getMajors(int page, int size) {
        List<Major> majors = new ArrayList<>();
        String sql = "SELECT * FROM mst_jurusan ORDER BY id_jurusan LIMIT ? OFFSET ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, size);
            preparedStatement.setInt(2, (page - 1) * size);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    majors.add(mapResultSetToMajor(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return majors;
    }

    @Override
    public int countMajors() {
        String sql = "SELECT COUNT(*) AS total FROM mst_jurusan";

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
    public Major getById(int id) {
        String sql = "SELECT * FROM mst_jurusan WHERE id_jurusan = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToMajor(resultSet);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public void saveOrUpdate(Major major) {
        String sqlInsert = "INSERT INTO mst_jurusan(nama_jurusan, singkatan, diubah_oleh) VALUES (?, ?, ?)";
        String sqlUpdate = "UPDATE mst_jurusan SET nama_jurusan = ?, singkatan = ?, diubah_oleh = ? WHERE id_jurusan = ?";

        try (Connection connection = getConnection()) {
            if (major.getId() == 0) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setString(1, major.getName());
                    preparedStatement.setString(2, major.getAbbreviation());
                    preparedStatement.setString(3, major.getUpdatedBy());

                    preparedStatement.executeUpdate();

                    try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int newId = generatedKeys.getInt(1);
                            String generatedCode = "J" + String.format("%02d", newId);

                            try (PreparedStatement updateCode = connection.prepareStatement(
                                    "UPDATE mst_jurusan SET kode_jurusan = ? WHERE id_jurusan = ?")) {
                                updateCode.setString(1, generatedCode);
                                updateCode.setInt(2, newId);
                                updateCode.executeUpdate();
                            }

                            major.setId(newId);
                            major.setCode(generatedCode);
                        }
                    }
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sqlUpdate)) {
                    preparedStatement.setString(1, major.getName());
                    preparedStatement.setString(2, major.getAbbreviation());
                    preparedStatement.setString(3, major.getUpdatedBy());
                    preparedStatement.setInt(4, major.getId());

                    preparedStatement.executeUpdate();
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM mst_jurusan WHERE id_jurusan = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    // Helper method untuk mapping ResultSet ke object Major
    private Major mapResultSetToMajor(ResultSet resultSet) throws Exception {
        Major major = new Major();
        major.setId(resultSet.getInt("id_jurusan"));
        major.setCode(resultSet.getString("kode_jurusan"));
        major.setName(resultSet.getString("nama_jurusan"));
        major.setAbbreviation(resultSet.getString("singkatan"));

        Timestamp createdAt = resultSet.getTimestamp("tanggal_buat");
        if (createdAt != null) {
            major.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = resultSet.getTimestamp("tanggal_update");
        if (updatedAt != null) {
            major.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        major.setUpdatedBy(resultSet.getString("diubah_oleh"));

        return major;
    }
}