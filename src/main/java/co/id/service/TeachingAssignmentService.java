package co.id.service;

import co.id.model.TeachingAssignment;
import java.util.List;

public interface TeachingAssignmentService {
    public List<TeachingAssignment> getAllTeachingAssignments();
    public List<TeachingAssignment> getTeachingAssignmentBy(String keyword);
    public List<TeachingAssignment> getTeachingAssignments(int page, int size);
    public List<TeachingAssignment> getByTeacher(int teacherId);
    public int countTeachingAssignments();
    public TeachingAssignment getById(int id);
    public void save(TeachingAssignment teachingAssignment, String currentUsername, String currentUserRole);
    public void delete(int id, String currentUserRole);
    public boolean isAssigned(int teacherId, int subjectId, int classroomId);
}