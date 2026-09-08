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
    public List<ExamSchedule> getByTeacherAndPeriod(int teacherId, String examType, String semester, String academicYear);
    public void saveOrUpdate(ExamSchedule examSchedule);
    public void delete(int id);
    public boolean hasConflict(LocalDate date, LocalTime startTime, LocalTime endTime,
            int roomId, int classroomId, int teacherId, int excludeId);
    public boolean hasSameSubjectForClassroom(int classroomId, int subjectId, String examType,
        String semester, String academicYear, int excludeId);
}
