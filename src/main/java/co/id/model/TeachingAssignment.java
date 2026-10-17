package co.id.model;

import java.time.LocalDateTime;

/**
 * Data pengampu: guru yang mengajar suatu mata pelajaran di suatu kelas.
 * Dipakai untuk menentukan guru mana yang boleh menginput nilai ujian.
 */
public class TeachingAssignment {
    private int id;
    private Teacher teacher;
    private Subject subject;
    private Classroom classroom;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public TeachingAssignment() {
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public Classroom getClassroom() { return classroom; }
    public void setClassroom(Classroom classroom) { this.classroom = classroom; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
