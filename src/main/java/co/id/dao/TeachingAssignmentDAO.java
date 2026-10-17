package co.id.dao;

import co.id.model.TeachingAssignment;
import java.util.List;

public interface TeachingAssignmentDAO {
    public List<TeachingAssignment> getAllTeachingAssignments();
    public List<TeachingAssignment> getTeachingAssignmentBy(String keyword);
    public List<TeachingAssignment> getTeachingAssignments(int page, int size);
    public List<TeachingAssignment> getByTeacher(int teacherId);
    public int countTeachingAssignments();
    public TeachingAssignment getById(int id);
    public void saveOrUpdate(TeachingAssignment teachingAssignment);
    public void delete(int id);
    public boolean isAssigned(int teacherId, int subjectId, int classroomId);
    public boolean isDuplicate(int teacherId, int subjectId, int classroomId, int excludeId);
}
