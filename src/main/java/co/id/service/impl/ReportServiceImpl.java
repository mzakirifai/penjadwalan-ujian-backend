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

            // Satu ruangan bisa berisi beberapa kelas yang duduknya dicampur. Daftar hadir dibuat
            // satu lembar per kelas di ruangan itu (urut kelas, lalu nama siswa) supaya tidak membingungkan.
            java.util.Map<String, List<SeatAssignment>> byClass = new java.util.TreeMap<>();
            for (SeatAssignment seat : room.getSeats()) {
                byClass.computeIfAbsent(seat.getClassName() == null ? "-" : seat.getClassName(), k -> new ArrayList<>()).add(seat);
            }
            for (java.util.Map.Entry<String, List<SeatAssignment>> group : byClass.entrySet()) {
                List<SeatAssignment> seats = group.getValue();
                seats.sort(java.util.Comparator.comparing((SeatAssignment s) -> s.getStudent().getName() == null ? "" : s.getStudent().getName(),
                        String.CASE_INSENSITIVE_ORDER).thenComparing(SeatAssignment::getSeatIndex));
                co.id.model.ExamSchedule schedule = seats.get(0).getSchedule();
                String subject = schedule != null && schedule.getSubject() != null ? " - " + schedule.getSubjectLabel() : "";
                for (SeatAssignment seat : seats) {
                    RoomAttendanceReportItem item = new RoomAttendanceReportItem();
                    item.setRoomKey(room.getRoom().getId() + "|" + group.getKey());
                    item.setRoomName(room.getRoom().getName());
                    item.setClassSummary(group.getKey() + subject);
                    item.setSupervisorName(supervisorName);
                    item.setSupervisorNip(supervisorNip);
                    item.setTotalStudents(String.valueOf(seats.size()));
                    item.setSeatLabel(seat.getSeatLabel());
                    item.setParticipantNumber(seat.getParticipantNumber());
                    item.setNis(seat.getStudent().getNis());
                    item.setStudentName(seat.getStudent().getName());
                    item.setClassName(seat.getClassName());
                    items.add(item);
                }
            }
        }
        return items;
    }

    @Override
    public List<RoomAttendanceReportItem> getOnlineAttendanceReport(List<co.id.model.ExamSchedule> schedules, Integer scheduleId) {
        List<RoomAttendanceReportItem> items = new ArrayList<>();
        if (schedules == null || schedules.isEmpty()) {
            return items;
        }
        co.id.service.ExamParticipantService participantService = new co.id.service.impl.ExamParticipantServiceImpl();
        List<co.id.model.TeachingAssignment> assignments =
                new co.id.service.impl.TeachingAssignmentServiceImpl().getAllTeachingAssignments();

        List<co.id.model.ExamSchedule> sorted = new ArrayList<>(schedules);
        sorted.sort(java.util.Comparator.comparing((co.id.model.ExamSchedule s) -> s.getClassroom().getName())
                .thenComparing(s -> s.getSubject() != null ? s.getSubject().getName() : ""));
        for (co.id.model.ExamSchedule schedule : sorted) {
            if (scheduleId != null && schedule.getId() != scheduleId) {
                continue;
            }
            List<co.id.model.ExamParticipant> participants = new ArrayList<>(participantService.getByExamSchedule(schedule.getId()));
            participants.sort(java.util.Comparator.comparing(
                    (co.id.model.ExamParticipant p) -> p.getParticipantNumber() == null ? "" : p.getParticipantNumber())
                    .thenComparing(p -> p.getStudent().getName(), String.CASE_INSENSITIVE_ORDER));
            Teacher teacher = subjectTeacherOf(schedule, assignments);
            String classSummary = schedule.getClassroom().getName()
                    + (schedule.getSubject() != null ? " - " + schedule.getSubjectLabel() : "");
            for (co.id.model.ExamParticipant participant : participants) {
                RoomAttendanceReportItem item = new RoomAttendanceReportItem();
                item.setRoomKey("ONLINE-" + schedule.getId());
                item.setRoomName("Online");
                item.setClassSummary(classSummary);
                item.setSupervisorTitle("Guru Mapel");
                item.setSignerTitle("Guru Mata Pelajaran");
                item.setSupervisorName(teacher != null ? teacher.getName() : null);
                item.setSupervisorNip(teacher != null ? teacher.getNip() : null);
                item.setTotalStudents(String.valueOf(participants.size()));
                item.setSeatHeader("No Peserta");
                item.setSeatLabel(participant.getParticipantNumber());
                item.setParticipantNumber(participant.getParticipantNumber());
                item.setNis(participant.getStudent().getNis());
                item.setStudentName(participant.getStudent().getName());
                item.setClassName(schedule.getClassroom().getName());
                items.add(item);
            }
        }
        return items;
    }

    /** Guru pengampu mapel di kelas itu (sesuai kode guru jadwal); null bila tidak ada / tidak bisa dipastikan. */
    private static Teacher subjectTeacherOf(co.id.model.ExamSchedule schedule, List<co.id.model.TeachingAssignment> assignments) {
        if (schedule.getSubject() == null || schedule.getClassroom() == null) {
            return null;
        }
        List<co.id.model.TeachingAssignment> matches = new ArrayList<>();
        for (co.id.model.TeachingAssignment ta : assignments) {
            if (ta.getSubject() != null && ta.getClassroom() != null && ta.getTeacher() != null
                    && ta.getSubject().getId() == schedule.getSubject().getId()
                    && ta.getClassroom().getId() == schedule.getClassroom().getId()
                    && (schedule.getTeacherCode() == null || schedule.getTeacherCode().equals(ta.getTeacherCode()))) {
                matches.add(ta);
            }
        }
        return matches.size() == 1 ? matches.get(0).getTeacher() : null;
    }
}