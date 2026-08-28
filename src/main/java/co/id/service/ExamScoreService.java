package co.id.service;

import co.id.model.ExamScore;
import java.util.List;

public interface ExamScoreService {
    public List<ExamScore> getAllExamScores();
    public List<ExamScore> getExamScoreBy(String keyword);
    public List<ExamScore> getExamScores(int page, int size);
    public List<ExamScore> getByExamSchedule(int examScheduleId);
    public List<ExamScore> getByStudent(int studentId);
    public int countExamScores();
    public int countByExamSchedule(int examScheduleId);
    public ExamScore getById(int id);
    public void save(ExamScore examScore, String currentUsername, String currentUserRole, Integer currentTeacherId);
    public void delete(int id, String currentUserRole);
}