package co.id.service.impl;

import co.id.dao.ExamScheduleDAO;
import co.id.dao.impl.ExamScheduleDAOImpl;
import co.id.model.ExamSchedule;
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

        // Jadwal hanya mengatur "apa & kapan". Ruangan dan pengawas diatur di menu Penempatan Ruangan,
        // jadi di sini yang diperiksa cukup bentrok KELAS (kelas yang sama pada jam yang bertumpuk).
        List<ScheduleConflict> conflicts = findConflicts(
                examSchedule.getDate(),
                examSchedule.getStartTime(),
                examSchedule.getEndTime(),
                examSchedule.getClassroom().getId(),
                excludeId
        );

        if (!conflicts.isEmpty()) {
            StringBuilder message = new StringBuilder("Jadwal bentrok:");
            for (ScheduleConflict conflict : conflicts) {
                message.append("\n- ").append(conflict.describe());
            }
            throw new ScheduleConflictException(message.toString(), conflicts);
        }

        // Tanggal/jam/kelas berubah pada jadwal yang sudah ditempatkan: penempatan lama dikosongkan
        boolean resetPlacement = willResetPlacement(examSchedule);

        examSchedule.setUpdatedBy(currentUsername);
        examScheduleDAO.saveOrUpdate(examSchedule, resetPlacement);
    }

    @Override
    public int importExamSchedules(List<ExamSchedule> schedules, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        int saved = 0;
        for (ExamSchedule schedule : schedules) {
            try {
                save(schedule, currentUsername, currentUserRole);
            } catch (RuntimeException ex) {
                throw new IllegalStateException(saved + " jadwal sudah tersimpan, lalu berhenti di jadwal "
                        + (schedule.getSubject() != null ? schedule.getSubject().getName() : "?") + " kelas "
                        + (schedule.getClassroom() != null ? schedule.getClassroom().getName() : "?") + ":\n"
                        + ex.getMessage(), ex);
            }
            saved++;
        }
        return saved;
    }

    @Override
    public boolean willResetPlacement(ExamSchedule examSchedule) {
        if (examSchedule == null || examSchedule.getId() == 0) {
            return false;
        }
        ExamSchedule existing = examScheduleDAO.getById(examSchedule.getId());
        if (existing == null) {
            return false;
        }
        boolean changed = !java.util.Objects.equals(existing.getDate(), examSchedule.getDate())
                || !java.util.Objects.equals(existing.getStartTime(), examSchedule.getStartTime())
                || !java.util.Objects.equals(existing.getEndTime(), examSchedule.getEndTime())
                || classroomId(existing) != classroomId(examSchedule);
        return changed && examScheduleDAO.hasPlacement(examSchedule.getId());
    }

    @Override
    public boolean hasPlacement(int scheduleId) {
        return scheduleId != 0 && examScheduleDAO.hasPlacement(scheduleId);
    }

    @Override
    public List<co.id.model.SessionSlot> findMisalignedSessions(java.time.LocalDate date, java.time.LocalTime startTime,
            java.time.LocalTime endTime, int excludeId) {
        if (date == null || startTime == null || endTime == null) {
            return List.of();
        }
        return examScheduleDAO.findMisalignedSessions(date, startTime, endTime, excludeId);
    }

    @Override
    public java.util.Map<Integer, co.id.model.PlacementSummary> getPlacementSummaries(List<Integer> scheduleIds) {
        return examScheduleDAO.getPlacementSummaries(scheduleIds);
    }

    private static int classroomId(ExamSchedule schedule) {
        return schedule.getClassroom() != null ? schedule.getClassroom().getId() : 0;
    }

    @Override
    public List<ScheduleConflict> findConflicts(java.time.LocalDate date, java.time.LocalTime startTime,
            java.time.LocalTime endTime, int classroomId, int excludeId) {
        List<ScheduleConflict> result = new java.util.ArrayList<>();
        for (ExamSchedule other : examScheduleDAO.findClassOverlaps(date, startTime, endTime, classroomId, excludeId)) {
            result.add(new ScheduleConflict(other));
        }
        return result;
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