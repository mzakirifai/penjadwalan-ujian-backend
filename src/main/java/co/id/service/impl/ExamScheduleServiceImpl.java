package co.id.service.impl;

import co.id.dao.ExamScheduleDAO;
import co.id.dao.impl.ExamScheduleDAOImpl;
import co.id.model.ExamSchedule;
import co.id.model.RoomSuggestion;
import co.id.model.ScheduleConflict;
import co.id.service.ScheduleConflictException;
import co.id.service.ExamScheduleService;
import java.util.List;

public class ExamScheduleServiceImpl implements ExamScheduleService {

    private final ExamScheduleDAO examScheduleDAO = new ExamScheduleDAOImpl();
    private final co.id.dao.StudentDAO studentDAO = new co.id.dao.impl.StudentDAOImpl();

    @Override
    public List<ExamSchedule> getAllExamSchedules() {
        return examScheduleDAO.getAllExamSchedules();
    }
    
    @Override
    public List<ExamSchedule> getAllExamSchedulesDetailed() {
        return examScheduleDAO.getAllExamSchedulesDetailed();
    }

    @Override
    public List<ExamSchedule> getExamScheduleBy(String keyword) {
        return examScheduleDAO.getExamScheduleBy(keyword);
    }

    @Override
    public List<ExamSchedule> getExamSchedules(int page, int size) {
        return examScheduleDAO.getExamSchedules(page, size);
    }
    
    @Override
    public List<ExamSchedule> getTodayExamSchedules() {
        return examScheduleDAO.getTodayExamSchedules();
    }

    @Override
    public int countExamSchedules() {
        return examScheduleDAO.countExamSchedules();
    }

    @Override
    public ExamSchedule getById(int id) {
        return examScheduleDAO.getById(id);
    }
    
    @Override
    public List<ExamSchedule> getByPeriod(String examType, String semester, String academicYear) {
        return examScheduleDAO.getByPeriod(examType, semester, academicYear);
    }
    
    @Override
    public List<ExamSchedule> getByTeacherAndPeriod(int teacherId, String examType, String semester, String academicYear) {
        return examScheduleDAO.getByTeacherAndPeriod(teacherId, examType, semester, academicYear);
    }

    @Override
    public java.util.Map<java.time.LocalDate, Integer> getExamCountPerDay(java.time.LocalDate start, java.time.LocalDate end) {
        return examScheduleDAO.getExamCountPerDay(start, end);
    }
    
    @Override
    public void save(ExamSchedule examSchedule, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        validate(examSchedule);

        int excludeId = examSchedule.getId(); // 0 kalau data baru, sehingga tidak bentrok dengan dirinya sendiri
        List<ScheduleConflict> conflicts = findConflicts(
                examSchedule.getDate(),
                examSchedule.getStartTime(),
                examSchedule.getEndTime(),
                examSchedule.getRoom() != null ? examSchedule.getRoom().getId() : 0,
                examSchedule.getClassroom().getId(),
                examSchedule.getTeacher() != null ? examSchedule.getTeacher().getId() : 0,
                excludeId
        );

        if (!conflicts.isEmpty()) {
            boolean hasRoomConflict = conflicts.stream().anyMatch(c -> c.getResource() == ScheduleConflict.Resource.RUANGAN);
            boolean roomOnly = conflicts.stream().allMatch(c -> c.getResource() == ScheduleConflict.Resource.RUANGAN);
            List<RoomSuggestion> suggestions = hasRoomConflict
                    ? suggestRooms(examSchedule.getDate(), examSchedule.getStartTime(), examSchedule.getEndTime(),
                            excludeId, examSchedule.getClassroom().getId())
                    : List.of();

            StringBuilder message = new StringBuilder("Jadwal bentrok:");
            for (ScheduleConflict conflict : conflicts) {
                message.append("\n- ").append(conflict.describe());
            }
            throw new ScheduleConflictException(message.toString(), conflicts, suggestions, roomOnly);
        }

        examSchedule.setUpdatedBy(currentUsername);
        examScheduleDAO.saveOrUpdate(examSchedule);
    }

