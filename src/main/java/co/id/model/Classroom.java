package co.id.model;

import java.time.LocalDateTime;

public class Classroom {
    private int id;
    private String code;
    private String name;
    private String grade;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    
    // Relasi Many-to-one
    private Major major;

    public Classroom() {
    }

    public Classroom(int id, String code, String name, String grade, LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy, Major major) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.grade = grade;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.major = major;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
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

    public Major getMajor() {
        return major;
    }

    public void setMajor(Major major) {
        this.major = major;
    }
    
    @Override
    public String toString(){
        return "Kelas[id ="+id+", kode_kelas ="+code+", nama_kelas ="+name+", tingkat ="+grade+", tanggal_buat ="+createdAt+", tanggal_update ="+updatedAt+", dibuat_oleh="+updatedBy+"]";
    }
}
