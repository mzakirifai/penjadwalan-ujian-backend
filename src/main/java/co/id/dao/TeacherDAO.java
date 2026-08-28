package co.id.dao;

import co.id.model.Teacher;
import java.util.List;

public interface TeacherDAO {
    public List<Teacher> getAllTeachers();
    public List<Teacher> getTeacherBy(String keyword);
    public List<Teacher> getTeachers(int page, int size);
    public int countTeachers();
    public Teacher getById(int id);
    public void saveOrUpdate(Teacher teacher);
    public void delete(int id);
}
