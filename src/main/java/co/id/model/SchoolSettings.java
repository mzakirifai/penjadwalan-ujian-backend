package co.id.model;

import java.time.LocalDateTime;

/**
 * Representasi tabel `pengaturan` — cuma ada 1 baris (id selalu 1).
 * Nampung profil sekolah (dipakai di kop surat semua laporan) dan
 * periode akademik aktif (default Semester/Tahun Akademik di layar
 * yang butuh filter periode, biar gak diketik manual tiap kali).
 */
public class SchoolSettings {
    private int id;
    private String schoolName;
    private String address;
    private String phone;
    private String email;
    private String principalName;
    private String principalNip;
    private String activeSemester;
    private String activeAcademicYear;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public SchoolSettings() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPrincipalName() {
        return principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    public String getPrincipalNip() {
        return principalNip;
    }

    public void setPrincipalNip(String principalNip) {
        this.principalNip = principalNip;
    }

    public String getActiveSemester() {
        return activeSemester;
    }

    public void setActiveSemester(String activeSemester) {
        this.activeSemester = activeSemester;
    }

    public String getActiveAcademicYear() {
        return activeAcademicYear;
    }

    public void setActiveAcademicYear(String activeAcademicYear) {
        this.activeAcademicYear = activeAcademicYear;
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
}