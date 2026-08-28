package co.id.model;

import java.time.LocalDateTime;

public class ExamParticipant {
    private int id;
    private String code;
    private String participantNumber;
    private String seatNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    
    // Relasi Many-to-one
    private ExamSchedule examSchedule;
    private Student student;

    public ExamParticipant() {
    }

    public ExamParticipant(int id, String code, String participantNumber, String seatNumber, LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy, ExamSchedule examSchedule, Student student) {
        this.id = id;
        this.code = code;
        this.participantNumber = participantNumber;
        this.seatNumber = seatNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.examSchedule = examSchedule;
        this.student = student;
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

    public String getParticipantNumber() {
        return participantNumber;
    }

    public void setParticipantNumber(String participantNumber) {
        this.participantNumber = participantNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
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

    public ExamSchedule getExamSchedule() {
        return examSchedule;
    }

    public void setExamSchedule(ExamSchedule examSchedule) {
        this.examSchedule = examSchedule;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }
    
    @Override
    public String toString() {
        return "Peserta Ujian[id=" + id + ", kode_peserta=" + code + ", no_peserta=" + participantNumber
                + ", no_kursi=" + seatNumber + ", tanggal_buat=" + createdAt
                + ", tanggal_update=" + updatedAt + ", diubah_oleh=" + updatedBy + "]";
    }
}