    @Override
    public List<ScheduleConflict> findConflicts(java.time.LocalDate date, java.time.LocalTime startTime,
            java.time.LocalTime endTime, int roomId, int classroomId, int teacherId, int excludeId) {
        List<ScheduleConflict> result = new java.util.ArrayList<>();
        for (ExamSchedule other : examScheduleDAO.findOverlapping(date, startTime, endTime, roomId, classroomId,
                teacherId, excludeId)) {
            if (other.getRoom() != null && other.getRoom().getId() == roomId) {
                result.add(new ScheduleConflict(ScheduleConflict.Resource.RUANGAN, other));
            }
            if (other.getClassroom() != null && other.getClassroom().getId() == classroomId) {
                result.add(new ScheduleConflict(ScheduleConflict.Resource.KELAS, other));
            }
            if (other.getTeacher() != null && other.getTeacher().getId() == teacherId) {
                result.add(new ScheduleConflict(ScheduleConflict.Resource.PENGAWAS, other));
            }
        }
        return result;
    }

    @Override
    public List<RoomSuggestion> suggestRooms(java.time.LocalDate date, java.time.LocalTime startTime,
            java.time.LocalTime endTime, int excludeId, Integer classroomId) {
        int students = classroomId == null ? 0 : studentDAO.getByClassroom(classroomId).size();

        List<RoomSuggestion> suggestions = new java.util.ArrayList<>();
        for (co.id.model.Room room : examScheduleDAO.getAvailableRooms(date, startTime, endTime, excludeId)) {
            suggestions.add(new RoomSuggestion(room, students));
        }

        // Yang muat didahulukan, mulai dari kapasitas terkecil yang cukup (hemat ruangan besar);
        // yang tidak muat di belakang, dari yang terbesar.
        suggestions.sort(java.util.Comparator
                .comparing((RoomSuggestion s) -> !s.isEnoughCapacity())
                .thenComparingInt(s -> s.isEnoughCapacity() ? s.getRoom().getCapacity() : -s.getRoom().getCapacity())
                .thenComparing(s -> s.getRoom().getName()));
        return suggestions;
    }

    @Override
    public void delete(int id, String currentUserRole) {
        ensureIsAdmin(currentUserRole);

        ExamSchedule existing = examScheduleDAO.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Data jadwal ujian tidak ditemukan.");
        }

        examScheduleDAO.delete(id);
    }
    
    @Override
    public boolean hasSameSubjectForClassroom(int classroomId, int subjectId, Integer teacherCode, String examType,
            String semester, String academicYear, int excludeId) {
        return examScheduleDAO.hasSameSubjectForClassroom(classroomId, subjectId, teacherCode, examType, semester, academicYear, excludeId);
    }

    private void ensureIsAdmin(String currentUserRole) {
        if (!"admin".equalsIgnoreCase(currentUserRole)) {
            throw new SecurityException("Hanya admin yang boleh mengubah data jadwal ujian.");
        }
    }

    private void validate(ExamSchedule examSchedule) {
        if (examSchedule.getExamType() == null || examSchedule.getExamType().isBlank()) {
            throw new IllegalArgumentException("Jenis ujian wajib diisi.");
        }
        if (examSchedule.getSemester() == null || examSchedule.getSemester().isBlank()) {
            throw new IllegalArgumentException("Semester wajib diisi.");
        }
        if (examSchedule.getAcademicYear() == null || examSchedule.getAcademicYear().isBlank()) {
            throw new IllegalArgumentException("Tahun akademik wajib diisi.");
        }
        if (examSchedule.getDate() == null) {
            throw new IllegalArgumentException("Tanggal ujian wajib diisi.");
        }
        if (examSchedule.getStartTime() == null || examSchedule.getEndTime() == null) {
            throw new IllegalArgumentException("Jam mulai dan jam selesai wajib diisi.");
        }
        if (!examSchedule.getEndTime().isAfter(examSchedule.getStartTime())) {
            throw new IllegalArgumentException("Jam selesai harus setelah jam mulai.");
        }
        if (examSchedule.getSubject() == null) {
            throw new IllegalArgumentException("Mata pelajaran wajib dipilih.");
        }
        if (examSchedule.getClassroom() == null) {
            throw new IllegalArgumentException("Kelas wajib dipilih.");
        }
        // Ruangan dan pengawas boleh kosong: pada ujian dengan ruangan campur, keduanya diatur per ruangan
        // lewat menu Penempatan Ruangan.
    }
}