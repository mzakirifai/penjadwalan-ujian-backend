package co.id.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Jumlah kursi terisi di satu ruangan pada satu sesi (tanggal + jam). */
public class SessionOccupancy {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final int seats;

    public SessionOccupancy(LocalDate date, LocalTime startTime, LocalTime endTime, int seats) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.seats = seats;
    }

    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getSeats() { return seats; }

    /** Mis. "16-11-2026 08:00 - 09:30 (34 siswa)". */
    public String getLabel() {
        return date.format(DATE) + " " + startTime.format(TIME) + " - " + endTime.format(TIME) + " (" + seats + " siswa)";
    }
}