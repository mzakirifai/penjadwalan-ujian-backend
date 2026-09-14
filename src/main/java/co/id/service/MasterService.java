    package co.id.service;

import co.id.model.Classroom;
import co.id.model.Major;
import co.id.model.Room;
import co.id.model.Student;
import co.id.model.Subject;
import co.id.model.Teacher;
import java.util.List;

public interface MasterService {
    // Teacher
    public List<Teacher> getAllTeachers();
    public List<Teacher> getTeacherBy(String keyword);
    public List<Teacher> getTeachers(int page, int size);
    public int countTeachers();
    public Teacher getByIdTeacher(int id);
    public void saveOrUpdateTeacher(Teacher teacher, String currentUsername, String currentUserRole);
    public void deleteTeacher(int id, String currentUserRole);
    
    // Major
    public List<Major> getAllMajors();
    public List<Major> getMajorBy(String keyword);
    public List<Major> getMajors(int page, int size);
    public int countMajors();
    public Major getByIdMajor(int id);
    public void saveOrUpdateMajor(Major major, String currentUsername, String currentUserRole);
    public void deleteMajor(int id, String currentUserRole);
    
    // Classroom
    public List<Classroom> getAllClassrooms();
    public List<Classroom> getClassroomBy(String keyword);
    public List<Classroom> getClassrooms(int page, int size);
    public int countClassrooms();
    public Classroom getByIdClassroom(int id);
    public void saveOrUpdateClassroom(Classroom classroom, String currentUsername, String currentUserRole);
    public void deleteClassroom(int id, String currentUserRole);
    
    // Student
    public List<Student> getAllStudents();
    public List<Student> getStudentBy(String keyword);
    public List<Student> getStudents(int page, int size);
    public int countStudents();
    public Student getByIdStudent(int id);
    public List<Student> getStudentsByClassroom(int classroomId);
    public void saveOrUpdateStudent(Student student, String currentUsername, String currentUserRole);
    public void deleteStudent(int id, String currentUserRole);
    
    // Subject
    public List<Subject> getAllSubjects();
    public List<Subject> getSubjectBy(String keyword);
    public List<Subject> getSubjects(int page, int size);
    public int countSubjects();
    public Subject getByIdSubject(int id);
    public void saveOrUpdateSubject(Subject subject, String currentUsername, String currentUserRole);
    public void deleteSubject(int id, String currentUserRole);
    
    // Room
    public List<Room> getAllRooms();
    public List<Room> getRoomsBy(String keyword);
    public List<Room> getRooms(int page, int size);
    public int countRooms();
    public Room getByIdRoom(int id);
    public void saveOrUpdateRoom(Room room, String currentUsername, String currentUserRole);
    public void deleteRoom(int id, String currentUserRole);
}
