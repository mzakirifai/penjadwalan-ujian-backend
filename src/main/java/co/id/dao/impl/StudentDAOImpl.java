package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.StudentDAO;
import co.id.model.Classroom;
import co.id.model.Student;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class StudentDAOImpl extends DatabaseConfiguration implements StudentDAO{

    @Override
    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM mst_siswa";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                students.add(mapBasicResultSetToStudent(resultSet));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return students;
    }

    @Override
    public List<Student> getStudentBy(String keyword) {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT s.*, "
                + "k.id_kelas, "
                + "k.kode_kelas, "
                + "k.nama_kelas, "
                + "k.tingkat "
                + "FROM mst_siswa s "
                + "LEFT JOIN mst_kelas k ON s.id_kelas = k.id_kelas "
                + "WHERE s.nama_siswa LIKE ? OR s.nis LIKE ? OR s.nisn LIKE ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, "%" + keyword + "%");
            preparedStatement.setString(2, "%" + keyword + "%");
            preparedStatement.setString(3, "%" + keyword + "%");

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(mapResultSetToStudent(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return students;
    }

    @Override
    public List<Student> getStudents(int page, int size) {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT s.*, "
                + "k.id_kelas, "
                + "k.kode_kelas, "
                + "k.nama_kelas, "
                + "k.tingkat "
                + "FROM mst_siswa s "
                + "LEFT JOIN mst_kelas k ON s.id_kelas = k.id_kelas "
                + "ORDER BY s.id_siswa "
                + "LIMIT ? OFFSET ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, size);
            preparedStatement.setInt(2, (page - 1) * size);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(mapResultSetToStudent(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return students;
    }

    @Override
    public int countStudents() {
        String sql = "SELECT COUNT(*) AS total FROM mst_siswa";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (resultSet.next()) {
                return resultSet.getInt("total");
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return 0;
    }

    @Override
    public Student getById(int id) {
        String sql = "SELECT s.*, "
                + "k.id_kelas, "
                + "k.kode_kelas, "
                + "k.nama_kelas, "
                + "k.tingkat "
                + "FROM mst_siswa s "
                + "LEFT JOIN mst_kelas k ON s.id_kelas = k.id_kelas "
                + "WHERE s.id_siswa = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToStudent(resultSet);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }
    
    @Override
    public List<Student> getByClassroom(int classroomId) {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT s.*, "
                + "k.id_kelas, k.kode_kelas, k.nama_kelas, k.tingkat "
                + "FROM mst_siswa s "
                + "LEFT JOIN mst_kelas k ON s.id_kelas = k.id_kelas "
                + "WHERE s.id_kelas = ? "
                + "ORDER BY s.nama_siswa";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, classroomId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    students.add(mapResultSetToStudent(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return students;
    }

    @Override
    public boolean isNisTaken(String nis, int excludeId) {
        String sql = "SELECT COUNT(*) AS total FROM mst_siswa WHERE nis = ? AND id_siswa <> ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, nis);
            preparedStatement.setInt(2, excludeId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total") > 0;
                }
            }
        } catch (Exception exception) {
            // Kalau pengecekan gagal, hentikan penyimpanan supaya tidak ada NIS ganda yang lolos
            throw new IllegalStateException("Gagal memeriksa NIS: " + exception.getMessage(), exception);
        }

        return false;
    }

    @Override
    public boolean isNisnTaken(String nisn, int excludeId) {
        String sql = "SELECT COUNT(*) AS total FROM mst_siswa WHERE nisn = ? AND id_siswa <> ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, nisn);
            preparedStatement.setInt(2, excludeId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total") > 0;
                }
            }
        } catch (Exception exception) {
            // Kalau pengecekan gagal, hentikan penyimpanan supaya tidak ada NISN ganda yang lolos
            throw new IllegalStateException("Gagal memeriksa NISN: " + exception.getMessage(), exception);
        }

        return false;
    }
    
    @Override
    public Set<String> getAllNis() {
        return readAllValues("SELECT nis FROM mst_siswa", "nis");
    }

    @Override
    public Set<String> getAllNisn() {
        return readAllValues("SELECT nisn FROM mst_siswa WHERE nisn IS NOT NULL", "nisn");
    }

    private Set<String> readAllValues(String sql, String column) {
        Set<String> values = new HashSet<>();

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                values.add(resultSet.getString(column));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal membaca data siswa dari database: " + exception.getMessage(), exception);
        }

        return values;
    }

    // Simpan banyak siswa dalam satu transaksi: kalau satu gagal, semuanya dibatalkan.
    @Override
    public int saveBatch(List<Student> students) {
        String sql = "INSERT INTO mst_siswa(nis, nisn, nama_siswa, id_kelas, jenis_kelamin, diubah_oleh) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                for (Student student : students) {
                    preparedStatement.setString(1, student.getNis());
                    preparedStatement.setString(2, student.getNisn());
                    preparedStatement.setString(3, student.getName());
                    preparedStatement.setInt(4, student.getClassroom().getId());
                    preparedStatement.setString(5, student.getGender());
                    preparedStatement.setString(6, student.getUpdatedBy());
                    preparedStatement.addBatch();
                }

                preparedStatement.executeBatch();
                connection.commit();
                return students.size();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Import siswa gagal, tidak ada data yang disimpan: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void saveOrUpdate(Student student) {
        String sqlInsert = "INSERT INTO mst_siswa(nis, nisn, nama_siswa, id_kelas, jenis_kelamin, diubah_oleh) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlUpdate = "UPDATE mst_siswa SET nis = ?, nisn = ?, nama_siswa = ?, id_kelas = ?, jenis_kelamin = ?, diubah_oleh = ? WHERE id_siswa = ?";

        try (Connection connection = getConnection()) {
            if (student.getId() == 0) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert)) {
                    preparedStatement.setString(1, student.getNis());
                    preparedStatement.setString(2, student.getNisn());
                    preparedStatement.setString(3, student.getName());
                    if (student.getClassroom() != null) {
                        preparedStatement.setInt(4, student.getClassroom().getId());
                    } else {
                        preparedStatement.setNull(4, Types.INTEGER);
                    }
                    preparedStatement.setString(5, student.getGender());
                    preparedStatement.setString(6, student.getUpdatedBy());
                    preparedStatement.executeUpdate();
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sqlUpdate)) {
                    preparedStatement.setString(1, student.getNis());
                    preparedStatement.setString(2, student.getNisn());
                    preparedStatement.setString(3, student.getName());
                    if (student.getClassroom() != null) {
                        preparedStatement.setInt(4, student.getClassroom().getId());
                    } else {
                        preparedStatement.setNull(4, Types.INTEGER);
                    }
                    preparedStatement.setString(5, student.getGender());
                    preparedStatement.setString(6, student.getUpdatedBy());
                    preparedStatement.setInt(7, student.getId());
                    preparedStatement.executeUpdate();
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal menyimpan data ke database: " + exception.getMessage(), exception);
        }
    }

    // Pengawas di ruangan yang sudah tidak berisi kursi peserta pada sesinya (mis. setelah siswanya dihapus)
    private static final String DELETE_IDLE_SUPERVISORS = "DELETE FROM trx_pengawas_ruangan WHERE NOT EXISTS ("
            + "SELECT 1 FROM trx_peserta ep JOIN trx_jadwal ej ON ej.id_ujian = ep.id_ujian "
            + "WHERE ep.id_ruangan = trx_pengawas_ruangan.id_ruangan AND ej.tanggal = trx_pengawas_ruangan.tanggal "
            + "AND ej.jam_mulai = trx_pengawas_ruangan.jam_mulai AND ej.jam_selesai = trx_pengawas_ruangan.jam_selesai)";

    // Hapus semua siswa dalam satu kelas sekaligus, dalam satu transaksi (berhasil semua atau tidak sama sekali).
    // Data peserta ujian dan nilai milik siswa tersebut ikut terhapus karena foreign key ON DELETE CASCADE;
    // pengawas di ruangan yang jadi kosong (tidak ada kursi lagi pada sesinya) ikut dibersihkan.
    @Override
    public int deleteByClassroom(int classroomId) {
        String sql = "DELETE FROM mst_siswa WHERE id_kelas = ?";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                int deleted;
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setInt(1, classroomId);
                    deleted = preparedStatement.executeUpdate();
                }
                try (PreparedStatement preparedStatement = connection.prepareStatement(DELETE_IDLE_SUPERVISORS)) {
                    preparedStatement.executeUpdate();
                }
                connection.commit();
                return deleted;
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception exception) {
            throw new RuntimeException("Hapus siswa per kelas gagal, tidak ada data yang dihapus: " + exception.getMessage(), exception);
        }
    }

    // Hitung nilai & kursi peserta ujian milik siswa satu kelas (yang akan ikut terhapus)
    @Override
    public int[] countExamDataByClassroom(int classroomId) {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM trx_nilai n JOIN mst_siswa s ON s.id_siswa = n.id_siswa WHERE s.id_kelas = ?) AS nilai, "
                + "(SELECT COUNT(*) FROM trx_peserta p JOIN mst_siswa s ON s.id_siswa = p.id_siswa WHERE s.id_kelas = ?) AS peserta";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, classroomId);
            preparedStatement.setInt(2, classroomId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                resultSet.next();
                return new int[]{resultSet.getInt("nilai"), resultSet.getInt("peserta")};
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Gagal menghitung data ujian siswa: " + exception.getMessage(), exception);
        }
    }

    @Override
    // Hapus satu siswa. Sama seperti hapus kelompok: kursi & nilainya ikut terhapus (ON DELETE CASCADE),
    // dan pengawas di ruangan yang jadi kosong ikut dibersihkan, dalam satu transaksi.
    public void delete(int id) {
        String sql = "DELETE FROM mst_siswa WHERE id_siswa = ?";

        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                    preparedStatement.setInt(1, id);
                    preparedStatement.executeUpdate();
                }
                try (PreparedStatement preparedStatement = connection.prepareStatement(DELETE_IDLE_SUPERVISORS)) {
                    preparedStatement.executeUpdate();
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
    
    // Helper method untuk mapping ResultSet ke object Student (tanpa data Classroom)
    private Student mapBasicResultSetToStudent(ResultSet resultSet) throws Exception {
        Student student = new Student();
        student.setId(resultSet.getInt("id_siswa"));
        student.setNis(resultSet.getString("nis"));
        student.setNisn(resultSet.getString("nisn"));
        student.setName(resultSet.getString("nama_siswa"));
        student.setGender(resultSet.getString("jenis_kelamin"));

        Timestamp createdAt = resultSet.getTimestamp("tanggal_buat");
        if (createdAt != null) {
            student.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = resultSet.getTimestamp("tanggal_update");
        if (updatedAt != null) {
            student.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        student.setUpdatedBy(resultSet.getString("diubah_oleh"));

        return student;
    }

    // Helper method untuk mapping ResultSet ke object Student (lengkap dengan data Classroom)
    private Student mapResultSetToStudent(ResultSet resultSet) throws Exception {
        Student student = mapBasicResultSetToStudent(resultSet);

        int classroomId = resultSet.getInt("id_kelas");
        if (!resultSet.wasNull()) {
            Classroom classroom = new Classroom();
            classroom.setId(classroomId);
            classroom.setCode(resultSet.getString("kode_kelas"));
            classroom.setName(resultSet.getString("nama_kelas"));
            classroom.setGrade(resultSet.getString("tingkat"));
            student.setClassroom(classroom);
        }

        return student;
    }
}