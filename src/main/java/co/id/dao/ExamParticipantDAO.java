package co.id.dao;

import co.id.model.ExamParticipant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ExamParticipantDAO {
    public List<ExamParticipant> getAllExamParticipants();
    public List<ExamParticipant> getExamParticipantBy(String keyword);
    public List<ExamParticipant> getExamParticipants(int page, int size);
    public List<ExamParticipant> getByExamSchedule(int examScheduleId);
    public List<ExamParticipant> getByStudent(int studentId);
    public int countExamParticipants();
    public int countByExamSchedule(int examScheduleId);
    public ExamParticipant getById(int id);
    public void saveOrUpdate(ExamParticipant examParticipant);
    public void delete(int id);
    public boolean isSeatTaken(int examScheduleId, String seatNumber, int excludeStudentId);
    public boolean isParticipantNumberTaken(int examScheduleId, String participantNumber, int excludeStudentId);
    public boolean isStudentRegistered(int examScheduleId, int studentId);
}
