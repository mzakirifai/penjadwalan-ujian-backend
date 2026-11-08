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
            exception.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM mst_siswa WHERE id_siswa = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            exception.printStackTrace();
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