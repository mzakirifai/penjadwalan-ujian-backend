package co.id.model;

import java.time.format.DateTimeFormatter;

/**
 * Satu bentrokan KELAS antara jadwal yang akan disimpan dan jadwal lain yang sudah ada.
 * Bentrok ruangan & pengawas tidak lagi dicek di sini: keduanya diatur per sesi di Penempatan Ruangan.
 */
public class ScheduleConflict {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ExamSchedule schedule; // jadwal lain milik kelas yang sama

    public ScheduleConflict(ExamSchedule schedule) {
        this.schedule = schedule;
    }

    public ExamSchedule getSchedule() { return schedule; }

    /** Kalimat siap tampil, mis. "Kelas X TKJ 1 sudah punya ujian DDPK TKJ (1), 07:00 - 08:30". */
    public String describe() {
        String classroom = schedule.getClassroom() != null ? schedule.getClassroom().getName() : "-";
        String subject = schedule.getSubjectLabel();
        String time = (schedule.getStartTime() != null ? schedule.getStartTime().format(TIME) : "?")
                + " - " + (schedule.getEndTime() != null ? schedule.getEndTime().format(TIME) : "?");

        return "Kelas " + classroom + " sudah punya ujian " + subject + ", " + time;
    }
}