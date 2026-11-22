package co.id.service.impl;

import co.id.dao.ReportDAO;
import co.id.dao.impl.ReportDAOImpl;
import co.id.model.ExamScore;
import co.id.model.Room;
import co.id.model.RoomAssignment;
import co.id.model.RoomPlan;
import co.id.model.SeatAssignment;
import co.id.model.Student;
import co.id.model.Subject;
import co.id.model.Teacher;
import co.id.model.report.ClassScoreRecapItem;
import co.id.model.report.ClassroomReportItem;
import co.id.model.report.ExamScheduleReportItem;
import co.id.model.report.MajorReportItem;
import co.id.model.report.ParticipantCardReportItem;
import co.id.model.report.RoomAttendanceReportItem;
import co.id.service.ReportService;
import java.util.ArrayList;
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

    @Override
    public List<ExamScore> getExamResultReport(int examScheduleId) {
        return reportDAO.getExamResultReport(examScheduleId);
    }

    @Override
    public List<ClassScoreRecapItem> getClassScoreRecapReport(int classroomId, String examType, String semester, String academicYear) {
        return reportDAO.getClassScoreRecapReport(classroomId, examType, semester, academicYear);
    }

    @Override
    public List<ExamScheduleReportItem> getTeacherScheduleReport(int teacherId, String examType, String semester, String academicYear) {
        return reportDAO.getTeacherScheduleReport(teacherId, examType, semester, academicYear);
    }

    @Override
    public List<RoomAttendanceReportItem> getRoomAttendanceReport(RoomPlan plan, Integer roomId) {
        List<RoomAttendanceReportItem> items = new ArrayList<>();
        if (plan == null) {
            return items;
        }
        for (RoomAssignment room : plan.getRooms()) {
            if (roomId != null && room.getRoom().getId() != roomId) {
                continue;
            }
            String supervisorName = room.getSupervisor() != null ? room.getSupervisor().getName() : null;
            String supervisorNip = room.getSupervisor() != null ? room.getSupervisor().getNip() : null;
            for (SeatAssignment seat : room.getSeats()) {
                RoomAttendanceReportItem item = new RoomAttendanceReportItem();
                item.setRoomKey(String.valueOf(room.getRoom().getId()));
                item.setRoomName(room.getRoom().getName());
                item.setClassSummary(room.describeClasses());
                item.setSupervisorName(supervisorName);
                item.setSupervisorNip(supervisorNip);
                item.setTotalStudents(String.valueOf(room.getFilled()));
                item.setSeatLabel(seat.getSeatLabel());
                item.setParticipantNumber(seat.getParticipantNumber());
                item.setNis(seat.getStudent().getNis());
                item.setStudentName(seat.getStudent().getName());
                item.setClassName(seat.getClassName());
                items.add(item);
            }
        }
        return items;
    }
}