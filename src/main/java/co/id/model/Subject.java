package co.id.model;

import java.time.LocalDateTime;

public class Subject {
    private int id;
    private String code;
    private String name;
    private String grade;
    private String type;
    private int passingGrade;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    
    // Relasi Many-to-one
    private Major major;

    public Subject() {
    }

    public Subject(int id, String code, String name, String grade, String type, int passingGrade, LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy, Major major) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.grade = grade;
        this.type = type;
        this.passingGrade = passingGrade;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getPassingGrade() {
        return passingGrade;
    }

    public void setPassingGrade(int passingGrade) {
        this.passingGrade = passingGrade;
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
        return "Mapel[id ="+id+", kode_mapel ="+code+", nama_mapel ="+name+", tingkat ="+grade+", jenis ="+type+", kkm ="+passingGrade+", tanggal_buat ="+createdAt+", tanggal_update ="+updatedAt+", diubah_oleh="+updatedBy+"]";
    }
}
