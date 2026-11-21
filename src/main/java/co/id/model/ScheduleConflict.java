package co.id.model;

import java.time.format.DateTimeFormatter;

/** Satu bentrokan antara jadwal yang akan disimpan dan jadwal lain yang sudah ada. */
public class ScheduleConflict {

    public enum Resource { RUANGAN, KELAS, PENGAWAS }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final Resource resource;
    private final ExamSchedule schedule; // jadwal lain yang sudah memakai sumber daya tersebut

    public ScheduleConflict(Resource resource, ExamSchedule schedule) {
        this.resource = resource;
        this.schedule = schedule;
    }

    public Resource getResource() { return resource; }
    public ExamSchedule getSchedule() { return schedule; }

    /** Kalimat siap tampil, mis. "Ruangan Lab 1 sudah dipakai X TKJ 1 (DDPK TKJ (1)), 07:00 - 08:30". */
    public String describe() {
        String classroom = schedule.getClassroom() != null ? schedule.getClassroom().getName() : "-";
        String subject = schedule.getSubjectLabel();
        String time = (schedule.getStartTime() != null ? schedule.getStartTime().format(TIME) : "?")
                + " - " + (schedule.getEndTime() != null ? schedule.getEndTime().format(TIME) : "?");

        switch (resource) {
            case RUANGAN:
                return "Ruangan " + (schedule.getRoom() != null ? schedule.getRoom().getName() : "-")
                        + " sudah dipakai " + classroom + " (" + subject + "), " + time;
            case KELAS:
                return "Kelas " + classroom + " sudah punya ujian " + subject + ", " + time;
            default:
                return "Pengawas " + (schedule.getTeacher() != null ? schedule.getTeacher().getName() : "-")
                        + " sudah mengawasi " + classroom + " (" + subject + "), " + time;
        }
    }
}