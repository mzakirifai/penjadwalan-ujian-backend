package co.id.service.impl;

import co.id.dao.UserDAO;
import co.id.dao.impl.UserDAOImpl;
import co.id.model.User;
import co.id.service.UserService;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;

public class UserServiceImpl implements UserService{
    
    private final UserDAO userDAO = new UserDAOImpl();
    
    @Override
    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    @Override
    public List<User> getUsers(int page, int size) {
        return userDAO.getUsers(page, size);
    }

    @Override
    public int countUsers() {
        return userDAO.countUsers();
    }

    @Override
    public User getById(int id) {
        return userDAO.getById(id);
    }

    @Override
    public List<User> getByKeyword(String keyword) {
        return userDAO.getByKeyword(keyword);
    }

    @Override
    public User login(String username, String rawPassword) {
        User user = userDAO.getByUsername(username);

        if (user == null) {
            return null;
        }

        boolean isPasswordMatch = BCrypt.checkpw(rawPassword, user.getPassword());
        return isPasswordMatch ? user : null;
    }

    @Override
    public void register(User user, String rawPassword, String currentUsername) {
        validate(user, rawPassword);

        if (userDAO.isUsernameTaken(user.getUsername(), 0)) {
            throw new IllegalStateException("Username \"" + user.getUsername() + "\" sudah dipakai.");
        }

        if ("guru".equalsIgnoreCase(user.getRole())
                && userDAO.isTeacherLinked(user.getTeacher().getId(), 0)) {
            throw new IllegalStateException("Guru ini sudah memiliki akun.");
        }

        String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());
        user.setPassword(hashedPassword);
        user.setUpdatedBy(currentUsername);

        userDAO.saveOrUpdate(user);
    }

    @Override
    public void changePassword(int userId, String oldRawPassword, String newRawPassword) {
        User user = userDAO.getById(userId);

        if (user == null) {
            throw new IllegalArgumentException("User tidak ditemukan.");
        }

        boolean isOldPasswordMatch = BCrypt.checkpw(oldRawPassword, user.getPassword());
        if (!isOldPasswordMatch) {
            throw new IllegalStateException("Password lama tidak sesuai.");
        }

        if (newRawPassword == null || newRawPassword.length() < 6) {
            throw new IllegalArgumentException("Password baru minimal 6 karakter.");
        }

        String hashedNewPassword = BCrypt.hashpw(newRawPassword, BCrypt.gensalt());
        userDAO.updatePassword(userId, hashedNewPassword);
    }

    @Override
    public void resetPassword(int userId, String newRawPassword, String currentUsername) {
        User user = userDAO.getById(userId);

        if (user == null) {
            throw new IllegalArgumentException("User tidak ditemukan.");
        }

        if (newRawPassword == null || newRawPassword.length() < 6) {
            throw new IllegalArgumentException("Password baru minimal 6 karakter.");
        }

        String hashedPassword = BCrypt.hashpw(newRawPassword, BCrypt.gensalt());
        userDAO.updatePassword(userId, hashedPassword);
    }

    @Override
    public void updateProfile(User user, String currentUsername) {
        validate(user, null);

        if (userDAO.isUsernameTaken(user.getUsername(), user.getId())) {
            throw new IllegalStateException("Username \"" + user.getUsername() + "\" sudah dipakai.");
        }

        if ("guru".equalsIgnoreCase(user.getRole())
                && userDAO.isTeacherLinked(user.getTeacher().getId(), user.getId())) {
            throw new IllegalStateException("Guru ini sudah memiliki akun lain.");
        }

        user.setUpdatedBy(currentUsername);
        userDAO.saveOrUpdate(user);
    }

    @Override
    public void delete(int id) {
        User existing = userDAO.getById(id);
        if (existing == null) {
            throw new IllegalArgumentException("User tidak ditemukan.");
        }

        userDAO.delete(id);
    }
    
    private void validate(User user, String rawPassword) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username wajib diisi.");
        }
        if (rawPassword != null && rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password minimal 6 karakter.");
        }
        if (user.getRole() == null
                || (!user.getRole().equalsIgnoreCase("admin") && !user.getRole().equalsIgnoreCase("guru"))) {
            throw new IllegalArgumentException("Role harus \"admin\" atau \"guru\".");
        }
        if ("admin".equalsIgnoreCase(user.getRole()) && user.getTeacher() != null) {
            throw new IllegalArgumentException("Akun admin tidak boleh terhubung ke data guru.");
        }
        if ("guru".equalsIgnoreCase(user.getRole()) && user.getTeacher() == null) {
            throw new IllegalArgumentException("Akun guru wajib dihubungkan ke data guru.");
        }
    }
}
