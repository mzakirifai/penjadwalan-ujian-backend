package co.id.service;

import co.id.model.ExamSession;
import co.id.model.Room;
import co.id.model.RoomPlan;
import java.time.LocalDate;
import java.util.List;

public interface RoomAllocationService {
    /** Sesi ujian pada tanggal tersebut (jadwal dengan jam mulai-selesai yang sama dijadikan satu sesi). */
    public List<ExamSession> getSessions(LocalDate date);

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