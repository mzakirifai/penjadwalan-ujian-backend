package co.id.model;

import java.time.LocalDateTime;

public class Student {
    private int id;
    private String nis;
    private String name;
    private String gender;
    private String phoneNumber;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    
    // Relasi Many-to-one
    private Classroom classroom;

    public Student() {
    }

    public Student(int id, String nis, String name, String gender, String phoneNumber, String address, LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy, Classroom classroom) {
        this.id = id;
        this.nis = nis;
        this.name = name;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.address = address;
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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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
        return "Siswa[id ="+id+", nis ="+nis+", nama_siswa ="+name+", jenis_kelamin ="+gender+", nomor_hp ="+phoneNumber+", alamat ="+address+", tanggal_buat ="+createdAt+", tanggal_update ="+updatedAt+", dibuat_oleh="+updatedBy+"]";
    }
}
