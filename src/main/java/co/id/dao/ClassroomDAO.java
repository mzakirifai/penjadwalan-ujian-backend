package co.id.dao;

import co.id.model.Classroom;
import java.util.List;

public interface ClassroomDAO {
    public List<Classroom> getAllClassrooms();
    public List<Classroom> getClassroomBy(String keyword);
    public List<Classroom> getClassrooms(int page, int size);
    public int countClassrooms();
    public Classroom getById(int id);
    public void saveOrUpdate(Classroom classroom);
    public void delete(int id);
}
