package co.id.service;

import co.id.model.User;
import java.util.List;

public interface UserService {
    public List<User> getAllUsers();
    public List<User> getUsers(int page, int size);
    public int countUsers();
    public User getById(int id);
    public List<User> getByKeyword(String keyword);
    public User login(String username, String rawPassword);
    public void register(User user, String rawPassword, String currentUsername);
    public void changePassword(int userId, String oldRawPassword, String newRawPassword);
    public void resetPassword(int userId, String newRawPassword, String currentUsername);
    public void updateProfile(User user, String currentUsername);
    public void delete(int id);
}
