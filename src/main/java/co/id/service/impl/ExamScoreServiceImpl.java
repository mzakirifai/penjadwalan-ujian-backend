package co.id.service.impl;

import co.id.dao.ExamParticipantDAO;
import co.id.dao.ExamScheduleDAO;
import co.id.dao.ExamScoreDAO;
import co.id.dao.TeachingAssignmentDAO;
import co.id.dao.impl.ExamParticipantDAOImpl;
import co.id.dao.impl.ExamScheduleDAOImpl;
import co.id.dao.impl.ExamScoreDAOImpl;
import co.id.dao.impl.TeachingAssignmentDAOImpl;
import co.id.model.ExamSchedule;
import co.id.model.ExamScore;
import co.id.service.ExamScoreService;
import java.util.List;

public class ExamScoreServiceImpl implements ExamScoreService {

    private final ExamScoreDAO examScoreDAO = new ExamScoreDAOImpl();
    private final ExamParticipantDAO examParticipantDAO = new ExamParticipantDAOImpl();
    private final ExamScheduleDAO examScheduleDAO = new ExamScheduleDAOImpl();
    private final TeachingAssignmentDAO teachingAssignmentDAO = new TeachingAssignmentDAOImpl();

    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    @Override
    public List<ExamScore> getAllExamScores() {
        return examScoreDAO.getAllExamScores();
    }

    @Override
    public List<ExamScore> getExamScoreBy(String keyword) {
        return examScoreDAO.getExamScoreBy(keyword);
    }

    @Override
    public List<ExamScore> getExamScores(int page, int size) {
        return examScoreDAO.getExamScores(page, size);
    }

    @Override
    public List<ExamScore> getByExamSchedule(int examScheduleId) {
        return examScoreDAO.getByExamSchedule(examScheduleId);
    }

    @Override
    public List<ExamScore> getByStudent(int studentId) {
        return examScoreDAO.getByStudent(studentId);
    }

    @Override
    public int countExamScores() {
        return examScoreDAO.countExamScores();
    }

    @Override
    public int countByExamSchedule(int examScheduleId) {
        return examScoreDAO.countByExamSchedule(examScheduleId);
    }

    @Override
    public ExamScore getById(int id) {
        return examScoreDAO.getById(id);
    }

    @Override
    public void save(ExamScore examScore, String currentUsername, String currentUserRole, Integer currentTeacherId) {
        ensureCanInputScore(currentUserRole, currentTeacherId, examScore);
        validate(examScore);

        int examScheduleId = examScore.getExamSchedule().getId();
        int studentId = examScore.getStudent().getId();
        int excludeId = examScore.getId(); // 0 kalau data baru, sehingga tidak bentrok dengan dirinya sendiri

        // 1. Cek siswa memang terdaftar sebagai peserta di ujian ini
        if (!examParticipantDAO.isStudentRegistered(examScheduleId, studentId)) {
            throw new IllegalStateException(
                    "Siswa ini belum terdaftar sebagai peserta pada jadwal ujian ini. Daftarkan sebagai peserta terlebih dahulu.");
        }

        // 2. Cek siswa belum punya nilai untuk ujian yang sama
        if (examScoreDAO.hasScore(examScheduleId, studentId, excludeId)) {
            throw new IllegalStateException(
                    "Siswa ini sudah memiliki nilai untuk jadwal ujian yang sama.");
        }

        examScore.setUpdatedBy(currentUsername);
        examScoreDAO.saveOrUpdate(examScore);
    }

