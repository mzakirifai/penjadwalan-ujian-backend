package co.id.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ExamSchedule {
    private int id;
    private String code;
    private String examType;
    private String semester;
    private String academicYear;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    
    // Relasi Many-to-one
    private Subject subject;
    private Classroom classroom;
    private Room room;
    private Teacher teacher;

    public ExamSchedule() {
    }

    public ExamSchedule(int id, String code, String examType, String semester, String academicYear, LocalDate date, LocalTime startTime, LocalTime endTime, String notes, LocalDateTime createdAt, LocalDateTime updatedAt, String updatedBy, Subject subject, Classroom classroom, Room room, Teacher teacher) {
        this.id = id;
        this.code = code;
        this.examType = examType;
        this.semester = semester;
        this.academicYear = academicYear;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.updatedBy = updatedBy;
        this.subject = subject;
        this.classroom = classroom;
        this.room = room;
        this.teacher = teacher;
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

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public Classroom getClassroom() {
        return classroom;
    }

    public void setClassroom(Classroom classroom) {
        this.classroom = classroom;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
    }
    
    @Override
    public String toString() {
        return "Jadwal Ujian[id=" + id + ", kode_ujian=" + code + ", jenis_ujian=" + examType
                + ", semester=" + semester + ", tahun_akademik=" + academicYear
                + ", tanggal=" + date + ", jam_mulai=" + startTime + ", jam_selesai=" + endTime
                + ", keterangan=" + notes + ", tanggal_buat=" + createdAt
                + ", tanggal_update=" + updatedAt + ", diubah_oleh=" + updatedBy + "]";
    }
}
