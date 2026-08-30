package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.ExamScheduleDAO;
import co.id.model.Classroom;
import co.id.model.ExamSchedule;
import co.id.model.Room;
import co.id.model.Subject;
import co.id.model.Teacher;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ExamScheduleDAOImpl extends DatabaseConfiguration implements ExamScheduleDAO{

    private static final String SELECT_JOIN =
            "SELECT ej.*, "
            + "s.id_mapel, s.kode_mapel, s.nama_mapel, s.tingkat AS mapel_tingkat, s.jenis AS mapel_jenis, s.kkm, "
            + "k.id_kelas, k.kode_kelas, k.nama_kelas, k.tingkat AS kelas_tingkat, "
            + "r.id_ruangan, r.kode_ruangan, r.nama_ruangan, r.lantai, r.kapasitas, "
            + "g.id_guru, g.nip, g.nama_guru "
            + "FROM trx_jadwal ej "
            + "LEFT JOIN mst_mapel s ON ej.id_mapel = s.id_mapel "
            + "LEFT JOIN mst_kelas k ON ej.id_kelas = k.id_kelas "
            + "LEFT JOIN mst_ruangan r ON ej.id_ruangan = r.id_ruangan "
            + "LEFT JOIN mst_guru g ON ej.id_guru = g.id_guru ";
    
    @Override
    public List<ExamSchedule> getAllExamSchedules() {
        List<ExamSchedule> schedules = new ArrayList<>();
        String sql = "SELECT * FROM trx_jadwal";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                schedules.add(mapBasicResultSetToExamSchedule(resultSet));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return schedules;
    }
    
    @Override
    public List<ExamSchedule> getAllExamSchedulesDetailed() {
        List<ExamSchedule> schedules = new ArrayList<>();
        String sql = SELECT_JOIN + "ORDER BY ej.id_ujian";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                schedules.add(mapResultSetToExamSchedule(resultSet));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return schedules;
    }

    @Override
    public List<ExamSchedule> getExamScheduleBy(String keyword) {
        List<ExamSchedule> schedules = new ArrayList<>();
        String sql = SELECT_JOIN + "WHERE ej.kode_ujian LIKE ? OR s.nama_mapel LIKE ? OR k.nama_kelas LIKE ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            String param = "%" + keyword + "%";
            preparedStatement.setString(1, param);
            preparedStatement.setString(2, param);
            preparedStatement.setString(3, param);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    schedules.add(mapResultSetToExamSchedule(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return schedules;
    }

    @Override
    public List<ExamSchedule> getExamSchedules(int page, int size) {
        List<ExamSchedule> schedules = new ArrayList<>();
        String sql = SELECT_JOIN + "ORDER BY ej.id_ujian LIMIT ? OFFSET ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, size);
            preparedStatement.setInt(2, (page - 1) * size);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    schedules.add(mapResultSetToExamSchedule(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return schedules;
    }
    
    @Override
    public List<ExamSchedule> getTodayExamSchedules() {
        List<ExamSchedule> schedules = new ArrayList<>();
        String sql = SELECT_JOIN + "WHERE ej.tanggal = ? ORDER BY ej.jam_mulai";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, java.time.LocalDate.now());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    schedules.add(mapResultSetToExamSchedule(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return schedules;
    }

    @Override
    public int countExamSchedules() {
        String sql = "SELECT COUNT(*) AS total FROM trx_jadwal";

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
    public ExamSchedule getById(int id) {
        String sql = SELECT_JOIN + "WHERE ej.id_ujian = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToExamSchedule(resultSet);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public void saveOrUpdate(ExamSchedule examSchedule) {
        String sqlInsert = "INSERT INTO trx_jadwal(jenis_ujian, semester, tahun_akademik, tanggal, jam_mulai, jam_selesai, id_mapel, id_kelas, id_ruangan, id_guru, keterangan, diubah_oleh) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlUpdate = "UPDATE trx_jadwal SET jenis_ujian = ?, semester = ?, tahun_akademik = ?, tanggal = ?, jam_mulai = ?, jam_selesai = ?, "
                + "id_mapel = ?, id_kelas = ?, id_ruangan = ?, id_guru = ?, keterangan = ?, diubah_oleh = ? WHERE id_ujian = ?";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (examSchedule.getId() == 0) {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                        bindCommonFields(preparedStatement, examSchedule);
                        preparedStatement.executeUpdate();

                        try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                int newId = generatedKeys.getInt(1);
                                String generatedCode = "UJ" + String.format("%03d", newId);

                                try (PreparedStatement updateCode = connection.prepareStatement(
                                        "UPDATE trx_jadwal SET kode_ujian = ? WHERE id_ujian = ?")) {
                                    updateCode.setString(1, generatedCode);
                                    updateCode.setInt(2, newId);
                                    updateCode.executeUpdate();
                                }

                                examSchedule.setId(newId);
                                examSchedule.setCode(generatedCode);
                            }
                        }
                    }
                } else {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sqlUpdate)) {
                        bindCommonFields(preparedStatement, examSchedule);
                        preparedStatement.setInt(13, examSchedule.getId());
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
    
    // Bind field yang sama untuk INSERT & UPDATE (urutan parameter 1-12 identik di kedua query)
    private void bindCommonFields(PreparedStatement preparedStatement, ExamSchedule examSchedule) throws Exception {
        preparedStatement.setString(1, examSchedule.getExamType());
        preparedStatement.setString(2, examSchedule.getSemester());
        preparedStatement.setString(3, examSchedule.getAcademicYear());
        preparedStatement.setObject(4, examSchedule.getDate());
        preparedStatement.setObject(5, examSchedule.getStartTime());
        preparedStatement.setObject(6, examSchedule.getEndTime());

        if (examSchedule.getSubject() != null) {
            preparedStatement.setInt(7, examSchedule.getSubject().getId());
        } else {
            preparedStatement.setNull(7, Types.INTEGER);
        }

        if (examSchedule.getClassroom() != null) {
            preparedStatement.setInt(8, examSchedule.getClassroom().getId());
        } else {
            preparedStatement.setNull(8, Types.INTEGER);
        }

        if (examSchedule.getRoom() != null) {
            preparedStatement.setInt(9, examSchedule.getRoom().getId());
        } else {
            preparedStatement.setNull(9, Types.INTEGER);
        }

        if (examSchedule.getTeacher() != null) {
            preparedStatement.setInt(10, examSchedule.getTeacher().getId());
        } else {
            preparedStatement.setNull(10, Types.INTEGER);
        }

        preparedStatement.setString(11, examSchedule.getNotes());
        preparedStatement.setString(12, examSchedule.getUpdatedBy());
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM trx_jadwal WHERE id_ujian = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public boolean hasConflict(LocalDate date, LocalTime startTime, LocalTime endTime, int roomId, int classroomId, int teacherId, int excludeId) {
        // Bentrok kalau: tanggal sama, DAN (ruang ATAU kelas ATAU guru sama),
        // DAN rentang waktu saling overlap (mulai_baru < selesai_lama DAN selesai_baru > mulai_lama)
        String sql = "SELECT COUNT(*) AS total FROM trx_jadwal "
                + "WHERE tanggal = ? "
                + "AND (id_ruangan = ? OR id_kelas = ? OR id_guru = ?) "
                + "AND jam_mulai < ? AND jam_selesai > ? "
                + "AND id_ujian <> ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, date);
            preparedStatement.setInt(2, roomId);
            preparedStatement.setInt(3, classroomId);
            preparedStatement.setInt(4, teacherId);
            preparedStatement.setObject(5, endTime);
            preparedStatement.setObject(6, startTime);
            preparedStatement.setInt(7, excludeId);

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
    public boolean hasSameSubjectForClassroom(int classroomId, int subjectId, String examType,
            String semester, String academicYear, int excludeId) {
        String sql = "SELECT COUNT(*) AS total FROM trx_jadwal "
                + "WHERE id_kelas = ? AND id_mapel = ? AND jenis_ujian = ? "
                + "AND semester = ? AND tahun_akademik = ? AND id_ujian <> ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, classroomId);
            preparedStatement.setInt(2, subjectId);
            preparedStatement.setString(3, examType);
            preparedStatement.setString(4, semester);
            preparedStatement.setString(5, academicYear);
            preparedStatement.setInt(6, excludeId);

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
    
    // Helper method untuk mapping ResultSet ke object ExamSchedule (tanpa data relasi)
    private ExamSchedule mapBasicResultSetToExamSchedule(ResultSet resultSet) throws Exception {
        ExamSchedule examSchedule = new ExamSchedule();
        examSchedule.setId(resultSet.getInt("id_ujian"));
        examSchedule.setCode(resultSet.getString("kode_ujian"));
        examSchedule.setExamType(resultSet.getString("jenis_ujian"));
        examSchedule.setSemester(resultSet.getString("semester"));
        examSchedule.setAcademicYear(resultSet.getString("tahun_akademik"));

        Date date = resultSet.getDate("tanggal");
        if (date != null) {
            examSchedule.setDate(date.toLocalDate());
        }

        Time startTime = resultSet.getTime("jam_mulai");
        if (startTime != null) {
            examSchedule.setStartTime(startTime.toLocalTime());
        }

        Time endTime = resultSet.getTime("jam_selesai");
        if (endTime != null) {
            examSchedule.setEndTime(endTime.toLocalTime());
        }

        examSchedule.setNotes(resultSet.getString("keterangan"));

        Timestamp createdAt = resultSet.getTimestamp("tanggal_buat");
        if (createdAt != null) {
            examSchedule.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("tanggal_update");
        if (updatedAt != null) {
            examSchedule.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        examSchedule.setUpdatedBy(resultSet.getString("diubah_oleh"));

        return examSchedule;
    }
    
    // Helper method untuk mapping ResultSet ke object ExamSchedule (lengkap dengan data relasi)
    private ExamSchedule mapResultSetToExamSchedule(ResultSet resultSet) throws Exception {
        ExamSchedule examSchedule = mapBasicResultSetToExamSchedule(resultSet);

        int subjectId = resultSet.getInt("id_mapel");
        if (!resultSet.wasNull()) {
            Subject subject = new Subject();
            subject.setId(subjectId);
            subject.setCode(resultSet.getString("kode_mapel"));
            subject.setName(resultSet.getString("nama_mapel"));
            subject.setGrade(resultSet.getString("mapel_tingkat"));
            subject.setType(resultSet.getString("mapel_jenis"));
            subject.setPassingGrade(resultSet.getInt("kkm"));
            examSchedule.setSubject(subject);
        }

        int classroomId = resultSet.getInt("id_kelas");
        if (!resultSet.wasNull()) {
            Classroom classroom = new Classroom();
            classroom.setId(classroomId);
            classroom.setCode(resultSet.getString("kode_kelas"));
            classroom.setName(resultSet.getString("nama_kelas"));
            classroom.setGrade(resultSet.getString("kelas_tingkat"));
            examSchedule.setClassroom(classroom);
        }

        int roomId = resultSet.getInt("id_ruangan");
        if (!resultSet.wasNull()) {
            Room room = new Room();
            room.setId(roomId);
            room.setCode(resultSet.getString("kode_ruangan"));
            room.setName(resultSet.getString("nama_ruangan"));
            room.setFloor(resultSet.getInt("lantai"));
            room.setCapacity(resultSet.getInt("kapasitas"));
            examSchedule.setRoom(room);
        }

        int teacherId = resultSet.getInt("id_guru");
        if (!resultSet.wasNull()) {
            Teacher teacher = new Teacher();
            teacher.setId(teacherId);
            teacher.setNip(resultSet.getString("nip"));
            teacher.setName(resultSet.getString("nama_guru"));
            examSchedule.setTeacher(teacher);
        }

        return examSchedule;
    }
}
