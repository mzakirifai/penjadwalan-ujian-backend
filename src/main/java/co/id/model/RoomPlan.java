package co.id.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Penempatan siswa ke ruangan untuk satu sesi ujian (tanggal + jam mulai + jam selesai yang sama). */
public class RoomPlan {
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final List<ExamSchedule> schedules;
    private final List<RoomAssignment> rooms = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public RoomPlan(LocalDate date, LocalTime startTime, LocalTime endTime, List<ExamSchedule> schedules) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.schedules = schedules;
    }

    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public List<ExamSchedule> getSchedules() { return schedules; }
    public List<RoomAssignment> getRooms() { return rooms; }
    public List<String> getWarnings() { return warnings; }

    public int getTotalStudents() {
        return rooms.stream().mapToInt(RoomAssignment::getFilled).sum();
    }
}