    @Override
    public int importScores(List<ExamScore> scores, String currentUsername, String currentUserRole,
            Integer currentTeacherId) {
        if (scores == null || scores.isEmpty()) {
            return 0;
        }

        int examScheduleId = scores.get(0).getExamSchedule().getId();
        ensureCanInputScore(currentUserRole, currentTeacherId, scores.get(0));

        java.util.Set<Integer> registered = new java.util.HashSet<>();
        for (co.id.model.ExamParticipant participant : examParticipantDAO.getByExamSchedule(examScheduleId)) {
            registered.add(participant.getStudent().getId());
        }

        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (ExamScore score : scores) {
            validate(score);
            if (score.getExamSchedule().getId() != examScheduleId) {
                throw new IllegalArgumentException("Semua nilai harus untuk satu jadwal ujian yang sama.");
            }
            int studentId = score.getStudent().getId();
            if (!registered.contains(studentId)) {
                throw new IllegalStateException("Siswa " + score.getStudent().getName()
                        + " belum terdaftar sebagai peserta pada jadwal ujian ini.");
            }
            if (!seen.add(studentId)) {
                throw new IllegalArgumentException("Siswa " + score.getStudent().getName() + " muncul dua kali.");
            }
            score.setUpdatedBy(currentUsername);
        }

        examScoreDAO.saveBatch(scores);
        return scores.size();
    }

    @Override
    public void delete(int id, String currentUserRole) {
        ensureIsAdmin(currentUserRole);

        ExamScore existing = examScoreDAO.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Data nilai ujian tidak ditemukan.");
        }

        examScoreDAO.delete(id);
    }
    
    @Override
    public co.id.model.report.PassFailSummary getPassFailSummary(String semester, String academicYear) {
        return examScoreDAO.getPassFailSummary(semester, academicYear);
    }

    private void ensureIsAdmin(String currentUserRole) {
        if (!"admin".equalsIgnoreCase(currentUserRole)) {
            throw new SecurityException("Hanya admin yang boleh mengubah data nilai ujian.");
        }
    }

    private void validate(ExamScore examScore) {
        if (examScore.getExamSchedule() == null || examScore.getExamSchedule().getId() == 0) {
            throw new IllegalArgumentException("Jadwal ujian wajib dipilih.");
        }
        if (examScore.getStudent() == null || examScore.getStudent().getId() == 0) {
            throw new IllegalArgumentException("Siswa wajib dipilih.");
        }
        if (examScore.getScore() < MIN_SCORE || examScore.getScore() > MAX_SCORE) {
            throw new IllegalArgumentException(
                    "Nilai harus di antara " + MIN_SCORE + " - " + MAX_SCORE + ".");
        }
    }
    
    private void ensureCanInputScore(String currentUserRole, Integer currentTeacherId, ExamScore examScore) {
        if ("admin".equalsIgnoreCase(currentUserRole)) {
            return;
        }

        if ("guru".equalsIgnoreCase(currentUserRole)) {
            if (examScore.getExamSchedule() == null || examScore.getExamSchedule().getId() == 0) {
                throw new IllegalArgumentException("Data jadwal ujian tidak lengkap.");
            }

            // Ambil ulang jadwal dari database agar mapel & kelasnya pasti lengkap
            ExamSchedule schedule = examScheduleDAO.getById(examScore.getExamSchedule().getId());
            if (schedule == null || schedule.getSubject() == null || schedule.getClassroom() == null) {
                throw new IllegalArgumentException("Data jadwal ujian tidak lengkap (mapel/kelas tidak diketahui).");
            }

            // Guru hanya boleh menginput nilai untuk mapel & kelas yang diampunya; jika ujiannya berkode
            // (mis. DDPK TKJ (2)), kode guru di data pengampu juga harus sama.
            boolean isTeachingAssignment = currentTeacherId != null && teachingAssignmentDAO.isAssignedForExam(
                    currentTeacherId, schedule.getSubject().getId(), schedule.getClassroom().getId(),
                    schedule.getTeacherCode());

            if (!isTeachingAssignment) {
                throw new SecurityException("Anda hanya bisa menginput nilai untuk mata pelajaran, kelas, dan kode guru yang Anda ajar.");
            }
            return;
        }

        throw new SecurityException("Anda tidak memiliki akses untuk menginput nilai.");
    }
}