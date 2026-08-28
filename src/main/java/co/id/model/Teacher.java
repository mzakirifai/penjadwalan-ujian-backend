package co.id.model;

import java.time.LocalDateTime;

public class Teacher {
    private int id;
    private String nip;
    private String name;
    private String gender;
    private String phoneNumber;
    private String email;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public Teacher() {
    }

    public Teacher(int id, String nip, String name, String gender, String phoneNumber, String email, String address, LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy) {
        this.id = id;
        this.nip = nip;
        this.name = name;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNip() {
        return nip;
    }

    public void setNip(String nip) {
        this.nip = nip;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    
    
    @Override
    public String toString(){
        return "Guru[id ="+id+", nip ="+nip+", nama ="+name+", jenis_kelamin ="+gender+", nomor_hp ="+phoneNumber+", email ="+email+", alamat ="+address+", tanggal_buat ="+createdAt+", tanggal_update ="+updatedAt+", diubah_oleh="+updatedBy+"]";
    }
}
