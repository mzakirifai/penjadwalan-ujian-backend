package co.id.dao;

import co.id.model.Student;
import java.util.List;
import java.util.Set;

public interface StudentDAO {
    public List<Student> getAllStudents();
    public List<Student> getStudentBy(String keyword);
    public List<Student> getStudents(int page, int size);
    public int countStudents();
    public Student getById(int id);
    public List<Student> getByClassroom(int classroomId);
    public boolean isNisTaken(String nis, int excludeId);
    public boolean isNisnTaken(String nisn, int excludeId);
    public Set<String> getAllNis();
    public Set<String> getAllNisn();
    public int saveBatch(List<Student> students);
    public void saveOrUpdate(Student student);
    public void delete(int id);
}