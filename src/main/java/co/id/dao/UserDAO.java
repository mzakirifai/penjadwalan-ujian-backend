package co.id.dao;

import co.id.model.User;
import java.util.List;

public interface UserDAO {
    public List<User> getAllUsers();
    public List<User> getUsers(int page, int size);
    public int countUsers();
    public User getById(int id);
    public User getByUsername(String username);
    public List<User> getByKeyword(String keyword);
    public void saveOrUpdate(User user);
    public void updatePassword(int id, String hashedPassword);
    public void delete(int id);
    public boolean isUsernameTaken(String username, int excludeId);
    public boolean isTeacherLinked(int teacherId, int excludeId);
}
