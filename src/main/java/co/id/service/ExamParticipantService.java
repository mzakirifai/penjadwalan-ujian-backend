package co.id.service;

import co.id.model.ExamParticipant;
import java.util.List;

public interface ExamParticipantService {
    public List<ExamParticipant> getAllExamParticipants();
    public List<ExamParticipant> getExamParticipantBy(String keyword);
    public List<ExamParticipant> getExamParticipants(int page, int size);
    public List<ExamParticipant> getByExamSchedule(int examScheduleId);
    public List<ExamParticipant> getByStudent(int studentId);
    public int countExamParticipants();
    public ExamParticipant getById(int id);
    public void save(ExamParticipant examParticipant, String currentUsername, String currentUserRole);
    public void delete(int id, String currentUserRole);
}
