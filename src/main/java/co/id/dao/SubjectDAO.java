package co.id.dao;

import co.id.model.Subject;
import java.util.List;

public interface SubjectDAO {
    public List<Subject> getAllSubjects();
    public List<Subject> getSubjectBy(String keyword);
    public List<Subject> getSubjects(int page, int size);
    public int countSubjects();
    public Subject getById(int id);
    public void saveOrUpdate(Subject subject);
    public void delete(int id);
}
