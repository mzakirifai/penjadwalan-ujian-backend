package co.id.dao;

import co.id.model.Teacher;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Akses data penempatan ruangan (ruangan per peserta dan pengawas per ruangan). */
public interface RoomAllocationDAO {

    /** Baris peserta ringan (dengan NIS dan nama siswa) untuk sekumpulan jadwal. */
    public static class SeatRow {
        public int participantId;
        public int scheduleId;
        public int studentId;
        public String nis;
        public String studentName;
        public String participantNumber;
        public String seatNumber;
        public Integer roomId;
    }

    /** Satu kursi yang akan disimpan. participantId null = peserta baru. */
    public static class SeatWrite {
        public Integer participantId;
        public int scheduleId;
        public int studentId;
        public String participantNumber;
        public int roomId;
        public String seatNumber;
    }

    public List<SeatRow> getSeatRows(List<Integer> scheduleIds);

    /** roomId -> pengawas untuk sesi tersebut. */
    public Map<Integer, Teacher> getSupervisors(LocalDate date, LocalTime start, LocalTime end);

    /** Ruangan yang dipakai sesi LAIN yang waktunya bertumpuk (sesi dengan jam persis sama tidak dihitung). */
    public Set<Integer> getBusyRoomIds(LocalDate date, LocalTime start, LocalTime end);

    /** Seperti getBusyRoomIds, dengan keterangan pemakai: roomId -> mis. "X DKV (07:30-09:00)". */
    public Map<Integer, String> getRoomUsages(LocalDate date, LocalTime start, LocalTime end);
    /** Guru yang mengawasi sesi LAIN yang waktunya bertumpuk. */
    public Set<Integer> getBusyTeacherIds(LocalDate date, LocalTime start, LocalTime end);

    /**
     * Simpan pengawas hasil impor dalam satu transaksi: tiap (sesi, ruangan) di daftar diganti pengawasnya,
     * ruangan lain tidak disentuh. Bila satu gagal, semuanya dibatalkan.
     */
    public int saveSupervisors(List<co.id.model.SupervisorAssignment> assignments, String updatedBy);

    /** teacherId -> jumlah ruangan yang diawasi pada tanggal tersebut. */
    public Map<Integer, Integer> getSupervisionCounts(LocalDate date);

    /**
     * Simpan penempatan satu sesi dalam satu transaksi: kursi peserta, pengawas per ruangan, dan
     * mengosongkan ruangan/pengawas pada jadwal-jadwal sesi tersebut (kini diatur per peserta / per ruangan).
     */
    public void saveAllocation(LocalDate date, LocalTime start, LocalTime end, List<Integer> scheduleIds,
            List<SeatWrite> seats, Map<Integer, Integer> supervisorByRoom, String updatedBy);
}