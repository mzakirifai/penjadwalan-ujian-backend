package co.id.dao;

import co.id.model.ExamScore;
import java.util.List;

public interface ExamScoreDAO {
    public List<ExamScore> getAllExamScores();
    public List<ExamScore> getExamScoreBy(String keyword);
    public List<ExamScore> getExamScores(int page, int size);
    public List<ExamScore> getByExamSchedule(int examScheduleId);
    public List<ExamScore> getByStudent(int studentId);
    public int countExamScores();
    public int countByExamSchedule(int examScheduleId);
    public ExamScore getById(int id);
    public void saveOrUpdate(ExamScore examScore);
    public void delete(int id);
    public boolean hasScore(int examScheduleId, int studentId, int excludeId);
}