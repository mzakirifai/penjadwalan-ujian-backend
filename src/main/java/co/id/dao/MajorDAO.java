package co.id.dao;

import co.id.model.Major;
import java.util.List;

public interface MajorDAO {
    public List<Major> getAllMajors();
    public List<Major> getMajorBy(String keyword);
    public List<Major> getMajors(int page, int size);
    public int countMajors();
    public Major getById(int id);
    public void saveOrUpdate(Major major);
    public void delete(int id);
}
