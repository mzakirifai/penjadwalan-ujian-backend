package co.id.model;

import java.time.LocalDateTime;

public class ExamScore {
    private int id;
    private String code;
    private int score;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    
    // Relasi Many-to-one
    private ExamSchedule examSchedule;
    private Student student;

    public ExamScore() {
    }

    public ExamScore(int id, String code, int score, LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy, ExamSchedule examSchedule, Student student) {
        this.id = id;
        this.code = code;
        this.score = score;
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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
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
        return "Nilai Ujian[id=" + id + ", kode_nilai=" + code + ", nilai=" + score
                + ", tanggal_buat=" + createdAt + ", tanggal_update=" + updatedAt 
                + ", diubah_oleh=" + updatedBy + "]";
    }
}
