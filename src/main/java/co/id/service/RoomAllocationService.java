package co.id.service;

import co.id.model.ExamSession;
import co.id.model.Room;
import co.id.model.RoomPlan;
import java.time.LocalDate;
import java.util.List;

public interface RoomAllocationService {
    /** Sesi ujian pada tanggal tersebut (jadwal dengan jam mulai-selesai yang sama dijadikan satu sesi). */
    public List<ExamSession> getSessions(LocalDate date);

    /** Sesi ujian Online pada tanggal itu (dikelompokkan per jam yang sama); tidak memakai ruangan. */
    public List<ExamSession> getOnlineSessions(LocalDate date);

    /** Sesi offline (ruangan) yang memuat jadwal periode ini: jenis ujian + semester + tahun akademik. */
    public List<ExamSession> getSessionsInPeriod(String examType, String semester, String academicYear);

    /** Jumlah siswa yang perlu kursi per jadwal (id jadwal -> jumlah), sama dengan hitungan mode otomatis. */
    public java.util.Map<Integer, Integer> getStudentCounts(ExamSession session);

    /**
     * Penempatan manual. distribution: id jadwal -> (id ruangan -> jumlah siswa), urutan ruangan = urutan pengisian.
     * Siswa tiap jadwal diurut nama lalu dibagi sesuai jumlah; kursi di tiap ruangan disebar seperti mode otomatis.
     */
    public RoomPlan buildManual(ExamSession session, java.util.Map<Integer, java.util.Map<Integer, Integer>> distribution,
                                List<Room> rooms);

    /** Pindahkan satu siswa ke ruangan lain di rencana ini (kursi kedua ruangan disusun ulang, nomor peserta tetap). */
    public void moveSeat(RoomPlan plan, co.id.model.SeatAssignment seat, Room target);

    /**
     * Terapkan susunan kelas -> ruangan dari rencana sumber ke sesi-sesi lain, lalu langsung disimpan.
     * Pengawas sesi tujuan yang sudah ada dipertahankan untuk ruangan yang sama.
     * @return satu baris hasil per sesi (berhasil / dilewati beserta alasannya)
     */
    public List<String> applyLayout(RoomPlan source, List<ExamSession> targets, String currentUsername, String currentUserRole);

    /** Semua ruangan beserta keterangan bila sedang dipakai sesi lain yang waktunya bertumpuk. */
    public List<co.id.model.RoomAvailability> getRoomAvailability(ExamSession session);
    /** Jumlah kursi yang dibutuhkan sesi ini (seluruh siswa kelas + peserta terdaftar per jadwal). */
    public int countSeatsNeeded(ExamSession session);
    /** Ruangan kosong yang dipilih otomatis: sesedikit mungkin, kursi kosong paling sedikit. */
    public List<Room> suggestRooms(ExamSession session);

    /** Membuat penempatan campur untuk sesi ini ke ruangan-ruangan terpilih. Belum disimpan. */
    public RoomPlan generate(ExamSession session, List<Room> rooms);

    /** Penempatan yang sudah tersimpan untuk sesi ini; null jika belum ada. */
    public RoomPlan loadSaved(ExamSession session);

    /** Mengisi pengawas tiap ruangan: yang tidak bertugas di sesi bertumpuk dan bukan pengampu mapel di ruangan itu. */
    public void assignSupervisorsAutomatically(RoomPlan plan);

    public void save(RoomPlan plan, String currentUsername, String currentUserRole);
}