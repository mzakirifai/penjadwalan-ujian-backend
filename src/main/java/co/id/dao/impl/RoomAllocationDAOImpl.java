package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.RoomAllocationDAO;
import co.id.model.Teacher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RoomAllocationDAOImpl extends DatabaseConfiguration implements RoomAllocationDAO {

    @Override
    public List<SeatRow> getSeatRows(List<Integer> scheduleIds) {
        List<SeatRow> rows = new ArrayList<>();
        if (scheduleIds.isEmpty()) {
            return rows;
        }

        String sql = "SELECT ep.id_peserta, ep.id_ujian, ep.id_siswa, ep.no_peserta, ep.no_kursi, ep.id_ruangan, "
                + "s.nis, s.nama_siswa FROM trx_peserta ep JOIN mst_siswa s ON ep.id_siswa = s.id_siswa "
                + "WHERE ep.id_ujian IN (" + placeholders(scheduleIds.size()) + ") ORDER BY ep.id_ujian, ep.no_peserta";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < scheduleIds.size(); i++) {
                ps.setInt(i + 1, scheduleIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SeatRow row = new SeatRow();
                    row.participantId = rs.getInt("id_peserta");
                    row.scheduleId = rs.getInt("id_ujian");
                    row.studentId = rs.getInt("id_siswa");
                    row.nis = rs.getString("nis");
                    row.studentName = rs.getString("nama_siswa");
                    row.participantNumber = rs.getString("no_peserta");
                    row.seatNumber = rs.getString("no_kursi");
                    int roomId = rs.getInt("id_ruangan");
                    row.roomId = rs.wasNull() ? null : roomId;
                    rows.add(row);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Gagal membaca data peserta: " + e.getMessage(), e);
        }
        return rows;
    }

    @Override
    public Map<Integer, Teacher> getSupervisors(LocalDate date, LocalTime start, LocalTime end) {
        Map<Integer, Teacher> result = new HashMap<>();
        String sql = "SELECT pr.id_ruangan, g.id_guru, g.nip, g.nama_guru FROM trx_pengawas_ruangan pr "
                + "JOIN mst_guru g ON pr.id_guru = g.id_guru "
                + "WHERE pr.tanggal = ? AND pr.jam_mulai = ? AND pr.jam_selesai = ?";

        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, date);
            ps.setObject(2, start);
            ps.setObject(3, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Teacher teacher = new Teacher();
                    teacher.setId(rs.getInt("id_guru"));
                    teacher.setNip(rs.getString("nip"));
                    teacher.setName(rs.getString("nama_guru"));
                    result.put(rs.getInt("id_ruangan"), teacher);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Gagal membaca data pengawas: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public Set<Integer> getBusyRoomIds(LocalDate date, LocalTime start, LocalTime end) {
        String overlap = "AND {t}.jam_mulai < ? AND {t}.jam_selesai > ? AND NOT ({t}.jam_mulai = ? AND {t}.jam_selesai = ?)";
        // Ruangan dipakai = ada kursi peserta di sesi lain yang waktunya bertumpuk, atau sudah ada pengawasnya
        String sql = "SELECT DISTINCT ep.id_ruangan AS id FROM trx_peserta ep JOIN trx_jadwal ej ON ep.id_ujian = ej.id_ujian "
                + "WHERE ep.id_ruangan IS NOT NULL AND ej.tanggal = ? " + overlap.replace("{t}", "ej")
                + " UNION SELECT DISTINCT pr.id_ruangan AS id FROM trx_pengawas_ruangan pr WHERE pr.tanggal = ? "
                + overlap.replace("{t}", "pr");
        return queryIds(sql, date, start, end, 2);
    }

    // Sama dengan getBusyRoomIds (kondisi persis sama), tetapi menyertakan kelas & jam pemakainya
    @Override
    public Map<Integer, String> getRoomUsages(LocalDate date, LocalTime start, LocalTime end) {
        String overlap = "AND {t}.jam_mulai < ? AND {t}.jam_selesai > ? AND NOT ({t}.jam_mulai = ? AND {t}.jam_selesai = ?)";
        String sql = "SELECT ep.id_ruangan AS id, COALESCE(k.nama_kelas, '-') AS kelas, ej.jam_mulai, ej.jam_selesai "
                + "FROM trx_peserta ep JOIN trx_jadwal ej ON ep.id_ujian = ej.id_ujian "
                + "LEFT JOIN mst_kelas k ON k.id_kelas = ej.id_kelas "
                + "WHERE ep.id_ruangan IS NOT NULL AND ej.tanggal = ? " + overlap.replace("{t}", "ej")
                + " UNION SELECT pr.id_ruangan AS id, 'pengawas sudah ditugaskan' AS kelas, pr.jam_mulai, pr.jam_selesai "
                + "FROM trx_pengawas_ruangan pr "
                + "WHERE pr.tanggal = ? " + overlap.replace("{t}", "pr")
                + " AND NOT EXISTS (SELECT 1 FROM trx_peserta ep2 JOIN trx_jadwal ej2 ON ep2.id_ujian = ej2.id_ujian "
                + "  WHERE ep2.id_ruangan = pr.id_ruangan AND ej2.tanggal = pr.tanggal "
                + "  AND ej2.jam_mulai = pr.jam_mulai AND ej2.jam_selesai = pr.jam_selesai)"
                + " ORDER BY id, jam_mulai, jam_selesai, kelas";

        // roomId -> (rentang jam -> daftar kelas), lalu dirangkai jadi teks
        Map<Integer, Map<String, java.util.Set<String>>> grouped = new java.util.TreeMap<>();
        java.time.format.DateTimeFormatter hhmm = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            for (int p = 0; p < 2; p++) {
                ps.setObject(index++, date);
                ps.setObject(index++, end);
                ps.setObject(index++, start);
                ps.setObject(index++, start);
                ps.setObject(index++, end);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String slot = rs.getTime("jam_mulai").toLocalTime().format(hhmm) + "-"
                            + rs.getTime("jam_selesai").toLocalTime().format(hhmm);
                    grouped.computeIfAbsent(rs.getInt("id"), k -> new java.util.LinkedHashMap<>())
                            .computeIfAbsent(slot, k -> new java.util.LinkedHashSet<>())
                            .add(rs.getString("kelas"));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Gagal membaca pemakaian ruangan: " + e.getMessage(), e);
        }

        Map<Integer, String> usages = new HashMap<>();
        grouped.forEach((roomId, slots) -> usages.put(roomId, slots.entrySet().stream()
                .map(e -> String.join(", ", e.getValue()) + " (" + e.getKey() + ")")
                .collect(java.util.stream.Collectors.joining("; "))));
        return usages;
    }

    @Override
    public Set<Integer> getBusyTeacherIds(LocalDate date, LocalTime start, LocalTime end) {
        String overlap = "AND {t}.jam_mulai < ? AND {t}.jam_selesai > ? AND NOT ({t}.jam_mulai = ? AND {t}.jam_selesai = ?)";
        String sql = "SELECT DISTINCT pr.id_guru AS id FROM trx_pengawas_ruangan pr WHERE pr.tanggal = ? "
                + overlap.replace("{t}", "pr");
        return queryIds(sql, date, start, end, 1);
    }

    @Override
    public int saveSupervisors(List<co.id.model.SupervisorAssignment> assignments, String updatedBy) {
        String delete = "DELETE FROM trx_pengawas_ruangan WHERE tanggal = ? AND jam_mulai = ? AND jam_selesai = ? AND id_ruangan = ?";
        String insert = "INSERT INTO trx_pengawas_ruangan(tanggal, jam_mulai, jam_selesai, id_ruangan, id_guru, diubah_oleh) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement del = connection.prepareStatement(delete);
                 PreparedStatement ins = connection.prepareStatement(insert)) {
                for (co.id.model.SupervisorAssignment a : assignments) {
                    del.setObject(1, a.getDate());
                    del.setObject(2, a.getStartTime());
                    del.setObject(3, a.getEndTime());
                    del.setInt(4, a.getRoom().getId());
                    del.executeUpdate();

                    ins.setObject(1, a.getDate());
                    ins.setObject(2, a.getStartTime());
                    ins.setObject(3, a.getEndTime());
                    ins.setInt(4, a.getRoom().getId());
                    ins.setInt(5, a.getTeacher().getId());
                    ins.setString(6, updatedBy);
                    ins.executeUpdate();
                }
                connection.commit();
                return assignments.size();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Gagal menyimpan pengawas, tidak ada data yang berubah: " + e.getMessage(), e);
        }
    }

    // Menjalankan query yang terdiri dari `parts` blok dengan parameter yang sama: tanggal, selesai, mulai, mulai, selesai
    private Set<Integer> queryIds(String sql, LocalDate date, LocalTime start, LocalTime end, int parts) {
        Set<Integer> ids = new HashSet<>();
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            int index = 1;
            for (int p = 0; p < parts; p++) {
                ps.setObject(index++, date);
                ps.setObject(index++, end);
                ps.setObject(index++, start);
                ps.setObject(index++, start);
                ps.setObject(index++, end);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Gagal membaca pemakaian ruangan/pengawas: " + e.getMessage(), e);
        }
        return ids;
    }

    @Override
    public Map<Integer, Integer> getSupervisionCounts(LocalDate date) {
        Map<Integer, Integer> counts = new HashMap<>();
        String sql = "SELECT id_guru, COUNT(*) AS total FROM trx_pengawas_ruangan WHERE tanggal = ? GROUP BY id_guru";
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    counts.put(rs.getInt("id_guru"), rs.getInt("total"));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Gagal membaca beban pengawas: " + e.getMessage(), e);
        }
        return counts;
    }

    @Override
    public void saveAllocation(LocalDate date, LocalTime start, LocalTime end, List<Integer> scheduleIds,
            List<SeatWrite> seats, Map<Integer, Integer> supervisorByRoom, String updatedBy) {

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                String ids = placeholders(scheduleIds.size());

                // 1. kosongkan kursi lama (NULL tidak bentrok dengan kunci unik) sebelum kursi baru dipasang
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE trx_peserta SET id_ruangan = NULL, no_kursi = NULL, diubah_oleh = ? WHERE id_ujian IN (" + ids + ")")) {
                    ps.setString(1, updatedBy);
                    for (int i = 0; i < scheduleIds.size(); i++) {
                        ps.setInt(i + 2, scheduleIds.get(i));
                    }
                    ps.executeUpdate();
                }

                // 2. kursi baru: peserta yang sudah ada di-update, siswa yang belum terdaftar didaftarkan
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE trx_peserta SET id_ruangan = ?, no_kursi = ?, diubah_oleh = ? WHERE id_peserta = ?");
                     PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO trx_peserta(id_ujian, id_siswa, no_peserta, id_ruangan, no_kursi, diubah_oleh) VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                     PreparedStatement setCode = connection.prepareStatement(
                        "UPDATE trx_peserta SET kode_peserta = ? WHERE id_peserta = ?")) {

                    for (SeatWrite seat : seats) {
                        if (seat.participantId != null) {
                            update.setInt(1, seat.roomId);
                            update.setString(2, seat.seatNumber);
                            update.setString(3, updatedBy);
                            update.setInt(4, seat.participantId);
                            update.executeUpdate();
                        } else {
                            insert.setInt(1, seat.scheduleId);
                            insert.setInt(2, seat.studentId);
                            insert.setString(3, seat.participantNumber);
                            insert.setInt(4, seat.roomId);
                            insert.setString(5, seat.seatNumber);
                            insert.setString(6, updatedBy);
                            insert.executeUpdate();
                            try (ResultSet keys = insert.getGeneratedKeys()) {
                                if (keys.next()) {
                                    int newId = keys.getInt(1);
                                    setCode.setString(1, "PST" + String.format("%04d", newId));
                                    setCode.setInt(2, newId);
                                    setCode.executeUpdate();
                                }
                            }
                        }
                    }
                }

                // 3. pengawas per ruangan untuk sesi ini
                try (PreparedStatement delete = connection.prepareStatement(
                        "DELETE FROM trx_pengawas_ruangan WHERE tanggal = ? AND jam_mulai = ? AND jam_selesai = ?")) {
                    delete.setObject(1, date);
                    delete.setObject(2, start);
                    delete.setObject(3, end);
                    delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO trx_pengawas_ruangan(tanggal, jam_mulai, jam_selesai, id_ruangan, id_guru, diubah_oleh) "
                        + "VALUES (?, ?, ?, ?, ?, ?)")) {
                    for (Map.Entry<Integer, Integer> entry : supervisorByRoom.entrySet()) {
                        if (entry.getValue() == null) {
                            continue;
                        }
                        insert.setObject(1, date);
                        insert.setObject(2, start);
                        insert.setObject(3, end);
                        insert.setInt(4, entry.getKey());
                        insert.setInt(5, entry.getValue());
                        insert.setString(6, updatedBy);
                        insert.addBatch();
                    }
                    insert.executeBatch();
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Gagal menyimpan penempatan ruangan: " + e.getMessage(), e);
        }
    }

    private static String placeholders(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(i == 0 ? "?" : ", ?");
        }
        return sb.toString();
    }
}