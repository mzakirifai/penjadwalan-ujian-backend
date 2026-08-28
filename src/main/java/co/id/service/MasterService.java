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
    public void saveOrUpdateTeacher(Teacher teacher, String currentUsername);
    public void deleteTeacher(int id);
    
    // Major
    public List<Major> getAllMajors();
    public List<Major> getMajorBy(String keyword);
    public List<Major> getMajors(int page, int size);
    public int countMajors();
    public Major getByIdMajor(int id);
    public void saveOrUpdateMajor(Major major, String currentUsername);
    public void deleteMajor(int id);
    
    // Classroom
    public List<Classroom> getAllClassrooms();
    public List<Classroom> getClassroomBy(String keyword);
    public List<Classroom> getClassrooms(int page, int size);
    public int countClassrooms();
    public Classroom getByIdClassroom(int id);
    public void saveOrUpdateClassroom(Classroom classroom, String currentUsername);
    public void deleteClassroom(int id);
    
    // Student
    public List<Student> getAllStudents();
    public List<Student> getStudentBy(String keyword);
    public List<Student> getStudents(int page, int size);
    public int countStudents();
    public Student getByIdStudent(int id);
    public void saveOrUpdateStudent(Student student, String currentUsername);
    public void deleteStudent(int id);
    
    // Subject
    public List<Subject> getAllSubjects();
    public List<Subject> getSubjectBy(String keyword);
    public List<Subject> getSubjects(int page, int size);
    public int countSubjects();
    public Subject getByIdSubject(int id);
    public void saveOrUpdateSubject(Subject subject, String currentUsername);
    public void deleteSubject(int id);
    
    // Room
    public List<Room> getAllRooms();
    public List<Room> getRoomsBy(String keyword);
    public List<Room> getRooms(int page, int size);
    public int countRooms();
    public Room getByIdRoom(int id);
    public void saveOrUpdateRoom(Room room, String currentUsername);
    public void deleteRoom(int id);
}
