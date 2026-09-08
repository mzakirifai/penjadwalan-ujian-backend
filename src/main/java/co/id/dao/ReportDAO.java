package co.id.dao;

import co.id.model.ExamParticipant;
import co.id.model.ExamScore;
import co.id.model.Room;
import co.id.model.Student;
import co.id.model.Subject;
import co.id.model.Teacher;
import co.id.model.report.ClassScoreRecapItem;
import co.id.model.report.ClassroomReportItem;
import co.id.model.report.ExamScheduleReportItem;
import co.id.model.report.MajorReportItem;
import co.id.model.report.ParticipantCardReportItem;
import java.util.List;

public interface ReportDAO {
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
    public List<ExamParticipant> getAttendanceListReport(int examScheduleId);
}