package co.id.service;

import co.id.model.ExamScore;
import co.id.model.RoomPlan;
import co.id.model.Room;
import co.id.model.Student;
import co.id.model.Subject;
import co.id.model.Teacher;
import co.id.model.report.ClassScoreRecapItem;
import co.id.model.report.ClassroomReportItem;
import co.id.model.report.ExamScheduleReportItem;
import co.id.model.report.MajorReportItem;
import co.id.model.report.ParticipantCardReportItem;
import co.id.model.report.RoomAttendanceReportItem;
import java.util.List;

public interface ReportService {
    public List<MajorReportItem> getMajorReport();
    public List<Room> getRoomReport();
    public List<ClassroomReportItem> getClassroomReport();
    public List<Subject> getSubjectReport(int majorId);
    public List<Teacher> getTeacherReport();
    public List<Student> getStudentListReport(int classroomId);
    public List<ExamScheduleReportItem> getExamScheduleReport(String examType, 
            String semester, String academicYear);
    public List<ExamScheduleReportItem> getTeacherScheduleReport(int teacherId, 
            String examType, String semester, String academicYear);
    public List<ParticipantCardReportItem> getParticipantCardReport(int studentId, 
            String examType, String semester, String academicYear);
    public List<ExamScore> getExamResultReport(int examScheduleId);
    public List<ClassScoreRecapItem> getClassScoreRecapReport(int classroomId, 
            String examType, String semester, String academicYear);
    /**
     * Daftar hadir per ruangan dari penempatan yang sudah tersimpan untuk satu sesi.
     * roomId null = semua ruangan di sesi itu (tiap ruangan dicetak di halaman baru).
     */
    public List<RoomAttendanceReportItem> getRoomAttendanceReport(RoomPlan plan, Integer roomId);
}