package co.id.service.impl;

import co.id.dao.ExamScheduleDAO;
import co.id.dao.impl.ExamScheduleDAOImpl;
import co.id.model.ExamSchedule;
import co.id.service.ExamScheduleService;
import java.util.List;

public class ExamScheduleServiceImpl implements ExamScheduleService {

    private final ExamScheduleDAO examScheduleDAO = new ExamScheduleDAOImpl();

    @Override
    public List<ExamSchedule> getAllExamSchedules() {
        return examScheduleDAO.getAllExamSchedules();
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
    public void save(ExamSchedule examSchedule, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        validate(examSchedule);

        int excludeId = examSchedule.getId(); // 0 kalau data baru, sehingga tidak bentrok dengan dirinya sendiri
        boolean conflict = examScheduleDAO.hasConflict(
                examSchedule.getDate(),
                examSchedule.getStartTime(),
                examSchedule.getEndTime(),
                examSchedule.getRoom().getId(),
                examSchedule.getClassroom().getId(),
                examSchedule.getTeacher().getId(),
                excludeId
        );

        if (conflict) {
            throw new IllegalStateException(
                    "Jadwal bentrok! Ruangan, kelas, atau guru sudah terpakai pada rentang waktu ini.");
        }

        examSchedule.setUpdatedBy(currentUsername);
        examScheduleDAO.saveOrUpdate(examSchedule);
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
    public boolean hasSameSubjectForClassroom(int classroomId, int subjectId, String examType,
            String semester, String academicYear, int excludeId) {
        return examScheduleDAO.hasSameSubjectForClassroom(classroomId, subjectId, examType, semester, academicYear, excludeId);
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
        if (examSchedule.getRoom() == null) {
            throw new IllegalArgumentException("Ruangan wajib dipilih.");
        }
        if (examSchedule.getTeacher() == null) {
            throw new IllegalArgumentException("Pengawas (guru) wajib dipilih.");
        }
    }
}