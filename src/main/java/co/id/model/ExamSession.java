package co.id.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Satu sesi ujian = semua jadwal pada tanggal dan jam mulai-selesai yang persis sama. */
public class ExamSession {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final List<ExamSchedule> schedules;

    public ExamSession(LocalDate date, LocalTime startTime, LocalTime endTime, List<ExamSchedule> schedules) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.schedules = schedules;
    }

    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public List<ExamSchedule> getSchedules() { return schedules; }

    @Override
    public String toString() {
        return startTime.format(TIME) + " - " + endTime.format(TIME) + "  (" + schedules.size() + " jadwal)";
    }
}