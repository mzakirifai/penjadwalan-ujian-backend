package co.id.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** Satu pengawas untuk satu ruangan di satu sesi ujian (hasil impor jadwal pengawas). */
public class SupervisorAssignment {
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final Room room;
    private final Teacher teacher;

    public SupervisorAssignment(LocalDate date, LocalTime startTime, LocalTime endTime, Room room, Teacher teacher) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.teacher = teacher;
    }

    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Room getRoom() { return room; }
    public Teacher getTeacher() { return teacher; }
}