package co.id.service.impl;

import co.id.dao.ReportDAO;
import co.id.dao.impl.ReportDAOImpl;
import co.id.model.Room;
import co.id.model.Student;
import co.id.model.Subject;
import co.id.model.Teacher;
import co.id.model.report.ClassroomReportItem;
import co.id.model.report.ExamScheduleReportItem;
import co.id.model.report.MajorReportItem;
import co.id.model.report.ParticipantCardReportItem;
import co.id.service.ReportService;
import java.util.List;

public class ReportServiceImpl implements ReportService {
    private final ReportDAO reportDAO;

    public ReportServiceImpl() {
        reportDAO = new ReportDAOImpl();
    }

    @Override
    public List<MajorReportItem> getMajorReport() {
        return reportDAO.getMajorReport();
    }

    @Override
    public List<Room> getRoomReport() {
        return reportDAO.getRoomReport();
    }

    @Override
    public List<ClassroomReportItem> getClassroomReport() {
        return reportDAO.getClassroomReport();
    }

    @Override
    public List<Subject> getSubjectReport(int majorId) {
        return reportDAO.getSubjectReport(majorId);
    }

    @Override
    public List<Teacher> getTeacherReport() {
        return reportDAO.getTeacherReport();
    }

    @Override
    public List<Student> getStudentListReport(int classroomId) {
        return reportDAO.getStudentListReport(classroomId);
    }
    
    @Override
    public List<ExamScheduleReportItem> getExamScheduleReport(String examType, String semester, String academicYear) {
        return reportDAO.getExamScheduleReport(examType, semester, academicYear);
    }
    
    @Override
    public List<ParticipantCardReportItem> getParticipantCardReport(int studentId, String examType, String semester, String academicYear) {
        return reportDAO.getParticipantCardReport(studentId, examType, semester, academicYear);
    }
}