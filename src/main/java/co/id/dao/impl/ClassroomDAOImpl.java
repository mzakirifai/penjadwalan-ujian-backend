package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.ClassroomDAO;
import co.id.model.Classroom;
import co.id.model.Major;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ClassroomDAOImpl extends DatabaseConfiguration implements ClassroomDAO {

    @Override
    public List<Classroom> getAllClassrooms() {
        List<Classroom> classrooms = new ArrayList<>();
        String sql = "SELECT * FROM mst_kelas";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                classrooms.add(mapBasicResultSetToClassroom(resultSet));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return classrooms;
    }

    @Override
    public List<Classroom> getClassroomBy(String keyword) {
        List<Classroom> classrooms = new ArrayList<>();
        String sql = "SELECT k.*, "
                + "j.id_jurusan, "
                + "j.kode_jurusan, "
                + "j.nama_jurusan, "
                + "j.singkatan "
                + "FROM mst_kelas k "
                + "LEFT JOIN mst_jurusan j ON k.id_jurusan = j.id_jurusan "
                + "WHERE k.nama_kelas LIKE ? OR k.kode_kelas LIKE ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, "%" + keyword + "%");
            preparedStatement.setString(2, "%" + keyword + "%");

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    classrooms.add(mapResultSetToClassroom(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return classrooms;
    }

    @Override
    public List<Classroom> getClassrooms(int page, int size) {
        List<Classroom> classrooms = new ArrayList<>();
        String sql = "SELECT k.*, "
                + "j.id_jurusan, "
                + "j.kode_jurusan, "
                + "j.nama_jurusan, "
                + "j.singkatan "
                + "FROM mst_kelas k "
                + "LEFT JOIN mst_jurusan j ON k.id_jurusan = j.id_jurusan "
                + "ORDER BY k.id_kelas "
                + "LIMIT ? OFFSET ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, size);
            preparedStatement.setInt(2, (page - 1) * size);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    classrooms.add(mapResultSetToClassroom(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return classrooms;
    }

    @Override
    public int countClassrooms() {
        String sql = "SELECT COUNT(*) AS total FROM mst_kelas";

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
    public Classroom getById(int id) {
        String sql = "SELECT k.*, "
                + "j.id_jurusan, "
                + "j.kode_jurusan, "
                + "j.nama_jurusan, "
                + "j.singkatan "
                + "FROM mst_kelas k "
                + "LEFT JOIN mst_jurusan j ON k.id_jurusan = j.id_jurusan "
                + "WHERE k.id_kelas = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToClassroom(resultSet);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    // Simpan banyak kelas dalam satu transaksi (impor): kalau satu gagal, semuanya dibatalkan.
    // Kode kelas dibuat sama seperti saveOrUpdate: "K" + id 3 digit.
    @Override
    public int saveBatch(List<Classroom> classrooms) {
        String sqlInsert = "INSERT INTO mst_kelas(nama_kelas, tingkat, id_jurusan, diubah_oleh) VALUES (?, ?, ?, ?)";
        String sqlCode = "UPDATE mst_kelas SET kode_kelas = ? WHERE id_kelas = ?";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement updateCode = connection.prepareStatement(sqlCode)) {
                for (Classroom classroom : classrooms) {
                    insert.setString(1, classroom.getName());
                    insert.setString(2, classroom.getGrade());
                    if (classroom.getMajor() != null) {
                        insert.setInt(3, classroom.getMajor().getId());
                    } else {
                        insert.setNull(3, Types.INTEGER);
                    }
                    insert.setString(4, classroom.getUpdatedBy());
                    insert.executeUpdate();

                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (keys.next()) {
                            int newId = keys.getInt(1);
                            String code = "K" + String.format("%03d", newId);
                            updateCode.setString(1, code);
                            updateCode.setInt(2, newId);
                            updateCode.executeUpdate();
                            classroom.setId(newId);
                            classroom.setCode(code);
                        }
                    }
                }
                connection.commit();
                return classrooms.size();
            } catch (Exception exception) {
                connection.rollback();
                for (Classroom classroom : classrooms) {
                    classroom.setId(0);
                    classroom.setCode(null);
                }
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Impor kelas gagal, tidak ada data yang disimpan: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void saveOrUpdate(Classroom classroom) {
        String sqlInsert = "INSERT INTO mst_kelas(nama_kelas, tingkat, id_jurusan, diubah_oleh) VALUES (?, ?, ?, ?)";
        String sqlUpdate = "UPDATE mst_kelas SET nama_kelas = ?, tingkat = ?, id_jurusan = ?, diubah_oleh = ? WHERE id_kelas = ?";

        try (Connection connection = getConnection()) {
            if (classroom.getId() == 0) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setString(1, classroom.getName());
                    preparedStatement.setString(2, classroom.getGrade());

                    if (classroom.getMajor() != null) {
                        preparedStatement.setInt(3, classroom.getMajor().getId());
                    } else {
                        preparedStatement.setNull(3, Types.INTEGER);
                    }

                    preparedStatement.setString(4, classroom.getUpdatedBy());

                    preparedStatement.executeUpdate();

                    try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int newId = generatedKeys.getInt(1);
                            String generatedCode = "K" + String.format("%03d", newId);

                            try (PreparedStatement updateCode = connection.prepareStatement(
                                    "UPDATE mst_kelas SET kode_kelas = ? WHERE id_kelas = ?")) {
                                updateCode.setString(1, generatedCode);
                                updateCode.setInt(2, newId);
                                updateCode.executeUpdate();
                            }

                            classroom.setId(newId);
                            classroom.setCode(generatedCode);
                        }
                    }
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sqlUpdate)) {
                    preparedStatement.setString(1, classroom.getName());
                    preparedStatement.setString(2, classroom.getGrade());

                    if (classroom.getMajor() != null) {
                        preparedStatement.setInt(3, classroom.getMajor().getId());
                    } else {
                        preparedStatement.setNull(3, Types.INTEGER);
                    }

                    preparedStatement.setString(4, classroom.getUpdatedBy());
                    preparedStatement.setInt(5, classroom.getId());

                    preparedStatement.executeUpdate();
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal menyimpan data ke database: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM mst_kelas WHERE id_kelas = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal menghapus data: " + exception.getMessage(), exception);
        }
    }

    // Helper method untuk mapping ResultSet ke object Classroom (tanpa data Major)
    private Classroom mapBasicResultSetToClassroom(ResultSet resultSet) throws Exception {
        Classroom classroom = new Classroom();
        classroom.setId(resultSet.getInt("id_kelas"));
        classroom.setCode(resultSet.getString("kode_kelas"));
        classroom.setName(resultSet.getString("nama_kelas"));
        classroom.setGrade(resultSet.getString("tingkat"));

        Timestamp createdAt = resultSet.getTimestamp("tanggal_buat");
        if (createdAt != null) {
            classroom.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = resultSet.getTimestamp("tanggal_update");
        if (updatedAt != null) {
            classroom.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        classroom.setUpdatedBy(resultSet.getString("diubah_oleh"));

        return classroom;
    }

    // Helper method untuk mapping ResultSet ke object Classroom (lengkap dengan data Major)
    private Classroom mapResultSetToClassroom(ResultSet resultSet) throws Exception {
        Classroom classroom = mapBasicResultSetToClassroom(resultSet);

        int majorId = resultSet.getInt("id_jurusan");
        if (!resultSet.wasNull()) {
            Major major = new Major();
            major.setId(majorId);
            major.setCode(resultSet.getString("kode_jurusan"));
            major.setName(resultSet.getString("nama_jurusan"));
            major.setAbbreviation(resultSet.getString("singkatan"));
            classroom.setMajor(major);
        }

        return classroom;
    }
}