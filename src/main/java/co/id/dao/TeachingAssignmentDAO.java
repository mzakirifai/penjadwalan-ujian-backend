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
    public int saveAll(List<TeachingAssignment> teachingAssignments);
    public void delete(int id);
    public boolean isAssigned(int teacherId, int subjectId, int classroomId);
    /** True jika kode guru di mapel tersebut sudah dipakai guru lain. */
    public boolean isCodeTakenByOtherTeacher(int subjectId, int teacherCode, int teacherId);
    public boolean isDuplicate(int teacherId, int subjectId, int classroomId, int excludeId);
}
