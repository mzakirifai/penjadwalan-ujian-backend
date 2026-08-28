package co.id.model;

import java.time.LocalDateTime;

public class User {
    private int id;
    private String username;
    private String password;
    private String fullName;
    private String role;
    private String photoPath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    
    // Relasi Many-to-one (nullable untuk role admin)
    private Teacher teacher;

    public User() {
    }

    public User(int id, String username, String password, String fullName, String role, String photoPath, LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy, Teacher teacher) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.photoPath = photoPath;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.teacher = teacher;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }
    
    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username + ", full_name=" + fullName + ", role=" + role
                + ", photo_path=" + photoPath + ", tanggal_buat=" + createdAt
                + ", tanggal_update=" + updatedAt + ", diubah_oleh=" + updatedBy + "]";
    }
}
