package co.id.service.impl;

import co.id.dao.ClassroomDAO;
import co.id.dao.MajorDAO;
import co.id.dao.RoomDAO;
import co.id.dao.StudentDAO;
import co.id.dao.SubjectDAO;
import co.id.dao.TeacherDAO;
import co.id.dao.impl.ClassroomDAOImpl;
import co.id.dao.impl.MajorDAOImpl;
import co.id.dao.impl.RoomDAOImpl;
import co.id.dao.impl.StudentDAOImpl;
import co.id.dao.impl.SubjectDAOImpl;
import co.id.dao.impl.TeacherDAOImpl;
import co.id.model.Classroom;
import co.id.model.Major;
import co.id.model.Room;
import co.id.model.Student;
import co.id.model.Subject;
import co.id.model.Teacher;
import co.id.service.MasterService;
import java.util.List;

public class MasterServiceImpl implements MasterService{
    private final TeacherDAO teacherDAO;
    private final MajorDAO majorDAO;
    private final ClassroomDAO classroomDAO;
    private final StudentDAO studentDAO;
    private final SubjectDAO subjectDAO;
    private final RoomDAO roomDAO;

    public MasterServiceImpl() {
        teacherDAO = new TeacherDAOImpl();
        majorDAO = new MajorDAOImpl();
        classroomDAO = new ClassroomDAOImpl();
        studentDAO = new StudentDAOImpl();
        subjectDAO = new SubjectDAOImpl();
        roomDAO = new RoomDAOImpl();
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
    public void saveOrUpdateTeacher(Teacher teacher, String currentUsername) {
        teacher.setUpdatedBy(currentUsername);
        teacherDAO.saveOrUpdate(teacher);
    }

    @Override
    public void deleteTeacher(int id) {
        teacherDAO.delete(id);
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
    public void saveOrUpdateMajor(Major major, String currentUsername) {
        major.setUpdatedBy(currentUsername);
        majorDAO.saveOrUpdate(major);
    }

    @Override
    public void deleteMajor(int id) {
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
    public void saveOrUpdateClassroom(Classroom classroom, String currentUsername) {
        classroom.setUpdatedBy(currentUsername);
        classroomDAO.saveOrUpdate(classroom);
    }

    @Override
    public void deleteClassroom(int id) {
        classroomDAO.delete(id);
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
    public void saveOrUpdateStudent(Student student, String currentUsername) {
        student.setUpdatedBy(currentUsername);
        studentDAO.saveOrUpdate(student);
    }

    @Override
    public void deleteStudent(int id) {
        studentDAO.delete(id);
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
    public void saveOrUpdateSubject(Subject subject, String currentUsername) {
        subject.setUpdatedBy(currentUsername);
        subjectDAO.saveOrUpdate(subject);
    }

    @Override
    public void deleteSubject(int id) {
        subjectDAO.delete(id);
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
    public void saveOrUpdateRoom(Room room, String currentUsername) {
        room.setUpdatedBy(currentUsername);
        roomDAO.saveOrUpdate(room);
    }

    @Override
    public void deleteRoom(int id) {
        roomDAO.delete(id);
    }
}
