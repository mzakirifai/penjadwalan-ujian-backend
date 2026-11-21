package co.id.dao;

import co.id.model.ExamSchedule;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ExamScheduleDAO {
    public List<ExamSchedule> getAllExamSchedules();
    public List<ExamSchedule> getAllExamSchedulesDetailed();
    public List<ExamSchedule> getExamScheduleBy(String keyword);
    public List<ExamSchedule> getExamSchedules(int page, int size);
    public List<ExamSchedule> getTodayExamSchedules();
    public int countExamSchedules();
    public ExamSchedule getById(int id);
    public List<ExamSchedule> getByPeriod(String examType, String semester, String academicYear);
    public List<ExamSchedule> getByTeacherAndPeriod(int teacherId, String examType, 
            String semester, String academicYear);
    public java.util.Map<java.time.LocalDate, Integer> getExamCountPerDay
        (java.time.LocalDate start, java.time.LocalDate end);
    public void saveOrUpdate(ExamSchedule examSchedule);
    /** Simpan; jika resetPlacement, kursi/ruangan peserta jadwal ini dikosongkan dalam transaksi yang sama. */
    public void saveOrUpdate(ExamSchedule examSchedule, boolean resetPlacement);
    /** True jika jadwal sudah punya penempatan ruangan (kursi peserta atau ruangan/pengawas data lama). */
    public boolean hasPlacement(int scheduleId);
    /** Ringkasan penempatan ruangan (ruangan, pengawas, status) untuk daftar jadwal, per id jadwal. */
    public java.util.Map<Integer, co.id.model.PlacementSummary> getPlacementSummaries(List<Integer> scheduleIds);
    /** Sesi lain di tanggal tersebut yang jamnya bertumpuk tetapi tidak sama persis dengan rentang ini. */
    public List<co.id.model.SessionSlot> findMisalignedSessions(LocalDate date, LocalTime startTime, LocalTime endTime, int excludeId);
    public void delete(int id);
    /** Jadwal lain untuk KELAS yang sama pada tanggal + rentang waktu yang overlap. */
    public List<ExamSchedule> findClassOverlaps(LocalDate date, LocalTime startTime, LocalTime endTime,
            int classroomId, int excludeId);
    public boolean hasSameSubjectForClassroom(int classroomId, int subjectId, Integer teacherCode, String examType,
        String semester, String academicYear, int excludeId);
}