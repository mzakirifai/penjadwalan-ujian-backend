package co.id.service;

import co.id.model.TeachingAssignment;
import java.util.List;

public interface TeachingAssignmentService {
    public List<TeachingAssignment> getAllTeachingAssignments();
    public List<TeachingAssignment> getTeachingAssignmentBy(String keyword);
    public List<TeachingAssignment> getTeachingAssignments(int page, int size);
    public List<TeachingAssignment> getByTeacher(int teacherId);
    public List<TeachingAssignment> getCodedByClassroomAndSubject(int classroomId, int subjectId);
    public boolean isAssignedForExam(int teacherId, int subjectId, int classroomId, Integer examTeacherCode);
    public int countTeachingAssignments();
    public TeachingAssignment getById(int id);
    public void save(TeachingAssignment teachingAssignment, String currentUsername, String currentUserRole);

    /**
     * Menambah banyak pengampu sekaligus (satu guru untuk banyak kelas dan mapel).
     * Kombinasi guru+mapel+kelas yang sudah ada otomatis dilewati.
     *
     * @return jumlah data yang benar-benar ditambahkan
     */
    public int saveAll(List<TeachingAssignment> teachingAssignments, String currentUsername, String currentUserRole);
    public void delete(int id, String currentUserRole);
    /** Import dari Excel: semua baris tersimpan dalam satu transaksi, atau tidak sama sekali. */
    public int importTeachingAssignments(List<TeachingAssignment> teachingAssignments, String currentUsername, String currentUserRole);
    public boolean isAssigned(int teacherId, int subjectId, int classroomId);
}