package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.SubjectDAO;
import co.id.model.Major;
import co.id.model.Subject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;


public class SubjectDAOImpl extends DatabaseConfiguration implements SubjectDAO{

    @Override
    public List<Subject> getAllSubjects() {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT * FROM mst_mapel";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                subjects.add(mapBasicResultSetToSubject(resultSet));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return subjects;
    }

    @Override
    public List<Subject> getSubjectBy(String keyword) {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT m.*, "
                + "j.id_jurusan, "
                + "j.kode_jurusan, "
                + "j.nama_jurusan, "
                + "j.singkatan "
                + "FROM mst_mapel m "
                + "LEFT JOIN mst_jurusan j ON m.id_jurusan = j.id_jurusan "
                + "WHERE m.nama_mapel LIKE ? OR m.kode_mapel LIKE ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, "%" + keyword + "%");
            preparedStatement.setString(2, "%" + keyword + "%");

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    subjects.add(mapResultSetToSubject(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return subjects;
    }

    @Override
    public List<Subject> getSubjects(int page, int size) {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT m.*, "
                + "j.id_jurusan, "
                + "j.kode_jurusan, "
                + "j.nama_jurusan, "
                + "j.singkatan "
                + "FROM mst_mapel m "
                + "LEFT JOIN mst_jurusan j ON m.id_jurusan = j.id_jurusan "
                + "ORDER BY m.id_mapel "
                + "LIMIT ? OFFSET ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, size);
            preparedStatement.setInt(2, (page - 1) * size);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    subjects.add(mapResultSetToSubject(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return subjects;
    }

    @Override
    public int countSubjects() {
        String sql = "SELECT COUNT(*) AS total FROM mst_mapel";

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
    public Subject getById(int id) {
        String sql = "SELECT m.*, "
                + "j.id_jurusan, "
                + "j.kode_jurusan, "
                + "j.nama_jurusan, "
                + "j.singkatan "
                + "FROM mst_mapel m "
                + "LEFT JOIN mst_jurusan j ON m.id_jurusan = j.id_jurusan "
                + "WHERE m.id_mapel = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToSubject(resultSet);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public void saveOrUpdate(Subject subject) {
        String sqlInsert = "INSERT INTO mst_mapel(nama_mapel, id_jurusan, tingkat, jenis, kkm, diubah_oleh) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlUpdate = "UPDATE mst_mapel SET nama_mapel = ?, id_jurusan = ?, tingkat = ?, jenis = ?, kkm = ?, diubah_oleh = ? WHERE id_mapel = ?";

        try (Connection connection = getConnection()) {
            if (subject.getId() == 0) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setString(1, subject.getName());

                    if (subject.getMajor() != null) {
                        preparedStatement.setInt(2, subject.getMajor().getId());
                    } else {
                        preparedStatement.setNull(2, Types.INTEGER);
                    }

                    preparedStatement.setString(3, subject.getGrade());
                    preparedStatement.setString(4, subject.getType());
                    preparedStatement.setInt(5, subject.getPassingGrade());
                    preparedStatement.setString(6, subject.getUpdatedBy());

                    preparedStatement.executeUpdate();

                    try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int newId = generatedKeys.getInt(1);
                            String generatedCode = "M" + String.format("%02d", newId);

                            try (PreparedStatement updateCode = connection.prepareStatement(
                                    "UPDATE mst_mapel SET kode_mapel = ? WHERE id_mapel = ?")) {
                                updateCode.setString(1, generatedCode);
                                updateCode.setInt(2, newId);
                                updateCode.executeUpdate();
                            }

                            subject.setId(newId);
                            subject.setCode(generatedCode);
                        }
                    }
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sqlUpdate)) {
                    preparedStatement.setString(1, subject.getName());

                    if (subject.getMajor() != null) {
                        preparedStatement.setInt(2, subject.getMajor().getId());
                    } else {
                        preparedStatement.setNull(2, Types.INTEGER);
                    }

                    preparedStatement.setString(3, subject.getGrade());
                    preparedStatement.setString(4, subject.getType());
                    preparedStatement.setInt(5, subject.getPassingGrade());
                    preparedStatement.setString(6, subject.getUpdatedBy());
                    preparedStatement.setInt(7, subject.getId());

                    preparedStatement.executeUpdate();
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM mst_mapel WHERE id_mapel = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    
    // Helper method untuk mapping ResultSet ke object Subject (tanpa data Major)
    private Subject mapBasicResultSetToSubject(ResultSet resultSet) throws Exception {
        Subject subject = new Subject();
        subject.setId(resultSet.getInt("id_mapel"));
        subject.setCode(resultSet.getString("kode_mapel"));
        subject.setName(resultSet.getString("nama_mapel"));
        subject.setGrade(resultSet.getString("tingkat"));
        subject.setType(resultSet.getString("jenis"));
        subject.setPassingGrade(resultSet.getInt("kkm"));

        Timestamp createdAt = resultSet.getTimestamp("tanggal_buat");
        if (createdAt != null) {
            subject.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = resultSet.getTimestamp("tanggal_update");
        if (updatedAt != null) {
            subject.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        subject.setUpdatedBy(resultSet.getString("diubah_oleh"));

        return subject;
    }

    // Helper method untuk mapping ResultSet ke object Subject (lengkap dengan data Major)
    private Subject mapResultSetToSubject(ResultSet resultSet) throws Exception {
        Subject subject = mapBasicResultSetToSubject(resultSet);

        int majorId = resultSet.getInt("id_jurusan");
        if (!resultSet.wasNull()) {
            Major major = new Major();
            major.setId(majorId);
            major.setCode(resultSet.getString("kode_jurusan"));
            major.setName(resultSet.getString("nama_jurusan"));
            major.setAbbreviation(resultSet.getString("singkatan"));
            subject.setMajor(major);
        }

        return subject;
    }
}