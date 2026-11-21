package co.id.service.impl;

import co.id.dao.ExamParticipantDAO;
import co.id.dao.impl.ExamParticipantDAOImpl;
import co.id.model.ExamParticipant;
import co.id.service.ExamParticipantService;
import java.util.List;

public class ExamParticipantServiceImpl implements ExamParticipantService{
    
    private final ExamParticipantDAO examParticipantDAO = new ExamParticipantDAOImpl();

    @Override
    public List<ExamParticipant> getAllExamParticipants() {
        return examParticipantDAO.getAllExamParticipants();
    }

    @Override
    public List<ExamParticipant> getExamParticipantBy(String keyword) {
        return examParticipantDAO.getExamParticipantBy(keyword);
    }

    @Override
    public List<ExamParticipant> getExamParticipants(int page, int size) {
        return examParticipantDAO.getExamParticipants(page, size);
    }

    @Override
    public List<ExamParticipant> getByExamSchedule(int examScheduleId) {
        return examParticipantDAO.getByExamSchedule(examScheduleId);
    }

    @Override
    public List<ExamParticipant> getByStudent(int studentId) {
        return examParticipantDAO.getByStudent(studentId);
    }

    @Override
    public int countExamParticipants() {
        return examParticipantDAO.countExamParticipants();
    }
    
    @Override
    public int countByExamSchedule(int examScheduleId) {
        return examParticipantDAO.countByExamSchedule(examScheduleId);
    }

    @Override
    public ExamParticipant getById(int id) {
        return examParticipantDAO.getById(id);
    }

    @Override
    public void save(ExamParticipant examParticipant, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        validate(examParticipant);

        int examScheduleId = examParticipant.getExamSchedule().getId();
        int studentId = examParticipant.getStudent().getId();
        int excludeStudentId = (examParticipant.getId() == 0) ? 0 : studentId;

        // 1. Cek siswa sudah terdaftar di ujian ini (khusus data baru)
        if (examParticipant.getId() == 0 && examParticipantDAO.isStudentRegistered(examScheduleId, studentId)) {
            throw new IllegalStateException(
                    "Siswa ini sudah terdaftar sebagai peserta pada jadwal ujian yang sama.");
        }

        // (Kapasitas ruangan dijaga oleh menu Penempatan Ruangan: ruangan tidak lagi melekat pada jadwal.)

        // 2. Cek nomor kursi sudah dipakai siswa lain
        // Peserta yang sudah ditempatkan di ruangan campur: kursi dicek per ruangan (dan lintas ujian pada waktu yang sama)
        boolean seatTaken = examParticipant.getRoom() != null
                ? examParticipantDAO.isSeatTakenInRoom(examScheduleId, examParticipant.getRoom().getId(),
                        examParticipant.getSeatNumber(), excludeStudentId)
                : examParticipantDAO.isSeatTaken(examScheduleId, examParticipant.getSeatNumber(), excludeStudentId);
        if (seatTaken) {
            throw new IllegalStateException(
                    "No. Kursi \"" + examParticipant.getSeatNumber() + "\" sudah dipakai oleh siswa lain "
                            + (examParticipant.getRoom() != null ? "di ruangan " + examParticipant.getRoom().getName() : "pada ujian ini") + ".");
        }

        // 3. Cek nomor peserta sudah dipakai siswa lain
        if (examParticipantDAO.isParticipantNumberTaken(examScheduleId, examParticipant.getParticipantNumber(), excludeStudentId)) {
            throw new IllegalStateException(
                    "No. Peserta \"" + examParticipant.getParticipantNumber() + "\" sudah dipakai oleh siswa lain pada ujian ini.");
        }

        examParticipant.setUpdatedBy(currentUsername);
        examParticipantDAO.saveOrUpdate(examParticipant);
    }

    @Override
    public void delete(int id, String currentUserRole) {
        ensureIsAdmin(currentUserRole);

        ExamParticipant existing = examParticipantDAO.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Data peserta ujian tidak ditemukan.");
        }

        examParticipantDAO.delete(id);
    }
    
    private void ensureIsAdmin(String currentUserRole) {
        if (!"admin".equalsIgnoreCase(currentUserRole)) {
            throw new SecurityException("Hanya admin yang boleh mengubah data peserta ujian.");
        }
    }
    
    private void validate(ExamParticipant examParticipant) {
        if (examParticipant.getExamSchedule() == null || examParticipant.getExamSchedule().getId() == 0) {
            throw new IllegalArgumentException("Jadwal ujian wajib dipilih.");
        }
        if (examParticipant.getStudent() == null || examParticipant.getStudent().getId() == 0) {
            throw new IllegalArgumentException("Siswa wajib dipilih.");
        }
        if (examParticipant.getParticipantNumber() == null || examParticipant.getParticipantNumber().isBlank()) {
            throw new IllegalArgumentException("No. Peserta wajib diisi.");
        }
        if (examParticipant.getSeatNumber() == null || examParticipant.getSeatNumber().isBlank()) {
            throw new IllegalArgumentException("No. Kursi wajib diisi.");
        }
    }
}