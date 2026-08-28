package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.ExamParticipantDAO;
import co.id.model.ExamParticipant;
import co.id.model.ExamSchedule;
import co.id.model.Student;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ExamParticipantDAOImpl extends DatabaseConfiguration implements ExamParticipantDAO {

    private static final String SELECT_JOIN =
        "SELECT ep.*, "
        + "ej.kode_ujian, ej.jenis_ujian, ej.tanggal, ej.jam_mulai, ej.jam_selesai, "
        + "s.nis, s.nama_siswa "
        + "FROM trx_peserta ep "
        + "LEFT JOIN trx_jadwal ej ON ep.id_ujian = ej.id_ujian "
        + "LEFT JOIN mst_siswa s ON ep.id_siswa = s.id_siswa ";

    @Override
    public List<ExamParticipant> getAllExamParticipants() {
        List<ExamParticipant> participants = new ArrayList<>();
        String sql = "SELECT * FROM trx_peserta";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                participants.add(mapBasicResultSetToExamParticipant(resultSet));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return participants;
    }

    @Override
    public List<ExamParticipant> getExamParticipantBy(String keyword) {
        List<ExamParticipant> participants = new ArrayList<>();
        String sql = SELECT_JOIN + "WHERE ep.kode_peserta LIKE ? OR s.nama_siswa LIKE ? OR s.nis LIKE ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            String param = "%" + keyword + "%";
            preparedStatement.setString(1, param);
            preparedStatement.setString(2, param);
            preparedStatement.setString(3, param);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    participants.add(mapResultSetToExamParticipant(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return participants;
    }

    @Override
    public List<ExamParticipant> getExamParticipants(int page, int size) {
        List<ExamParticipant> participants = new ArrayList<>();
        String sql = SELECT_JOIN + "ORDER BY ep.id_peserta LIMIT ? OFFSET ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, size);
            preparedStatement.setInt(2, (page - 1) * size);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    participants.add(mapResultSetToExamParticipant(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return participants;
    }

    @Override
    public List<ExamParticipant> getByExamSchedule(int examScheduleId) {
        List<ExamParticipant> participants = new ArrayList<>();
        String sql = SELECT_JOIN + "WHERE ep.id_ujian = ? ORDER BY s.nama_siswa";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, examScheduleId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    participants.add(mapResultSetToExamParticipant(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return participants;
    }

    @Override
    public List<ExamParticipant> getByStudent(int studentId) {
        List<ExamParticipant> participants = new ArrayList<>();
        String sql = SELECT_JOIN + "WHERE ep.id_siswa = ? ORDER BY ej.tanggal, ej.jam_mulai";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, studentId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    participants.add(mapResultSetToExamParticipant(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return participants;
    }

    @Override
    public int countExamParticipants() {
        String sql = "SELECT COUNT(*) AS total FROM trx_peserta";

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
        String sql = "SELECT COUNT(*) AS total FROM trx_peserta WHERE id_ujian = ?";

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
    public ExamParticipant getById(int id) {
        String sql = SELECT_JOIN + "WHERE ep.id_peserta = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToExamParticipant(resultSet);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public void saveOrUpdate(ExamParticipant examParticipant) {
        String sqlInsert = "INSERT INTO trx_peserta(id_ujian, id_siswa, no_peserta, no_kursi, diubah_oleh) VALUES (?, ?, ?, ?, ?)";
        String sqlUpdate = "UPDATE trx_peserta SET id_ujian = ?, id_siswa = ?, no_peserta = ?, no_kursi = ?, diubah_oleh = ? WHERE id_peserta = ?";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (examParticipant.getId() == 0) {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                        preparedStatement.setInt(1, examParticipant.getExamSchedule().getId());
                        preparedStatement.setInt(2, examParticipant.getStudent().getId());
                        preparedStatement.setString(3, examParticipant.getParticipantNumber());
                        preparedStatement.setString(4, examParticipant.getSeatNumber());
                        preparedStatement.setString(5, examParticipant.getUpdatedBy());

                        preparedStatement.executeUpdate();

                        try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                int newId = generatedKeys.getInt(1);
                                String generatedCode = "PST" + String.format("%04d", newId);

                                try (PreparedStatement updateCode = connection.prepareStatement(
                                        "UPDATE trx_peserta SET kode_peserta = ? WHERE id_peserta = ?")) {
                                    updateCode.setString(1, generatedCode);
                                    updateCode.setInt(2, newId);
                                    updateCode.executeUpdate();
                                }

                                examParticipant.setId(newId);
                                examParticipant.setCode(generatedCode);
                            }
                        }
                    }
                } else {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sqlUpdate)) {
                        preparedStatement.setInt(1, examParticipant.getExamSchedule().getId());
                        preparedStatement.setInt(2, examParticipant.getStudent().getId());
                        preparedStatement.setString(3, examParticipant.getParticipantNumber());
                        preparedStatement.setString(4, examParticipant.getSeatNumber());
                        preparedStatement.setString(5, examParticipant.getUpdatedBy());
                        preparedStatement.setInt(6, examParticipant.getId());

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
        String sql = "DELETE FROM trx_peserta WHERE id_peserta = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean isSeatTaken(int examScheduleId, String seatNumber, int excludeStudentId) {
        String sql = "SELECT COUNT(*) AS total FROM trx_peserta WHERE id_ujian = ? AND no_kursi = ? AND id_siswa <> ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, examScheduleId);
            preparedStatement.setString(2, seatNumber);
            preparedStatement.setInt(3, excludeStudentId);

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
    public boolean isParticipantNumberTaken(int examScheduleId, String participantNumber, int excludeStudentId) {
        String sql = "SELECT COUNT(*) AS total FROM trx_peserta WHERE id_ujian = ? AND no_peserta = ? AND id_siswa <> ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, examScheduleId);
            preparedStatement.setString(2, participantNumber);
            preparedStatement.setInt(3, excludeStudentId);

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
    public boolean isStudentRegistered(int examScheduleId, int studentId) {
        String sql = "SELECT COUNT(*) AS total FROM trx_peserta WHERE id_ujian = ? AND id_siswa = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, examScheduleId);
            preparedStatement.setInt(2, studentId);

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

    // Helper method untuk mapping ResultSet ke object ExamParticipant (tanpa data relasi)
    private ExamParticipant mapBasicResultSetToExamParticipant(ResultSet resultSet) throws Exception {
        ExamParticipant examParticipant = new ExamParticipant();
        examParticipant.setId(resultSet.getInt("id_peserta"));
        examParticipant.setCode(resultSet.getString("kode_peserta"));
        examParticipant.setParticipantNumber(resultSet.getString("no_peserta"));
        examParticipant.setSeatNumber(resultSet.getString("no_kursi"));

        Timestamp createdAt = resultSet.getTimestamp("tanggal_buat");
        if (createdAt != null) {
            examParticipant.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("tanggal_update");
        if (updatedAt != null) {
            examParticipant.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        examParticipant.setUpdatedBy(resultSet.getString("diubah_oleh"));

        return examParticipant;
    }

    // Helper method untuk mapping ResultSet ke object ExamParticipant (lengkap dengan data relasi)
    private ExamParticipant mapResultSetToExamParticipant(ResultSet resultSet) throws Exception {
        ExamParticipant examParticipant = mapBasicResultSetToExamParticipant(resultSet);

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

            examParticipant.setExamSchedule(examSchedule);
        }

        int studentId = resultSet.getInt("id_siswa");
        if (!resultSet.wasNull()) {
            Student student = new Student();
            student.setId(studentId);
            student.setNis(resultSet.getString("nis"));
            student.setName(resultSet.getString("nama_siswa"));
            examParticipant.setStudent(student);
        }

        return examParticipant;
    }
}