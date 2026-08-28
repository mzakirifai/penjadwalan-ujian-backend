package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.ExamScoreDAO;
import co.id.model.ExamSchedule;
import co.id.model.ExamScore;
import co.id.model.Student;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ExamScoreDAOImpl extends DatabaseConfiguration implements ExamScoreDAO {

    private static final String SELECT_JOIN =
        "SELECT es.*, "
        + "ej.kode_ujian, ej.jenis_ujian, ej.tanggal, ej.jam_mulai, ej.jam_selesai, "
        + "s.nis, s.nama_siswa "
        + "FROM trx_nilai es "
        + "LEFT JOIN trx_jadwal ej ON es.id_ujian = ej.id_ujian "
        + "LEFT JOIN mst_siswa s ON es.id_siswa = s.id_siswa ";

    @Override
    public List<ExamScore> getAllExamScores() {
        List<ExamScore> scores = new ArrayList<>();
        String sql = "SELECT * FROM trx_nilai";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                scores.add(mapBasicResultSetToExamScore(resultSet));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return scores;
    }

    @Override
    public List<ExamScore> getExamScoreBy(String keyword) {
        List<ExamScore> scores = new ArrayList<>();
        String sql = SELECT_JOIN + "WHERE es.kode_nilai LIKE ? OR s.nama_siswa LIKE ? OR s.nis LIKE ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            String param = "%" + keyword + "%";
            preparedStatement.setString(1, param);
            preparedStatement.setString(2, param);
            preparedStatement.setString(3, param);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    scores.add(mapResultSetToExamScore(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return scores;
    }

    @Override
    public List<ExamScore> getExamScores(int page, int size) {
        List<ExamScore> scores = new ArrayList<>();
        String sql = SELECT_JOIN + "ORDER BY es.id_nilai LIMIT ? OFFSET ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, size);
            preparedStatement.setInt(2, (page - 1) * size);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    scores.add(mapResultSetToExamScore(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return scores;
    }

    @Override
    public List<ExamScore> getByExamSchedule(int examScheduleId) {
        List<ExamScore> scores = new ArrayList<>();
        String sql = SELECT_JOIN + "WHERE es.id_ujian = ? ORDER BY s.nama_siswa";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, examScheduleId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    scores.add(mapResultSetToExamScore(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return scores;
    }

    @Override
    public List<ExamScore> getByStudent(int studentId) {
        List<ExamScore> scores = new ArrayList<>();
        String sql = SELECT_JOIN + "WHERE es.id_siswa = ? ORDER BY ej.tanggal, ej.jam_mulai";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, studentId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    scores.add(mapResultSetToExamScore(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return scores;
    }

    @Override
    public int countExamScores() {
        String sql = "SELECT COUNT(*) AS total FROM trx_nilai";

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
    public int countByExamSchedule(int examScheduleId) {
        String sql = "SELECT COUNT(*) AS total FROM trx_nilai WHERE id_ujian = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, examScheduleId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total");
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return 0;
    }

    @Override
    public ExamScore getById(int id) {
        String sql = SELECT_JOIN + "WHERE es.id_nilai = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToExamScore(resultSet);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public void saveOrUpdate(ExamScore examScore) {
        String sqlInsert = "INSERT INTO trx_nilai(id_ujian, id_siswa, nilai, diubah_oleh) VALUES (?, ?, ?, ?)";
        String sqlUpdate = "UPDATE trx_nilai SET id_ujian = ?, id_siswa = ?, nilai = ?, diubah_oleh = ? WHERE id_nilai = ?";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (examScore.getId() == 0) {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                        preparedStatement.setInt(1, examScore.getExamSchedule().getId());
                        preparedStatement.setInt(2, examScore.getStudent().getId());
                        preparedStatement.setInt(3, examScore.getScore());
                        preparedStatement.setString(4, examScore.getUpdatedBy());

                        preparedStatement.executeUpdate();

                        try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                int newId = generatedKeys.getInt(1);
                                String generatedCode = "NLI" + String.format("%04d", newId);

                                try (PreparedStatement updateCode = connection.prepareStatement(
                                        "UPDATE trx_nilai SET kode_nilai = ? WHERE id_nilai = ?")) {
                                    updateCode.setString(1, generatedCode);
                                    updateCode.setInt(2, newId);
                                    updateCode.executeUpdate();
                                }

                                examScore.setId(newId);
                                examScore.setCode(generatedCode);
                            }
                        }
                    }
                } else {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sqlUpdate)) {
                        preparedStatement.setInt(1, examScore.getExamSchedule().getId());
                        preparedStatement.setInt(2, examScore.getStudent().getId());
                        preparedStatement.setInt(3, examScore.getScore());
                        preparedStatement.setString(4, examScore.getUpdatedBy());
                        preparedStatement.setInt(5, examScore.getId());

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
    public void delete(int id) {
        String sql = "DELETE FROM trx_nilai WHERE id_nilai = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean hasScore(int examScheduleId, int studentId, int excludeId) {
        String sql = "SELECT COUNT(*) AS total FROM trx_nilai WHERE id_ujian = ? AND id_siswa = ? AND id_nilai <> ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, examScheduleId);
            preparedStatement.setInt(2, studentId);
            preparedStatement.setInt(3, excludeId);

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

    // Helper method untuk mapping ResultSet ke object ExamScore (tanpa data relasi)
    private ExamScore mapBasicResultSetToExamScore(ResultSet resultSet) throws Exception {
        ExamScore examScore = new ExamScore();
        examScore.setId(resultSet.getInt("id_nilai"));
        examScore.setCode(resultSet.getString("kode_nilai"));
        examScore.setScore(resultSet.getInt("nilai"));

        Timestamp createdAt = resultSet.getTimestamp("tanggal_buat");
        if (createdAt != null) {
            examScore.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("tanggal_update");
        if (updatedAt != null) {
            examScore.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        examScore.setUpdatedBy(resultSet.getString("diubah_oleh"));

        return examScore;
    }

    // Helper method untuk mapping ResultSet ke object ExamScore (lengkap dengan data relasi)
    private ExamScore mapResultSetToExamScore(ResultSet resultSet) throws Exception {
        ExamScore examScore = mapBasicResultSetToExamScore(resultSet);

        int examScheduleId = resultSet.getInt("id_ujian");
        if (!resultSet.wasNull()) {
            ExamSchedule examSchedule = new ExamSchedule();
            examSchedule.setId(examScheduleId);
            examSchedule.setCode(resultSet.getString("kode_ujian"));
            examSchedule.setExamType(resultSet.getString("jenis_ujian"));

            if (resultSet.getDate("tanggal") != null) {
                examSchedule.setDate(resultSet.getDate("tanggal").toLocalDate());
            }
            if (resultSet.getTime("jam_mulai") != null) {
                examSchedule.setStartTime(resultSet.getTime("jam_mulai").toLocalTime());
            }
            if (resultSet.getTime("jam_selesai") != null) {
                examSchedule.setEndTime(resultSet.getTime("jam_selesai").toLocalTime());
            }

            examScore.setExamSchedule(examSchedule);
        }

        int studentId = resultSet.getInt("id_siswa");
        if (!resultSet.wasNull()) {
            Student student = new Student();
            student.setId(studentId);
            student.setNis(resultSet.getString("nis"));
            student.setName(resultSet.getString("nama_siswa"));
            examScore.setStudent(student);
        }

        return examScore;
    }
}