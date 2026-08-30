package co.id.dao;

import co.id.model.Student;
import java.util.List;

public interface StudentDAO {
    public List<Student> getAllStudents();
    public List<Student> getStudentBy(String keyword);
    public List<Student> getStudents(int page, int size);
    public int countStudents();
    public Student getById(int id);
    public List<Student> getByClassroom(int classroomId);
    public void saveOrUpdate(Student student);
    public void delete(int id);
}
