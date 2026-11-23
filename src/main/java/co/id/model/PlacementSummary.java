package co.id.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Ringkasan penempatan ruangan satu jadwal ujian untuk ditampilkan di daftar jadwal:
 * ruangan mana saja yang dipakai (beserta jumlah siswanya), siapa pengawasnya, dan statusnya.
 * Sumber data: trx_peserta.id_ruangan + trx_pengawas_ruangan (hasil menu Penempatan Ruangan).
 */
public class PlacementSummary {

    public enum Status {
        SIAP("Siap"),
        TANPA_PENGAWAS("Tanpa pengawas"),
        SEBAGIAN("Sebagian ditempatkan"),
        BELUM("Belum ditempatkan"),
        TANPA_SISWA("Kelas tanpa siswa"),
        ONLINE("Online (tanpa ruangan)");

        private final String label;

        Status(String label) { this.label = label; }

        public String getLabel() { return label; }
    }

    /** Satu ruangan yang dipakai jadwal ini. */
    public static class RoomUsage {
        private final String code;
        private final String name;
        private final int studentCount;
        private final String supervisorName; // null jika ruangan ini belum punya pengawas

        public RoomUsage(String code, String name, int studentCount, String supervisorName) {
            this.code = code;
            this.name = name;
            this.studentCount = studentCount;
            this.supervisorName = supervisorName;
        }

        public String getCode() { return code; }
        public String getName() { return name; }
        public int getStudentCount() { return studentCount; }
        public String getSupervisorName() { return supervisorName; }

        /** Kode ruangan bila ada, agar kolom tetap ringkas; jika kosong pakai nama. */
        public String getShortName() {
            return code != null && !code.isBlank() ? code : name;
        }
    }

    private final int scheduleId;
    private final List<RoomUsage> rooms = new ArrayList<>();
    private int classStudentCount;   // jumlah siswa di kelas jadwal ini
    private int unplacedCount;       // siswa kelas yang belum mendapat kursi
    private boolean online;          // ujian online: tidak memakai ruangan & pengawas

    public PlacementSummary(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public int getScheduleId() { return scheduleId; }
    public List<RoomUsage> getRooms() { return rooms; }
    public int getClassStudentCount() { return classStudentCount; }
    public void setClassStudentCount(int classStudentCount) { this.classStudentCount = classStudentCount; }
    public int getUnplacedCount() { return unplacedCount; }
    public void setUnplacedCount(int unplacedCount) { this.unplacedCount = unplacedCount; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public int getRoomsWithoutSupervisor() {
        return (int) rooms.stream().filter(r -> r.getSupervisorName() == null).count();
    }

    public Status getStatus() {
        if (online) {
            return Status.ONLINE;
        }
        if (rooms.isEmpty()) {
            return classStudentCount == 0 ? Status.TANPA_SISWA : Status.BELUM;
        }
        if (unplacedCount > 0) {
            return Status.SEBAGIAN;
        }
        if (getRoomsWithoutSupervisor() > 0) {
            return Status.TANPA_PENGAWAS;
        }
        return Status.SIAP;
    }

    /** Teks kolom Status, mis. "Sebagian ditempatkan (3 siswa belum)". */
    public String getStatusLabel() {
        Status status = getStatus();
        String label = status.getLabel();
        if (status == Status.SEBAGIAN) {
            label += " (" + unplacedCount + " siswa belum)";
        } else if (status == Status.TANPA_PENGAWAS && rooms.size() > 1) {
            label += " (" + getRoomsWithoutSupervisor() + " ruangan)";
        }
        return label;
    }

    /** Teks kolom Ruangan, mis. "R004 (20), R001 (14)"; jadwal lama cukup nama ruangannya. */
    public String getRoomsLabel() {
        if (online) {
            return "Online";
        }
        if (rooms.isEmpty()) {
            return "Belum ditempatkan";
        }
        return rooms.stream()
                .map(r -> r.getShortName() + " (" + r.getStudentCount() + ")")
                .collect(Collectors.joining(", "));
    }

    /** Rincian untuk tooltip kolom Ruangan. */
    public String getRoomsDetail() {
        if (online) {
            return "Ujian online: tidak memakai ruangan dan pengawas.";
        }
        if (rooms.isEmpty()) {
            return "Atur di menu Transaksi > Penempatan Ruangan.";
        }
        return rooms.stream()
                .map(r -> r.getShortName() + " - " + r.getName()
                        + ": " + r.getStudentCount() + " siswa"
                        + ", pengawas: " + (r.getSupervisorName() != null ? r.getSupervisorName() : "belum ada"))
                .collect(Collectors.joining("\n"));
    }

    /** Teks kolom Pengawas: nama-nama pengawas (tanpa duplikat), atau "-" bila belum ada. */
    public String getSupervisorsLabel() {
        if (online) {
            return "-";
        }
        Set<String> names = new LinkedHashSet<>();
        for (RoomUsage room : rooms) {
            if (room.getSupervisorName() != null) {
                names.add(room.getSupervisorName());
            }
        }
        if (names.isEmpty()) {
            return rooms.isEmpty() ? "-" : "Belum ada pengawas";
        }
        return String.join(", ", names);
    }
}