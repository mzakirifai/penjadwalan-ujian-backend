package co.id.dao.impl;

import co.id.config.AppLog;
import co.id.config.DatabaseConfiguration;
import co.id.dao.ExamScheduleDAO;
import co.id.model.Classroom;
import co.id.model.ExamSchedule;
import co.id.model.Room;
import co.id.model.Subject;
import co.id.model.Teacher;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ExamScheduleDAOImpl extends DatabaseConfiguration implements ExamScheduleDAO{

    private static final String SELECT_JOIN =
            "SELECT ej.*, "
            + "s.id_mapel, s.kode_mapel, s.nama_mapel, s.tingkat AS mapel_tingkat, s.jenis AS mapel_jenis, s.kkm, "
            + "k.id_kelas, k.kode_kelas, k.nama_kelas, k.tingkat AS kelas_tingkat "
            + "FROM trx_jadwal ej "
            + "LEFT JOIN mst_mapel s ON ej.id_mapel = s.id_mapel "
            + "LEFT JOIN mst_kelas k ON ej.id_kelas = k.id_kelas ";
    
    @Override
    public List<ExamSchedule> getAllExamSchedules() {
        List<ExamSchedule> schedules = new ArrayList<>();
        String sql = "SELECT * FROM trx_jadwal";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                schedules.add(mapBasicResultSetToExamSchedule(resultSet));
            }
        } catch (Exception exception) {
            AppLog.error("ExamScheduleDAOImpl.getAllExamSchedules", exception);
        }

        return schedules;
    }
    
    @Override
    public List<ExamSchedule> getAllExamSchedulesDetailed() {
        List<ExamSchedule> schedules = new ArrayList<>();
        String sql = SELECT_JOIN + "ORDER BY ej.id_ujian";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                schedules.add(mapResultSetToExamSchedule(resultSet));
            }
        } catch (Exception exception) {
            AppLog.error("ExamScheduleDAOImpl.getAllExamSchedulesDetailed", exception);
        }

        return schedules;
    }

    @Override
    public List<ExamSchedule> getExamScheduleBy(String keyword) {
        List<ExamSchedule> schedules = new ArrayList<>();
        String sql = SELECT_JOIN + "WHERE ej.kode_ujian LIKE ? OR s.nama_mapel LIKE ? OR k.nama_kelas LIKE ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            String param = "%" + keyword + "%";
            preparedStatement.setString(1, param);
            preparedStatement.setString(2, param);
            preparedStatement.setString(3, param);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    schedules.add(mapResultSetToExamSchedule(resultSet));
                }
            }
        } catch (Exception exception) {
            AppLog.error("ExamScheduleDAOImpl.getExamScheduleBy", exception);
        }

        return schedules;
    }

    @Override
    public List<ExamSchedule> getExamSchedules(int page, int size) {
        List<ExamSchedule> schedules = new ArrayList<>();
        String sql = SELECT_JOIN + "ORDER BY ej.id_ujian LIMIT ? OFFSET ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, size);
            preparedStatement.setInt(2, (page - 1) * size);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    schedules.add(mapResultSetToExamSchedule(resultSet));
                }
            }
        } catch (Exception exception) {
            AppLog.error("ExamScheduleDAOImpl.getExamSchedules", exception);
        }

        return schedules;
    }
    
    @Override
    public List<ExamSchedule> getTodayExamSchedules() {
        List<ExamSchedule> schedules = new ArrayList<>();
        String sql = SELECT_JOIN + "WHERE ej.tanggal = ? ORDER BY ej.jam_mulai";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, java.time.LocalDate.now());

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    schedules.add(mapResultSetToExamSchedule(resultSet));
                }
            }
        } catch (Exception exception) {
            AppLog.error("ExamScheduleDAOImpl.getTodayExamSchedules", exception);
        }

        return schedules;
    }

    @Override
    public int countExamSchedules() {
        String sql = "SELECT COUNT(*) AS total FROM trx_jadwal";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (resultSet.next()) {
                return resultSet.getInt("total");
            }
        } catch (Exception exception) {
            AppLog.error("ExamScheduleDAOImpl.countExamSchedules", exception);
        }

        return 0;
    }

    @Override
    public ExamSchedule getById(int id) {
        String sql = SELECT_JOIN + "WHERE ej.id_ujian = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToExamSchedule(resultSet);
                }
            }
        } catch (Exception exception) {
            AppLog.error("ExamScheduleDAOImpl.getById", exception);
        }

        return null;
    }
    
    @Override
    public List<ExamSchedule> getByPeriod(String examType, String semester, String academicYear) {
        List<ExamSchedule> schedules = new ArrayList<>();
        String sql = SELECT_JOIN
                + "WHERE ej.jenis_ujian = ? AND ej.semester = ? AND ej.tahun_akademik = ? "
                + "ORDER BY ej.tanggal, ej.jam_mulai";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, examType);
            preparedStatement.setString(2, semester);
            preparedStatement.setString(3, academicYear);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    schedules.add(mapResultSetToExamSchedule(resultSet));
                }
            }
        } catch (Exception exception) {
            AppLog.error("ExamScheduleDAOImpl.getByPeriod", exception);
        }

        return schedules;
    }
    
    @Override
    public List<ExamSchedule> getByTeacherAndPeriod(int teacherId, String examType, String semester, String academicYear) {
        // Satu baris = satu jadwal (kelas + mapel) yang punya peserta di ruangan yang diawasi guru ini
        // pada sesinya. Ruangan campur menghasilkan beberapa baris (satu per kelas) dengan ruangan yang sama.
        // Ruangan & pengawas hasil penempatan dibawa lewat examSchedule.setRoom/setTeacher.
        List<ExamSchedule> schedules = new ArrayList<>();
        String sql = "SELECT ej.*, "
                + "s.id_mapel, s.kode_mapel, s.nama_mapel, s.tingkat AS mapel_tingkat, s.jenis AS mapel_jenis, s.kkm, "
                + "k.id_kelas, k.kode_kelas, k.nama_kelas, k.tingkat AS kelas_tingkat, "
                + "r.id_ruangan, r.kode_ruangan, r.nama_ruangan, r.lantai, r.kapasitas, "
                + "g.id_guru, g.nip, g.nama_guru "
                + "FROM trx_pengawas_ruangan pr "
                + "JOIN trx_jadwal ej ON ej.tanggal = pr.tanggal AND ej.jam_mulai = pr.jam_mulai AND ej.jam_selesai = pr.jam_selesai "
                + "JOIN mst_ruangan r ON r.id_ruangan = pr.id_ruangan "
                + "JOIN mst_guru g ON g.id_guru = pr.id_guru "
                + "LEFT JOIN mst_mapel s ON ej.id_mapel = s.id_mapel "
                + "LEFT JOIN mst_kelas k ON ej.id_kelas = k.id_kelas "
                + "WHERE pr.id_guru = ? AND ej.jenis_ujian = ? AND ej.semester = ? AND ej.tahun_akademik = ? "
                + "AND EXISTS (SELECT 1 FROM trx_peserta ep WHERE ep.id_ujian = ej.id_ujian AND ep.id_ruangan = pr.id_ruangan) "
                + "ORDER BY ej.tanggal, ej.jam_mulai, r.nama_ruangan, k.nama_kelas";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, teacherId);
            preparedStatement.setString(2, examType);
            preparedStatement.setString(3, semester);
            preparedStatement.setString(4, academicYear);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    ExamSchedule schedule = mapResultSetToExamSchedule(resultSet);
                    Room room = new Room();
                    room.setId(resultSet.getInt("id_ruangan"));
                    room.setCode(resultSet.getString("kode_ruangan"));
                    room.setName(resultSet.getString("nama_ruangan"));
                    room.setFloor(resultSet.getInt("lantai"));
                    room.setCapacity(resultSet.getInt("kapasitas"));
                    schedule.setRoom(room);
                    Teacher teacher = new Teacher();
                    teacher.setId(resultSet.getInt("id_guru"));
                    teacher.setNip(resultSet.getString("nip"));
                    teacher.setName(resultSet.getString("nama_guru"));
                    schedule.setTeacher(teacher);
                    schedules.add(schedule);
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal membaca jadwal mengawas guru: " + exception.getMessage(), exception);
        }

        return schedules;
    }
    
    @Override
    public java.util.Map<java.time.LocalDate, Integer> getExamCountPerDay(java.time.LocalDate start, java.time.LocalDate end) {
        java.util.Map<java.time.LocalDate, Integer> counts = new java.util.LinkedHashMap<>();

        String sql = "SELECT tanggal, COUNT(*) AS jumlah FROM trx_jadwal "
                + "WHERE tanggal BETWEEN ? AND ? GROUP BY tanggal ORDER BY tanggal";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setDate(1, java.sql.Date.valueOf(start));
            preparedStatement.setDate(2, java.sql.Date.valueOf(end));

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    java.sql.Date date = resultSet.getDate("tanggal");
                    counts.put(date.toLocalDate(), resultSet.getInt("jumlah"));
                }
            }
        } catch (Exception exception) {
            AppLog.error("ExamScheduleDAOImpl.getExamCountPerDay", exception);
        }

        return counts;
    }

    @Override
    public void saveOrUpdate(ExamSchedule examSchedule) {
        saveOrUpdate(examSchedule, false);
    }

    // Jadwal dianggap sudah ditempatkan bila ada pesertanya yang sudah mendapat ruangan
    @Override
    public boolean hasPlacement(int scheduleId) {
        String sql = "SELECT EXISTS (SELECT 1 FROM trx_peserta WHERE id_ujian = ? AND id_ruangan IS NOT NULL) AS placed";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, scheduleId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() && resultSet.getBoolean("placed");
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal memeriksa penempatan ruangan: " + exception.getMessage(), exception);
        }
    }

    // Hapus pengawas pada sesi (tanggal + jam) yang ruangannya sudah tidak berisi peserta sama sekali
    private void removeIdleSupervisors(Connection connection, Object date, Object startTime, Object endTime) throws Exception {
        String sql = "DELETE FROM trx_pengawas_ruangan WHERE tanggal = ? AND jam_mulai = ? AND jam_selesai = ? "
                + "AND NOT EXISTS (SELECT 1 FROM trx_peserta ep JOIN trx_jadwal ej ON ep.id_ujian = ej.id_ujian "
                + "WHERE ep.id_ruangan = trx_pengawas_ruangan.id_ruangan AND ej.tanggal = trx_pengawas_ruangan.tanggal "
                + "AND ej.jam_mulai = trx_pengawas_ruangan.jam_mulai AND ej.jam_selesai = trx_pengawas_ruangan.jam_selesai)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setObject(1, date);
            preparedStatement.setObject(2, startTime);
            preparedStatement.setObject(3, endTime);
            preparedStatement.executeUpdate();
        }
    }

    // Tanggal + jam lama sebuah jadwal (dibaca sebelum diubah/dihapus); null jika tidak ada
    private Object[] readSlot(Connection connection, int scheduleId) throws Exception {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT tanggal, jam_mulai, jam_selesai FROM trx_jadwal WHERE id_ujian = ?")) {
            preparedStatement.setInt(1, scheduleId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new Object[]{resultSet.getDate("tanggal"), resultSet.getTime("jam_mulai"), resultSet.getTime("jam_selesai")};
                }
            }
        }
        return null;
    }

    @Override
    public void saveOrUpdate(ExamSchedule examSchedule, boolean resetPlacement) {
        String sqlInsert = "INSERT INTO trx_jadwal(jenis_ujian, semester, tahun_akademik, tanggal, jam_mulai, jam_selesai, id_mapel, id_kelas, keterangan, diubah_oleh, kode_guru, mode_ujian) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlUpdate = "UPDATE trx_jadwal SET jenis_ujian = ?, semester = ?, tahun_akademik = ?, tanggal = ?, jam_mulai = ?, jam_selesai = ?, "
                + "id_mapel = ?, id_kelas = ?, keterangan = ?, diubah_oleh = ?, kode_guru = ?, mode_ujian = ? WHERE id_ujian = ?";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try {
                if (examSchedule.getId() == 0) {
                    try (PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                        bindCommonFields(preparedStatement, examSchedule);
                        preparedStatement.executeUpdate();

                        try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                int newId = generatedKeys.getInt(1);
                                String generatedCode = "UJ" + String.format("%03d", newId);

                                try (PreparedStatement updateCode = connection.prepareStatement(
                                        "UPDATE trx_jadwal SET kode_ujian = ? WHERE id_ujian = ?")) {
                                    updateCode.setString(1, generatedCode);
                                    updateCode.setInt(2, newId);
                                    updateCode.executeUpdate();
                                }

                                examSchedule.setId(newId);
                                examSchedule.setCode(generatedCode);
                            }
                        }
                    }
                } else {
                    Object[] oldSlot = resetPlacement ? readSlot(connection, examSchedule.getId()) : null;
                    if (resetPlacement) {
                        // tanggal/jam/kelas berubah atau jadi online: kursi & ruangan lama tidak berlaku lagi
                        try (PreparedStatement clearSeats = connection.prepareStatement(
                                "UPDATE trx_peserta SET id_ruangan = NULL, no_kursi = NULL, diubah_oleh = ? WHERE id_ujian = ?")) {
                            clearSeats.setString(1, examSchedule.getUpdatedBy());
                            clearSeats.setInt(2, examSchedule.getId());
                            clearSeats.executeUpdate();
                        }
                    }

                    try (PreparedStatement preparedStatement = connection.prepareStatement(sqlUpdate)) {
                        bindCommonFields(preparedStatement, examSchedule);
                        preparedStatement.setInt(13, examSchedule.getId());
                        preparedStatement.executeUpdate();
                    }

                    if (oldSlot != null) {
                        removeIdleSupervisors(connection, oldSlot[0], oldSlot[1], oldSlot[2]);
                    }
                }

                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal menyimpan data ke database: " + exception.getMessage(), exception);
        }
    }
    
    // Bind field yang sama untuk INSERT & UPDATE (urutan parameter 1-13 identik di kedua query)
    private void bindCommonFields(PreparedStatement preparedStatement, ExamSchedule examSchedule) throws Exception {
        preparedStatement.setString(1, examSchedule.getExamType());
        preparedStatement.setString(2, examSchedule.getSemester());
        preparedStatement.setString(3, examSchedule.getAcademicYear());
        preparedStatement.setObject(4, examSchedule.getDate());
        preparedStatement.setObject(5, examSchedule.getStartTime());
        preparedStatement.setObject(6, examSchedule.getEndTime());

        if (examSchedule.getSubject() != null) {
            preparedStatement.setInt(7, examSchedule.getSubject().getId());
        } else {
            preparedStatement.setNull(7, Types.INTEGER);
        }

        if (examSchedule.getClassroom() != null) {
            preparedStatement.setInt(8, examSchedule.getClassroom().getId());
        } else {
            preparedStatement.setNull(8, Types.INTEGER);
        }

        preparedStatement.setString(9, examSchedule.getNotes());
        preparedStatement.setString(10, examSchedule.getUpdatedBy());

        if (examSchedule.getTeacherCode() != null) {
            preparedStatement.setInt(11, examSchedule.getTeacherCode());
        } else {
            preparedStatement.setNull(11, Types.INTEGER);
        }
        preparedStatement.setString(12, examSchedule.isOnline() ? ExamSchedule.MODE_ONLINE : ExamSchedule.MODE_OFFLINE);
    }

    // Ubah mode banyak jadwal sekaligus. Jadwal yang dijadikan Online dikosongkan ruangan & kursinya,
    // lalu pengawas di ruangan yang jadi kosong ikut dibersihkan. Semua atau tidak sama sekali.
    @Override
    public int updateMode(List<Integer> scheduleIds, String mode, String updatedBy) {
        boolean online = ExamSchedule.MODE_ONLINE.equals(mode);
        int changed = 0;
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                for (Integer id : scheduleIds) {
                    Object[] slot = readSlot(connection, id);
                    if (slot == null) {
                        continue;
                    }
                    if (online) {
                        try (PreparedStatement clearSeats = connection.prepareStatement(
                                "UPDATE trx_peserta SET id_ruangan = NULL, no_kursi = NULL, diubah_oleh = ? "
                                + "WHERE id_ujian = ? AND (id_ruangan IS NOT NULL OR no_kursi IS NOT NULL)")) {
                            clearSeats.setString(1, updatedBy);
                            clearSeats.setInt(2, id);
                            clearSeats.executeUpdate();
                        }
                    }
                    try (PreparedStatement ps = connection.prepareStatement(
                            "UPDATE trx_jadwal SET mode_ujian = ?, diubah_oleh = ? WHERE id_ujian = ? AND mode_ujian <> ?")) {
                        ps.setString(1, mode);
                        ps.setString(2, updatedBy);
                        ps.setInt(3, id);
                        ps.setString(4, mode);
                        changed += ps.executeUpdate();
                    }
                    if (online) {
                        removeIdleSupervisors(connection, slot[0], slot[1], slot[2]);
                    }
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal mengubah mode ujian: " + exception.getMessage(), exception);
        }
        return changed;
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM trx_jadwal WHERE id_ujian = ?";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                Object[] oldSlot = readSlot(connection, id);
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setInt(1, id);
                    preparedStatement.executeUpdate();
                }
                // peserta ikut terhapus (cascade); pengawas di ruangan yang jadi kosong ikut dibersihkan
                if (oldSlot != null) {
                    removeIdleSupervisors(connection, oldSlot[0], oldSlot[1], oldSlot[2]);
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal menghapus data: " + exception.getMessage(), exception);
        }
    }

    @Override
    public List<ExamSchedule> findClassOverlaps(LocalDate date, LocalTime startTime, LocalTime endTime,
                                                int classroomId, int excludeId) {
        // Bentrok kelas: tanggal sama, kelas sama, dan rentang waktu saling overlap
        // (mulai_baru < selesai_lama DAN selesai_baru > mulai_lama). Ruangan & pengawas dicek di Penempatan Ruangan.
        // Jika query gagal, exception dilempar (tidak dianggap "tidak bentrok").
        String sql = SELECT_JOIN
                + "WHERE ej.tanggal = ? AND ej.id_kelas = ? "
                + "AND ej.jam_mulai < ? AND ej.jam_selesai > ? "
                + "AND ej.id_ujian <> ? ORDER BY ej.jam_mulai";

        List<ExamSchedule> result = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setObject(1, date);
            preparedStatement.setInt(2, classroomId);
            preparedStatement.setObject(3, endTime);
            preparedStatement.setObject(4, startTime);
            preparedStatement.setInt(5, excludeId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(mapResultSetToExamSchedule(resultSet));
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal memeriksa bentrok jadwal: " + exception.getMessage(), exception);
        }
        return result;
    }

    // Ringkasan penempatan per jadwal (untuk daftar jadwal). Satu kali panggil untuk satu halaman data.
    @Override
    public java.util.Map<Integer, co.id.model.PlacementSummary> getPlacementSummaries(List<Integer> scheduleIds) {
        java.util.Map<Integer, co.id.model.PlacementSummary> result = new java.util.LinkedHashMap<>();
        if (scheduleIds == null || scheduleIds.isEmpty()) {
            return result;
        }
        for (Integer id : scheduleIds) {
            result.put(id, new co.id.model.PlacementSummary(id));
        }
        String in = String.join(",", java.util.Collections.nCopies(scheduleIds.size(), "?"));

        // 1. Ruangan hasil Penempatan Ruangan: jumlah siswa per ruangan + pengawas ruangan itu pada sesinya
        String sqlRooms = "SELECT ep.id_ujian, r.kode_ruangan, r.nama_ruangan, COUNT(*) AS jumlah, "
                + "(SELECT g.nama_guru FROM trx_pengawas_ruangan pr JOIN mst_guru g ON g.id_guru = pr.id_guru "
                + " WHERE pr.id_ruangan = r.id_ruangan AND pr.tanggal = ej.tanggal "
                + " AND pr.jam_mulai = ej.jam_mulai AND pr.jam_selesai = ej.jam_selesai LIMIT 1) AS pengawas "
                + "FROM trx_peserta ep "
                + "JOIN trx_jadwal ej ON ej.id_ujian = ep.id_ujian "
                + "JOIN mst_ruangan r ON r.id_ruangan = ep.id_ruangan "
                + "WHERE ep.id_ujian IN (" + in + ") "
                + "GROUP BY ep.id_ujian, r.id_ruangan, r.kode_ruangan, r.nama_ruangan, ej.tanggal, ej.jam_mulai, ej.jam_selesai "
                + "ORDER BY ep.id_ujian, r.kode_ruangan, r.nama_ruangan";

        // 2. Jumlah siswa kelas
        String sqlSchedule = "SELECT ej.id_ujian, ej.mode_ujian, "
                + "(SELECT COUNT(*) FROM mst_siswa s WHERE s.id_kelas = ej.id_kelas) AS jumlah_siswa "
                + "FROM trx_jadwal ej "
                + "WHERE ej.id_ujian IN (" + in + ")";

        // 3. Siswa kelas yang belum mendapat kursi pada jadwal tersebut
        String sqlUnplaced = "SELECT ej.id_ujian, COUNT(*) AS belum "
                + "FROM trx_jadwal ej JOIN mst_siswa s ON s.id_kelas = ej.id_kelas "
                + "WHERE ej.id_ujian IN (" + in + ") AND NOT EXISTS ("
                + " SELECT 1 FROM trx_peserta ep WHERE ep.id_ujian = ej.id_ujian "
                + " AND ep.id_siswa = s.id_siswa AND ep.id_ruangan IS NOT NULL) "
                + "GROUP BY ej.id_ujian";

        try (Connection connection = getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(sqlRooms)) {
                bindIds(ps, scheduleIds);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.get(rs.getInt("id_ujian")).getRooms().add(new co.id.model.PlacementSummary.RoomUsage(
                                rs.getString("kode_ruangan"), rs.getString("nama_ruangan"),
                                rs.getInt("jumlah"), rs.getString("pengawas")));
                    }
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(sqlSchedule)) {
                bindIds(ps, scheduleIds);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.get(rs.getInt("id_ujian")).setClassStudentCount(rs.getInt("jumlah_siswa"));
                        result.get(rs.getInt("id_ujian")).setOnline(ExamSchedule.MODE_ONLINE.equals(rs.getString("mode_ujian")));
                    }
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(sqlUnplaced)) {
                bindIds(ps, scheduleIds);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        result.get(rs.getInt("id_ujian")).setUnplacedCount(rs.getInt("belum"));
                    }
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal membaca ringkasan penempatan ruangan: " + exception.getMessage(), exception);
        }
        return result;
    }

    private static void bindIds(PreparedStatement ps, List<Integer> ids) throws Exception {
        for (int i = 0; i < ids.size(); i++) {
            ps.setInt(i + 1, ids.get(i));
        }
    }

    // Sesi lain pada tanggal yang sama yang jamnya bertumpuk tetapi TIDAK sama persis
    // (mis. 07:30-09:00 vs 08:00-09:30). Jadwal seperti ini tidak bisa dicampur dalam satu sesi.
    @Override
    public List<co.id.model.SessionSlot> findMisalignedSessions(LocalDate date, LocalTime startTime, LocalTime endTime, int excludeId) {
        String sql = "SELECT jam_mulai, jam_selesai, COUNT(*) AS total FROM trx_jadwal "
                + "WHERE tanggal = ? AND jam_mulai < ? AND jam_selesai > ? "
                + "AND NOT (jam_mulai = ? AND jam_selesai = ?) AND id_ujian <> ? "
                + "AND mode_ujian = 'Offline' " // ujian online tidak ikut sesi ruangan
                + "GROUP BY jam_mulai, jam_selesai ORDER BY jam_mulai, jam_selesai";

        List<co.id.model.SessionSlot> slots = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setObject(1, date);
            preparedStatement.setObject(2, endTime);
            preparedStatement.setObject(3, startTime);
            preparedStatement.setObject(4, startTime);
            preparedStatement.setObject(5, endTime);
            preparedStatement.setInt(6, excludeId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    slots.add(new co.id.model.SessionSlot(
                            resultSet.getTime("jam_mulai").toLocalTime(),
                            resultSet.getTime("jam_selesai").toLocalTime(),
                            resultSet.getInt("total")));
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal memeriksa sesi ujian: " + exception.getMessage(), exception);
        }
        return slots;
    }

    @Override
    public boolean hasSameSubjectForClassroom(int classroomId, int subjectId, Integer teacherCode, String examType,
            String semester, String academicYear, int excludeId) {
        String sql = "SELECT COUNT(*) AS total FROM trx_jadwal "
                + "WHERE id_kelas = ? AND id_mapel = ? AND kode_guru <=> ? AND jenis_ujian = ? "
                + "AND semester = ? AND tahun_akademik = ? AND id_ujian <> ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, classroomId);
            preparedStatement.setInt(2, subjectId);
            if (teacherCode != null) {
                preparedStatement.setInt(3, teacherCode);
            } else {
                preparedStatement.setNull(3, Types.INTEGER);
            }
            preparedStatement.setString(4, examType);
            preparedStatement.setString(5, semester);
            preparedStatement.setString(6, academicYear);
            preparedStatement.setInt(7, excludeId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total") > 0;
                }
            }
        } catch (Exception exception) {
            AppLog.error("ExamScheduleDAOImpl.hasSameSubjectForClassroom", exception);
        }

        return false;
    }
    
    // Helper method untuk mapping ResultSet ke object ExamSchedule (tanpa data relasi)
    private ExamSchedule mapBasicResultSetToExamSchedule(ResultSet resultSet) throws Exception {
        ExamSchedule examSchedule = new ExamSchedule();
        examSchedule.setId(resultSet.getInt("id_ujian"));
        examSchedule.setCode(resultSet.getString("kode_ujian"));

        int teacherCode = resultSet.getInt("kode_guru");
        examSchedule.setTeacherCode(resultSet.wasNull() ? null : teacherCode);

        examSchedule.setExamType(resultSet.getString("jenis_ujian"));
        examSchedule.setMode(resultSet.getString("mode_ujian"));
        examSchedule.setSemester(resultSet.getString("semester"));
        examSchedule.setAcademicYear(resultSet.getString("tahun_akademik"));

        Date date = resultSet.getDate("tanggal");
        if (date != null) {
            examSchedule.setDate(date.toLocalDate());
        }

        Time startTime = resultSet.getTime("jam_mulai");
        if (startTime != null) {
            examSchedule.setStartTime(startTime.toLocalTime());
        }

        Time endTime = resultSet.getTime("jam_selesai");
        if (endTime != null) {
            examSchedule.setEndTime(endTime.toLocalTime());
        }

        examSchedule.setNotes(resultSet.getString("keterangan"));

        Timestamp createdAt = resultSet.getTimestamp("tanggal_buat");
        if (createdAt != null) {
            examSchedule.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("tanggal_update");
        if (updatedAt != null) {
            examSchedule.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        examSchedule.setUpdatedBy(resultSet.getString("diubah_oleh"));

        return examSchedule;
    }
    
    // Helper method untuk mapping ResultSet ke object ExamSchedule (lengkap dengan data relasi)
    private ExamSchedule mapResultSetToExamSchedule(ResultSet resultSet) throws Exception {
        ExamSchedule examSchedule = mapBasicResultSetToExamSchedule(resultSet);

        int subjectId = resultSet.getInt("id_mapel");
        if (!resultSet.wasNull()) {
            Subject subject = new Subject();
            subject.setId(subjectId);
            subject.setCode(resultSet.getString("kode_mapel"));
            subject.setName(resultSet.getString("nama_mapel"));
            subject.setGrade(resultSet.getString("mapel_tingkat"));
            subject.setType(resultSet.getString("mapel_jenis"));
            subject.setPassingGrade(resultSet.getInt("kkm"));
            examSchedule.setSubject(subject);
        }

        int classroomId = resultSet.getInt("id_kelas");
        if (!resultSet.wasNull()) {
            Classroom classroom = new Classroom();
            classroom.setId(classroomId);
            classroom.setCode(resultSet.getString("kode_kelas"));
            classroom.setName(resultSet.getString("nama_kelas"));
            classroom.setGrade(resultSet.getString("kelas_tingkat"));
            examSchedule.setClassroom(classroom);
        }

        return examSchedule;
    }
}