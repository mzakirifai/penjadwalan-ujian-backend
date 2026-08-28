package co.id.service;

import co.id.model.ExamSchedule;
import java.util.List;

public interface ExamScheduleService {
    public List<ExamSchedule> getAllExamSchedules();
    public List<ExamSchedule> getExamScheduleBy(String keyword);
    public List<ExamSchedule> getExamSchedules(int page, int size);
    public List<ExamSchedule> getTodayExamSchedules();
    public int countExamSchedules();
    public ExamSchedule getById(int id);
    public void save(ExamSchedule examSchedule, String currentUsername, String currentUserRole);
    public void delete(int id, String currentUserRole);
    public boolean hasSameSubjectForClassroom(int classroomId, int subjectId, String examType,
        String semester, String academicYear, int excludeId);
}