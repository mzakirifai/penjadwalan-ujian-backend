package co.id.service.impl;

import co.id.dao.TeachingAssignmentDAO;
import co.id.dao.impl.TeachingAssignmentDAOImpl;
import co.id.model.TeachingAssignment;
import co.id.service.TeachingAssignmentService;
import java.util.List;

public class TeachingAssignmentServiceImpl implements TeachingAssignmentService {

    private final TeachingAssignmentDAO teachingAssignmentDAO = new TeachingAssignmentDAOImpl();

    @Override
    public List<TeachingAssignment> getAllTeachingAssignments() {
        return teachingAssignmentDAO.getAllTeachingAssignments();
    }

    @Override
    public List<TeachingAssignment> getTeachingAssignmentBy(String keyword) {
        return teachingAssignmentDAO.getTeachingAssignmentBy(keyword);
    }

    @Override
    public List<TeachingAssignment> getTeachingAssignments(int page, int size) {
        return teachingAssignmentDAO.getTeachingAssignments(page, size);
    }

    @Override
    public List<TeachingAssignment> getByTeacher(int teacherId) {
        return teachingAssignmentDAO.getByTeacher(teacherId);
    }

    @Override
    public int countTeachingAssignments() {
        return teachingAssignmentDAO.countTeachingAssignments();
    }

    @Override
    public TeachingAssignment getById(int id) {
        return teachingAssignmentDAO.getById(id);
    }

    @Override
    public void save(TeachingAssignment ta, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);

        if (ta.getTeacher() == null || ta.getTeacher().getId() == 0) {
            throw new IllegalArgumentException("Guru wajib dipilih.");
        }
        if (ta.getClassroom() == null || ta.getClassroom().getId() == 0) {
            throw new IllegalArgumentException("Kelas wajib dipilih.");
        }
        if (ta.getSubject() == null || ta.getSubject().getId() == 0) {
            throw new IllegalArgumentException("Mata pelajaran wajib dipilih.");
        }
        if (teachingAssignmentDAO.isDuplicate(ta.getTeacher().getId(), ta.getSubject().getId(),
                ta.getClassroom().getId(), ta.getId())) {
            throw new IllegalStateException("Guru ini sudah terdaftar sebagai pengampu mapel tersebut di kelas yang sama.");
        }

        ta.setUpdatedBy(currentUsername);
        teachingAssignmentDAO.saveOrUpdate(ta);
    }

    @Override
    public void delete(int id, String currentUserRole) {
        ensureIsAdmin(currentUserRole);

        if (teachingAssignmentDAO.getById(id) == null) {
            throw new IllegalArgumentException("Data pengampu tidak ditemukan.");
        }
        teachingAssignmentDAO.delete(id);
    }

    @Override
    public boolean isAssigned(int teacherId, int subjectId, int classroomId) {
        return teachingAssignmentDAO.isAssigned(teacherId, subjectId, classroomId);
    }

    private void ensureIsAdmin(String currentUserRole) {
        if (!"admin".equalsIgnoreCase(currentUserRole)) {
            throw new SecurityException("Hanya admin yang boleh mengubah data pengampu.");
        }
    }
}