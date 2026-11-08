package co.id.model;

import java.time.LocalDateTime;

public class Student {
    private int id;
    private String nis;
    private String nisn;
    private String name;
    private String gender;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    
    // Relasi Many-to-one
    private Classroom classroom;

    public Student() {
    }

    public Student(int id, String nis, String nisn, String name, String gender, LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy, Classroom classroom) {
        this.id = id;
        this.nis = nis;
        this.nisn = nisn;
        this.name = name;
        this.gender = gender;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.classroom = classroom;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNis() {
        return nis;
    }

    public void setNis(String nis) {
        this.nis = nis;
    }

    public String getNisn() {
        return nisn;
    }

    public void setNisn(String nisn) {
        this.nisn = nisn;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
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

    public Classroom getClassroom() {
        return classroom;
    }

    public void setClassroom(Classroom classroom) {
        this.classroom = classroom;
    }

    
    
    @Override
    public String toString(){
        return "Siswa[id ="+id+", nis ="+nis+", nisn ="+nisn+", nama_siswa ="+name+", jenis_kelamin ="+gender+", tanggal_buat ="+createdAt+", tanggal_update ="+updatedAt+", dibuat_oleh="+updatedBy+"]";
    }
}