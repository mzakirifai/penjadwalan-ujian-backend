package co.id.service.impl;

import co.id.dao.ClassroomDAO;
import co.id.dao.MajorDAO;
import co.id.dao.RoomDAO;
import co.id.dao.UserDAO;
import co.id.dao.StudentDAO;
import co.id.dao.SubjectDAO;
import co.id.dao.TeacherDAO;
import co.id.dao.impl.ClassroomDAOImpl;
import co.id.dao.impl.MajorDAOImpl;
import co.id.dao.impl.RoomDAOImpl;
import co.id.dao.impl.UserDAOImpl;
import co.id.dao.impl.StudentDAOImpl;
import co.id.dao.impl.SubjectDAOImpl;
import co.id.dao.impl.TeacherDAOImpl;
import co.id.model.Classroom;
import co.id.model.Major;
import co.id.model.Room;
import co.id.model.User;
import co.id.model.SessionOccupancy;
import co.id.model.Student;
import co.id.model.Subject;
import co.id.model.Teacher;
import co.id.service.MasterService;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class MasterServiceImpl implements MasterService{
    private final TeacherDAO teacherDAO;
    private final MajorDAO majorDAO;
    private final ClassroomDAO classroomDAO;
    private final StudentDAO studentDAO;
    private final SubjectDAO subjectDAO;
    private final RoomDAO roomDAO;
    private final UserDAO userDAO;

    public MasterServiceImpl() {
        teacherDAO = new TeacherDAOImpl();
        majorDAO = new MajorDAOImpl();
        classroomDAO = new ClassroomDAOImpl();
        studentDAO = new StudentDAOImpl();
        subjectDAO = new SubjectDAOImpl();
        roomDAO = new RoomDAOImpl();
        userDAO = new UserDAOImpl();
    }

    // Teacher
    @Override
    public List<Teacher> getAllTeachers() {
        return teacherDAO.getAllTeachers();
    }

    @Override
    public List<Teacher> getTeacherBy(String keyword) {
        return teacherDAO.getTeacherBy(keyword);
    }

    @Override
    public List<Teacher> getTeachers(int page, int size) {
        return teacherDAO.getTeachers(page, size);
    }

    @Override
    public int countTeachers() {
        return teacherDAO.countTeachers();
    }

    @Override
    public Teacher getByIdTeacher(int id) {
        return teacherDAO.getById(id);
    }

    @Override
    public void saveOrUpdateTeacher(Teacher teacher, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);

        // NIP opsional (banyak guru honorer belum punya NIP), tetapi jika diisi harus unik
        teacher.setNip(normalizeNip(teacher.getNip()));
        if (teacher.getName() == null || teacher.getName().isBlank() || teacher.getGender() == null) {
            throw new IllegalArgumentException("Nama dan Jenis Kelamin wajib diisi.");
        }
        if (teacher.getNip() != null) {
            for (Teacher other : teacherDAO.getAllTeachers()) {
                if (other.getId() != teacher.getId() && teacher.getNip().equals(normalizeNip(other.getNip()))) {
                    throw new IllegalArgumentException("NIP " + teacher.getNip() + " sudah dipakai " + other.getName() + ".");
                }
            }
        }

        teacher.setUpdatedBy(currentUsername);
        teacherDAO.saveOrUpdate(teacher);
    }
    
    @Override
    public int importTeachers(List<Teacher> teachers, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);

        if (teachers == null || teachers.isEmpty()) {
            return 0;
        }

        // Validasi ulang di service, jangan hanya mengandalkan pengecekan di layar
        Set<String> seenNip = new HashSet<>();
        for (Teacher existing : teacherDAO.getAllTeachers()) {
            if (normalizeNip(existing.getNip()) != null) {
                seenNip.add(normalizeNip(existing.getNip()));
            }
        }

        for (Teacher teacher : teachers) {
            teacher.setNip(normalizeNip(teacher.getNip()));
            if (teacher.getName() == null || teacher.getName().isBlank() || teacher.getGender() == null) {
                throw new IllegalArgumentException("Nama dan Jenis Kelamin wajib diisi (" + teacher.getName() + ").");
            }
            // NIP boleh kosong; yang diisi tidak boleh sama dengan guru lain
            if (teacher.getNip() != null && !seenNip.add(teacher.getNip())) {
                throw new IllegalArgumentException("NIP " + teacher.getNip() + " sudah terdaftar.");
            }
            teacher.setUpdatedBy(currentUsername);
        }

        return teacherDAO.saveBatch(teachers);
    }

    // Hapus data master. Bila ditolak foreign key (data masih dipakai, ON DELETE RESTRICT),
    // tampilkan pesan yang bisa dimengerti admin, bukan pesan mentah dari MySQL.
    private static void deleteSafely(Runnable delete, String messageWhenInUse) {
        try {
            delete.run();
        } catch (RuntimeException exception) {
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof java.sql.SQLIntegrityConstraintViolationException) {
                    throw new IllegalStateException(messageWhenInUse, exception);
                }
            }
            throw exception;
        }
    }

    // NIP dirapikan: spasi dibuang, kosong menjadi null
    private static String normalizeNip(String nip) {
        if (nip == null) {
            return null;
        }
        String cleaned = nip.replaceAll("\\s+", "");
        return cleaned.isEmpty() ? null : cleaned;
    }

    @Override
    public void deleteTeacher(int id, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        User account = userDAO.getByTeacherId(id);
        if (account != null) {
            throw new IllegalStateException("Guru ini tidak bisa dihapus karena masih terhubung dengan akun login \""
                    + account.getUsername() + "\". Hapus akun tersebut terlebih dahulu di menu User Management.");
        }
        deleteSafely(() -> teacherDAO.delete(id),
                "Guru ini tidak bisa dihapus karena masih menjadi pengawas di Penempatan Ruangan. Ganti pengawasnya terlebih dahulu.");
    }

    // Major
    @Override
    public List<Major> getAllMajors() {
        return majorDAO.getAllMajors();
    }

    @Override
    public List<Major> getMajorBy(String keyword) {
        return majorDAO.getMajorBy(keyword);
    }

    @Override
    public List<Major> getMajors(int page, int size) {
        return majorDAO.getMajors(page, size);
    }

    @Override
    public int countMajors() {
        return majorDAO.countMajors();
    }

    @Override
    public Major getByIdMajor(int id) {
        return majorDAO.getById(id);
    }

    @Override
    public void saveOrUpdateMajor(Major major, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        major.setUpdatedBy(currentUsername);
        majorDAO.saveOrUpdate(major);
    }

    @Override
    public void deleteMajor(int id, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        majorDAO.delete(id);
    }

    // Classroom
    @Override
    public List<Classroom> getAllClassrooms() {
        return classroomDAO.getAllClassrooms();
    }

    @Override
    public List<Classroom> getClassroomBy(String keyword) {
        return classroomDAO.getClassroomBy(keyword);
    }

    @Override
    public List<Classroom> getClassrooms(int page, int size) {
        return classroomDAO.getClassrooms(page, size);
    }

    @Override
    public int countClassrooms() {
        return classroomDAO.countClassrooms();
    }

    @Override
    public Classroom getByIdClassroom(int id) {
        return classroomDAO.getById(id);
    }

    @Override
    public void saveOrUpdateClassroom(Classroom classroom, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        classroom.setUpdatedBy(currentUsername);
        classroomDAO.saveOrUpdate(classroom);
    }

    @Override
    public void deleteClassroom(int id, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        deleteSafely(() -> classroomDAO.delete(id),
                "Kelas ini tidak bisa dihapus karena masih punya jadwal ujian. Hapus atau ubah jadwal ujiannya terlebih dahulu.");
    }

    // Student
    @Override
    public List<Student> getAllStudents() {
        return studentDAO.getAllStudents();
    }

    @Override
    public List<Student> getStudentBy(String keyword) {
        return studentDAO.getStudentBy(keyword);
    }

    @Override
    public List<Student> getStudents(int page, int size) {
        return studentDAO.getStudents(page, size);
    }

    @Override
    public int countStudents() {
        return studentDAO.countStudents();
    }

    @Override
    public Student getByIdStudent(int id) {
        return studentDAO.getById(id);
    }
    
    @Override
    public List<Student> getStudentsByClassroom(int classroomId) {
        return studentDAO.getByClassroom(classroomId);
    }

    @Override
    public void saveOrUpdateStudent(Student student, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        validateStudent(student);
        student.setUpdatedBy(currentUsername);
        studentDAO.saveOrUpdate(student);
    }
    
    @Override
    public Set<String> getAllStudentNis() {
        return studentDAO.getAllNis();
    }

    @Override
    public Set<String> getAllStudentNisn() {
        return studentDAO.getAllNisn();
    }

    @Override
    public int importStudents(List<Student> students, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);

        if (students == null || students.isEmpty()) {
            return 0;
        }

        // Validasi ulang di service, jangan hanya mengandalkan pengecekan di layar
        Set<String> existingNis = studentDAO.getAllNis();
        Set<String> existingNisn = studentDAO.getAllNisn();
        Set<String> seenNis = new HashSet<>();
        Set<String> seenNisn = new HashSet<>();

        for (Student student : students) {
            String nis = student.getNis() == null ? "" : student.getNis().trim();
            String nisn = student.getNisn() == null ? "" : student.getNisn().trim();
            String name = student.getName() == null ? "" : student.getName().trim();

            if (nis.isEmpty() || name.isEmpty()) {
                throw new IllegalArgumentException("NIS dan Nama wajib diisi (NIS: " + nis + ").");
            }
            if (!nisn.matches("\\d{10}")) {
                throw new IllegalArgumentException("NISN harus 10 digit angka (NIS: " + nis + ").");
            }
            if (student.getClassroom() == null) {
                throw new IllegalArgumentException("Kelas wajib dipilih (NIS: " + nis + ").");
            }
            if (existingNis.contains(nis) || !seenNis.add(nis)) {
                throw new IllegalArgumentException("NIS " + nis + " sudah terdaftar.");
            }
            if (existingNisn.contains(nisn) || !seenNisn.add(nisn)) {
                throw new IllegalArgumentException("NISN " + nisn + " sudah terdaftar.");
            }

            student.setNis(nis);
            student.setNisn(nisn);
            student.setName(name);
            student.setUpdatedBy(currentUsername);
        }

        return studentDAO.saveBatch(students);
    }

    private void validateStudent(Student student) {
        String nis = student.getNis() == null ? "" : student.getNis().trim();
        if (nis.isEmpty()) {
            throw new IllegalArgumentException("NIS wajib diisi.");
        }
        student.setNis(nis);
        if (studentDAO.isNisTaken(nis, student.getId())) {
            throw new IllegalStateException("NIS \"" + nis + "\" sudah dipakai siswa lain.");
        }

        String nisn = student.getNisn() == null ? "" : student.getNisn().trim();
        if (nisn.isEmpty()) {
            throw new IllegalArgumentException("NISN wajib diisi.");
        }
        if (!nisn.matches("\\d{10}")) {
            throw new IllegalArgumentException("NISN harus 10 digit angka.");
        }
        student.setNisn(nisn);
        if (studentDAO.isNisnTaken(nisn, student.getId())) {
            throw new IllegalStateException("NISN \"" + nisn + "\" sudah dipakai siswa lain.");
        }
    }

    @Override
    public void deleteStudent(int id, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        studentDAO.delete(id);
    }

    @Override
    public int deleteStudentsByClassroom(int classroomId, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        return studentDAO.deleteByClassroom(classroomId);
    }

    @Override
    public int[] countStudentExamData(int classroomId) {
        return studentDAO.countExamDataByClassroom(classroomId);
    }

    // Subject
    @Override
    public List<Subject> getAllSubjects() {
        return subjectDAO.getAllSubjects();
    }

    @Override
    public List<Subject> getSubjectBy(String keyword) {
        return subjectDAO.getSubjectBy(keyword);
    }

    @Override
    public List<Subject> getSubjects(int page, int size) {
        return subjectDAO.getSubjects(page, size);
    }

    @Override
    public int countSubjects() {
        return subjectDAO.countSubjects();
    }

    @Override
    public Subject getByIdSubject(int id) {
        return subjectDAO.getById(id);
    }

    @Override
    public void saveOrUpdateSubject(Subject subject, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        subject.setUpdatedBy(currentUsername);
        subjectDAO.saveOrUpdate(subject);
    }

    @Override
    public void deleteSubject(int id, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        deleteSafely(() -> subjectDAO.delete(id),
                "Mapel ini tidak bisa dihapus karena masih dipakai di jadwal ujian. Hapus atau ubah jadwal ujiannya terlebih dahulu.");
    }

    // Room
    @Override
    public List<Room> getAllRooms() {
        return roomDAO.getAllRooms();
    }

    @Override
    public List<Room> getRoomsBy(String keyword) {
        return roomDAO.getRoomBy(keyword);
    }

    @Override
    public List<Room> getRooms(int page, int size) {
        return roomDAO.getRooms(page, size);
    }

    @Override
    public int countRooms() {
        return roomDAO.countRooms();
    }

    @Override
    public Room getByIdRoom(int id) {
        return roomDAO.getById(id);
    }

    @Override
    public void saveOrUpdateRoom(Room room, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        if (room.getId() > 0) {
            List<SessionOccupancy> over = roomDAO.findUpcomingOverCapacity(room.getId(), room.getCapacity());
            if (!over.isEmpty()) {
                String sessions = over.stream().limit(3).map(SessionOccupancy::getLabel)
                        .collect(java.util.stream.Collectors.joining(", "));
                throw new IllegalArgumentException("Kapasitas tidak bisa diubah menjadi " + room.getCapacity()
                        + " karena ruangan ini sudah ditempati lebih banyak siswa pada: " + sessions
                        + (over.size() > 3 ? " dan " + (over.size() - 3) + " sesi lain" : "")
                        + ". Atur ulang Penempatan Ruangan sesi tersebut terlebih dahulu.");
            }
        }
        room.setUpdatedBy(currentUsername);
        roomDAO.saveOrUpdate(room);
    }

    @Override
    public void deleteRoom(int id, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        deleteSafely(() -> roomDAO.delete(id),
                "Ruangan ini tidak bisa dihapus karena masih dipakai di Penempatan Ruangan (kursi peserta atau pengawas). Pindahkan penempatannya terlebih dahulu.");
    }
    
    private void ensureIsAdmin(String currentUserRole) {
        if (!"admin".equalsIgnoreCase(currentUserRole)) {
            throw new SecurityException("Hanya admin yang boleh mengubah data master.");
        }
    }
}