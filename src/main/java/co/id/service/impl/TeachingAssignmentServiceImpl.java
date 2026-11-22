package co.id.service.impl;

import co.id.dao.TeachingAssignmentDAO;
import co.id.dao.impl.TeachingAssignmentDAOImpl;
import co.id.importer.TeachingAssignmentImportParser;
import co.id.model.TeachingAssignment;
import co.id.service.TeachingAssignmentService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public List<TeachingAssignment> getCodedByClassroomAndSubject(int classroomId, int subjectId) {
        return teachingAssignmentDAO.getCodedByClassroomAndSubject(classroomId, subjectId);
    }

    @Override
    public boolean isAssignedForExam(int teacherId, int subjectId, int classroomId, Integer examTeacherCode) {
        return teachingAssignmentDAO.isAssignedForExam(teacherId, subjectId, classroomId, examTeacherCode);
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
        validateCode(ta);
        if (teachingAssignmentDAO.isDuplicate(ta.getTeacher().getId(), ta.getSubject().getId(),
                ta.getClassroom().getId(), ta.getId())) {
            throw new IllegalStateException("Guru ini sudah terdaftar sebagai pengampu mapel tersebut di kelas yang sama.");
        }

        ta.setUpdatedBy(currentUsername);
        teachingAssignmentDAO.saveOrUpdate(ta);
    }

    @Override
    public int saveAll(List<TeachingAssignment> teachingAssignments, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);

        if (teachingAssignments == null || teachingAssignments.isEmpty()) {
            throw new IllegalArgumentException("Tidak ada kombinasi guru, kelas, dan mapel yang dipilih.");
        }

        List<TeachingAssignment> toInsert = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (TeachingAssignment ta : teachingAssignments) {
            if (ta.getTeacher() == null || ta.getTeacher().getId() == 0) {
                throw new IllegalArgumentException("Guru wajib dipilih.");
            }
            if (ta.getClassroom() == null || ta.getClassroom().getId() == 0) {
                throw new IllegalArgumentException("Kelas wajib dipilih.");
            }
            if (ta.getSubject() == null || ta.getSubject().getId() == 0) {
                throw new IllegalArgumentException("Mata pelajaran wajib dipilih.");
            }

            int teacherId = ta.getTeacher().getId();
            int subjectId = ta.getSubject().getId();
            int classroomId = ta.getClassroom().getId();
            validateCode(ta);

            // Lewati kombinasi yang dobel di dalam daftar maupun yang sudah ada di database
            boolean firstTime = seen.add(teacherId + "-" + subjectId + "-" + classroomId);
            if (!firstTime || teachingAssignmentDAO.isAssigned(teacherId, subjectId, classroomId)) {
                continue;
            }

            ta.setId(0);
            ta.setUpdatedBy(currentUsername);
            toInsert.add(ta);
        }

        return teachingAssignmentDAO.saveAll(toInsert);
    }

    @Override
    public int importTeachingAssignments(List<TeachingAssignment> teachingAssignments, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);

        if (teachingAssignments == null || teachingAssignments.isEmpty()) {
            return 0;
        }

        // Validasi ulang di service, jangan hanya mengandalkan pengecekan di layar pratinjau
        Set<String> seen = new HashSet<>();
        Map<String, Integer> codeOwners = new HashMap<>();
        for (TeachingAssignment ta : teachingAssignments) {
            if (ta.getTeacher() == null || ta.getSubject() == null || ta.getClassroom() == null) {
                throw new IllegalArgumentException("Guru, mapel, dan kelas wajib diisi.");
            }
            int teacherId = ta.getTeacher().getId();
            int subjectId = ta.getSubject().getId();
            int classroomId = ta.getClassroom().getId();
            String label = ta.getTeacher().getName() + " - " + ta.getSubject().getName() + " - " + ta.getClassroom().getName();

            if (!TeachingAssignmentImportParser.isSubjectApplicable(ta.getSubject(), ta.getClassroom())) {
                throw new IllegalArgumentException("Mapel tidak berlaku untuk kelasnya (" + label + ").");
            }
            validateCode(ta);
            if (ta.getTeacherCode() != null) {
                Integer owner = codeOwners.putIfAbsent(subjectId + "#" + ta.getTeacherCode(), teacherId);
                if (owner != null && owner != teacherId) {
                    throw new IllegalStateException("Kode " + ta.getTeacherCode() + " pada mapel \"" + ta.getSubject().getName()
                            + "\" dipakai lebih dari satu guru di file.");
                }
            }
            if (!seen.add(teacherId + "-" + subjectId + "-" + classroomId)
                    || teachingAssignmentDAO.isAssigned(teacherId, subjectId, classroomId)) {
                throw new IllegalStateException("Pengampu sudah terdaftar atau ganda (" + label + ").");
            }

            ta.setId(0);
            ta.setUpdatedBy(currentUsername);
        }

        return teachingAssignmentDAO.saveAll(teachingAssignments);
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

    // Kode guru boleh kosong (mapel yang hanya punya satu guru). Jika diisi: 1-99,
    // dan di dalam satu mapel satu kode hanya boleh dipakai satu guru.
    private void validateCode(TeachingAssignment ta) {
        Integer code = ta.getTeacherCode();
        if (code == null) {
            return;
        }
        if (code < 1 || code > 99) {
            throw new IllegalArgumentException("Kode guru harus berupa angka 1 sampai 99.");
        }
        if (teachingAssignmentDAO.isCodeTakenByOtherTeacher(ta.getSubject().getId(), code, ta.getTeacher().getId())) {
            throw new IllegalStateException("Kode " + code + " pada mapel \"" + ta.getSubject().getName()
                    + "\" sudah dipakai guru lain. Gunakan kode yang berbeda.");
        }
    }

    private void ensureIsAdmin(String currentUserRole) {
        if (!"admin".equalsIgnoreCase(currentUserRole)) {
            throw new SecurityException("Hanya admin yang boleh mengubah data pengampu.");
        }
    }
}