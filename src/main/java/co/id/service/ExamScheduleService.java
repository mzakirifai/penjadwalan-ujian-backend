package co.id.service;

import co.id.model.ExamSchedule;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ExamScheduleService {
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
    public java.util.Map<java.time.LocalDate, Integer> getExamCountPerDay(java.time.LocalDate start, 
            java.time.LocalDate end);
    public void save(ExamSchedule examSchedule, String currentUsername, String currentUserRole);
    /** True jika menyimpan perubahan ini (tanggal/jam/kelas) akan mengosongkan penempatan ruangan yang sudah ada. */
    public boolean willResetPlacement(ExamSchedule examSchedule);
    /** True jika jadwal sudah punya penempatan ruangan. */
    public boolean hasPlacement(int scheduleId);
    /** Ringkasan penempatan ruangan (ruangan, pengawas, status) untuk daftar jadwal, per id jadwal. */
    public java.util.Map<Integer, co.id.model.PlacementSummary> getPlacementSummaries(List<Integer> scheduleIds);
    /** Sesi lain di tanggal tersebut yang jamnya bertumpuk tetapi tidak sama persis (tidak bisa dicampur). */
    public List<co.id.model.SessionSlot> findMisalignedSessions(LocalDate date, LocalTime startTime, LocalTime endTime, int excludeId);
    public void delete(int id, String currentUserRole);
    /** Bentrokan (ruangan / kelas / pengawas) untuk rentang waktu ini; kosong jika tidak ada. */
    public List<co.id.model.ScheduleConflict> findConflicts(LocalDate date, LocalTime startTime, LocalTime endTime,
            int roomId, int classroomId, int teacherId, int excludeId);
    /** Ruangan kosong pada rentang waktu ini, yang paling pas untuk kelas tersebut di urutan pertama. */
    public List<co.id.model.RoomSuggestion> suggestRooms(LocalDate date, LocalTime startTime, LocalTime endTime,
            int excludeId, Integer classroomId);
    public boolean hasSameSubjectForClassroom(int classroomId, int subjectId, Integer teacherCode, String examType,
        String semester, String academicYear, int excludeId);
}