package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.ExamParticipantDAO;
import co.id.dao.ExamScheduleDAO;
import co.id.dao.ExamScoreDAO;
import co.id.dao.ReportDAO;
import co.id.dao.RoomDAO;
import co.id.dao.StudentDAO;
import co.id.dao.TeacherDAO;
import co.id.model.ExamParticipant;
import co.id.model.ExamSchedule;
import co.id.model.ExamScore;
import co.id.model.Major;
import co.id.model.Room;
import co.id.model.Student;
import co.id.model.Subject;
import co.id.model.Teacher;
import co.id.model.report.ClassScoreRecapItem;
import co.id.model.report.ClassroomReportItem;
import co.id.model.report.ExamScheduleReportItem;
import co.id.model.report.MajorReportItem;
import co.id.model.report.ParticipantCardReportItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReportDAOImpl extends DatabaseConfiguration implements ReportDAO {
    private final RoomDAO roomDAO;
    private final TeacherDAO teacherDAO;
    private final StudentDAO studentDAO;
    private final ExamScheduleDAO examScheduleDAO;
    private final ExamParticipantDAO examParticipantDAO;
    private final ExamScoreDAO examScoreDAO;

    public ReportDAOImpl() {
        roomDAO = new RoomDAOImpl();
        teacherDAO = new TeacherDAOImpl();
        studentDAO = new StudentDAOImpl();
        examScheduleDAO = new ExamScheduleDAOImpl();
        examParticipantDAO = new ExamParticipantDAOImpl();
        examScoreDAO = new ExamScoreDAOImpl();
        
    }

    @Override
    public List<MajorReportItem> getMajorReport() {
        List<MajorReportItem> items = new ArrayList<>();

        String sql = "SELECT j.kode_jurusan, j.nama_jurusan, j.singkatan, "
                + "COUNT(DISTINCT k.id_kelas) AS jumlah_kelas, "
                + "COUNT(s.id_siswa) AS jumlah_siswa "
                + "FROM mst_jurusan j "
                + "LEFT JOIN mst_kelas k ON k.id_jurusan = j.id_jurusan "
                + "LEFT JOIN mst_siswa s ON s.id_kelas = k.id_kelas "
                + "GROUP BY j.id_jurusan, j.kode_jurusan, j.nama_jurusan, j.singkatan "
                + "ORDER BY j.kode_jurusan";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                MajorReportItem item = new MajorReportItem();
                item.setCode(resultSet.getString("kode_jurusan"));
                item.setName(resultSet.getString("nama_jurusan"));
                item.setAbbreviation(resultSet.getString("singkatan"));
                item.setClassCount(resultSet.getInt("jumlah_kelas"));
                item.setStudentCount(resultSet.getInt("jumlah_siswa"));
                items.add(item);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return items;
    }

    @Override
    public List<Room> getRoomReport() {
        return roomDAO.getAllRooms();
    }

    @Override
    public List<ClassroomReportItem> getClassroomReport() {
        List<ClassroomReportItem> items = new ArrayList<>();

        String sql = "SELECT k.kode_kelas, k.nama_kelas, k.tingkat, "
                + "COUNT(s.id_siswa) AS jumlah_siswa, "
                + "j.id_jurusan, j.kode_jurusan, j.nama_jurusan, j.singkatan "
                + "FROM mst_kelas k "
                + "LEFT JOIN mst_jurusan j ON k.id_jurusan = j.id_jurusan "
                + "LEFT JOIN mst_siswa s ON s.id_kelas = k.id_kelas "
                + "GROUP BY k.id_kelas, k.kode_kelas, k.nama_kelas, k.tingkat, "
                + "j.id_jurusan, j.kode_jurusan, j.nama_jurusan, j.singkatan "
                + "ORDER BY k.kode_kelas";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                ClassroomReportItem item = new ClassroomReportItem();
                item.setCode(resultSet.getString("kode_kelas"));
                item.setName(resultSet.getString("nama_kelas"));
                item.setGrade(resultSet.getString("tingkat"));
                item.setStudentCount(resultSet.getInt("jumlah_siswa"));

                int majorId = resultSet.getInt("id_jurusan");
                if (!resultSet.wasNull()) {
                    Major major = new Major();
                    major.setId(majorId);
                    major.setCode(resultSet.getString("kode_jurusan"));
                    major.setName(resultSet.getString("nama_jurusan"));
                    major.setAbbreviation(resultSet.getString("singkatan"));
                    item.setMajor(major);
                }

                items.add(item);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return items;
    }

    @Override
    public List<Subject> getSubjectReport(int majorId) {
        List<Subject> subjects = new ArrayList<>();

        String sql = "SELECT m.kode_mapel, m.nama_mapel, m.tingkat, m.jenis, m.kkm "
                + "FROM mst_mapel m "
                + "WHERE m.id_jurusan = ? "
                + "ORDER BY m.kode_mapel";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, majorId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Subject subject = new Subject();
                    subject.setCode(resultSet.getString("kode_mapel"));
                    subject.setName(resultSet.getString("nama_mapel"));
                    subject.setGrade(resultSet.getString("tingkat"));
                    subject.setType(resultSet.getString("jenis"));
                    subject.setPassingGrade(resultSet.getInt("kkm"));
                    subjects.add(subject);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return subjects;
    }

    @Override
    public List<Teacher> getTeacherReport() {
        return teacherDAO.getAllTeachers();
    }

    @Override
    public List<Student> getStudentListReport(int classroomId) {
        return studentDAO.getByClassroom(classroomId);
    }

    @Override
    public List<ExamScheduleReportItem> getExamScheduleReport(String examType, String semester, String academicYear) {
        List<ExamSchedule> schedules = examScheduleDAO.getByPeriod(examType, semester, academicYear);
        List<ExamScheduleReportItem> items = new ArrayList<>();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        for (ExamSchedule schedule : schedules) {
            ExamScheduleReportItem item = new ExamScheduleReportItem();

            item.setDate(schedule.getDate() != null ? schedule.getDate().format(dateFormatter) : "-");
            item.setStartTime(schedule.getStartTime() != null ? schedule.getStartTime().format(timeFormatter) : "-");
            item.setEndTime(schedule.getEndTime() != null ? schedule.getEndTime().format(timeFormatter) : "-");
            item.setSubjectName(schedule.getSubject() != null ? schedule.getSubject().getName() : "-");
            item.setClassroomName(schedule.getClassroom() != null ? schedule.getClassroom().getName() : "-");
            item.setRoomName(schedule.getRoom() != null ? schedule.getRoom().getName() : "-");
            item.setTeacherName(schedule.getTeacher() != null ? schedule.getTeacher().getName() : "-");

            items.add(item);
        }

        return items;
    }
    
    @Override
    public List<ExamScheduleReportItem> getTeacherScheduleReport(int teacherId, String examType, String semester, String academicYear) {
        List<ExamSchedule> schedules = examScheduleDAO.getByTeacherAndPeriod(teacherId, examType, semester, academicYear);
        return mapToExamScheduleReportItems(schedules);
    }
 
    private List<ExamScheduleReportItem> mapToExamScheduleReportItems(List<ExamSchedule> schedules) {
        List<ExamScheduleReportItem> items = new ArrayList<>();
 
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
 
        for (ExamSchedule schedule : schedules) {
            ExamScheduleReportItem item = new ExamScheduleReportItem();
 
            item.setDate(schedule.getDate() != null ? schedule.getDate().format(dateFormatter) : "-");
            item.setStartTime(schedule.getStartTime() != null ? schedule.getStartTime().format(timeFormatter) : "-");
            item.setEndTime(schedule.getEndTime() != null ? schedule.getEndTime().format(timeFormatter) : "-");
            item.setSubjectName(schedule.getSubject() != null ? schedule.getSubject().getName() : "-");
            item.setClassroomName(schedule.getClassroom() != null ? schedule.getClassroom().getName() : "-");
            item.setRoomName(schedule.getRoom() != null ? schedule.getRoom().getName() : "-");
            item.setTeacherName(schedule.getTeacher() != null ? schedule.getTeacher().getName() : "-");
 
            items.add(item);
        }
 
        return items;
    }

    @Override
    public List<ParticipantCardReportItem> getParticipantCardReport(int studentId, String examType, String semester, String academicYear) {
        List<ParticipantCardReportItem> items = new ArrayList<>();
 
        String sql = "SELECT ep.no_peserta, ep.no_kursi, "
                + "ej.tanggal, ej.jam_mulai, ej.jam_selesai, "
                + "m.nama_mapel, r.nama_ruangan "
                + "FROM trx_peserta ep "
                + "JOIN trx_jadwal ej ON ep.id_ujian = ej.id_ujian "
                + "LEFT JOIN mst_mapel m ON ej.id_mapel = m.id_mapel "
                + "LEFT JOIN mst_ruangan r ON ej.id_ruangan = r.id_ruangan "
                + "WHERE ep.id_siswa = ? AND ej.jenis_ujian = ? AND ej.semester = ? AND ej.tahun_akademik = ? "
                + "ORDER BY ej.tanggal, ej.jam_mulai";
 
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
 
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
 
            preparedStatement.setInt(1, studentId);
            preparedStatement.setString(2, examType);
            preparedStatement.setString(3, semester);
            preparedStatement.setString(4, academicYear);
 
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    ParticipantCardReportItem item = new ParticipantCardReportItem();
 
                    item.setParticipantNumber(resultSet.getString("no_peserta"));
                    item.setSeatNumber(resultSet.getString("no_kursi"));
                    item.setSubjectName(resultSet.getString("nama_mapel"));
                    item.setRoomName(resultSet.getString("nama_ruangan"));
 
                    java.sql.Date date = resultSet.getDate("tanggal");
                    item.setDate(date != null ? date.toLocalDate().format(dateFormatter) : "-");
 
                    java.sql.Time startTime = resultSet.getTime("jam_mulai");
                    item.setStartTime(startTime != null ? startTime.toLocalTime().format(timeFormatter) : "-");
 
                    java.sql.Time endTime = resultSet.getTime("jam_selesai");
                    item.setEndTime(endTime != null ? endTime.toLocalTime().format(timeFormatter) : "-");
 
                    items.add(item);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
 
        return items;
    }
    
    @Override
    public List<ExamScore> getExamResultReport(int examScheduleId) {
        return examScoreDAO.getByExamSchedule(examScheduleId);
    }

    @Override
    public List<ClassScoreRecapItem> getClassScoreRecapReport(int classroomId, String examType, String semester, String academicYear) {
        List<ClassScoreRecapItem> items = new ArrayList<>();
 
        String sql = "SELECT s.nis, s.nama_siswa, m.nama_mapel, es.nilai "
                + "FROM trx_nilai es "
                + "JOIN trx_jadwal ej ON es.id_ujian = ej.id_ujian "
                + "JOIN mst_siswa s ON es.id_siswa = s.id_siswa "
                + "JOIN mst_mapel m ON ej.id_mapel = m.id_mapel "
                + "WHERE ej.id_kelas = ? AND ej.jenis_ujian = ? AND ej.semester = ? AND ej.tahun_akademik = ? "
                + "ORDER BY s.nama_siswa, m.nama_mapel";
 
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
 
            preparedStatement.setInt(1, classroomId);
            preparedStatement.setString(2, examType);
            preparedStatement.setString(3, semester);
            preparedStatement.setString(4, academicYear);
 
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    ClassScoreRecapItem item = new ClassScoreRecapItem();
                    item.setNis(resultSet.getString("nis"));
                    item.setStudentName(resultSet.getString("nama_siswa"));
                    item.setSubjectName(resultSet.getString("nama_mapel"));
                    item.setScore(resultSet.getInt("nilai"));
                    items.add(item);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
 
        return items;
    }
    
    @Override
    public List<ExamParticipant> getAttendanceListReport(int examScheduleId) {
        return examParticipantDAO.getByExamSchedule(examScheduleId);
    }
}