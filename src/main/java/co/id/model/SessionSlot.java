package co.id.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Satu sesi ujian pada suatu tanggal: rentang jam yang sama persis, beserta jumlah jadwal di dalamnya. */
public class SessionSlot {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int scheduleCount;

    public SessionSlot(LocalTime startTime, LocalTime endTime, int scheduleCount) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.scheduleCount = scheduleCount;
    }

    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getScheduleCount() { return scheduleCount; }

    /** Mis. "08:00 - 09:30". */
    public String getLabel() {
        return startTime.format(TIME) + " - " + endTime.format(TIME);
    }
}