package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.ReportDAO;
import co.id.dao.RoomDAO;
import co.id.dao.impl.RoomDAOImpl;
import co.id.model.Major;
import co.id.model.Room;
import co.id.model.Subject;
import co.id.model.report.ClassroomReportItem;
import co.id.model.report.MajorReportItem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ReportDAOImpl extends DatabaseConfiguration implements ReportDAO {
    private final RoomDAO roomDAO;

    public ReportDAOImpl() {
        roomDAO = new RoomDAOImpl();
    }

    @Override
    public List<MajorReportItem> getMajorReport() {
        List<MajorReportItem> items = new ArrayList<>();

        String sql = "SELECT j.kode_jurusan, j.nama_jurusan, j.singkatan, "
                + "COUNT(DISTINCT k.id_kelas) AS jumlah_kelas, "
                + "COUNT(s.id_siswa) AS jumlah_siswa "
                + "FROM mst_jurusan j "
                + "LEFT JOIN mst_kelas k ON k.id_jurusan = j.id_jurusan "
                + "LEFT JOIN mst_siswa s ON s.id_kelas = k.id_kelas "
                + "GROUP BY j.id_jurusan, j.kode_jurusan, j.nama_jurusan, j.singkatan "
                + "ORDER BY j.kode_jurusan";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                MajorReportItem item = new MajorReportItem();
                item.setCode(resultSet.getString("kode_jurusan"));
                item.setName(resultSet.getString("nama_jurusan"));
                item.setAbbreviation(resultSet.getString("singkatan"));
                item.setClassCount(resultSet.getInt("jumlah_kelas"));
                item.setStudentCount(resultSet.getInt("jumlah_siswa"));
                items.add(item);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return items;
    }

    @Override
    public List<Room> getRoomReport() {
        return roomDAO.getAllRooms();
    }

    @Override
    public List<ClassroomReportItem> getClassroomReport() {
        List<ClassroomReportItem> items = new ArrayList<>();

        String sql = "SELECT k.kode_kelas, k.nama_kelas, k.tingkat, "
                + "COUNT(s.id_siswa) AS jumlah_siswa, "
                + "j.id_jurusan, j.kode_jurusan, j.nama_jurusan, j.singkatan "
                + "FROM mst_kelas k "
                + "LEFT JOIN mst_jurusan j ON k.id_jurusan = j.id_jurusan "
                + "LEFT JOIN mst_siswa s ON s.id_kelas = k.id_kelas "
                + "GROUP BY k.id_kelas, k.kode_kelas, k.nama_kelas, k.tingkat, "
                + "j.id_jurusan, j.kode_jurusan, j.nama_jurusan, j.singkatan "
                + "ORDER BY k.kode_kelas";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                ClassroomReportItem item = new ClassroomReportItem();
                item.setCode(resultSet.getString("kode_kelas"));
                item.setName(resultSet.getString("nama_kelas"));
                item.setGrade(resultSet.getString("tingkat"));
                item.setStudentCount(resultSet.getInt("jumlah_siswa"));

                int majorId = resultSet.getInt("id_jurusan");
                if (!resultSet.wasNull()) {
                    Major major = new Major();
                    major.setId(majorId);
                    major.setCode(resultSet.getString("kode_jurusan"));
                    major.setName(resultSet.getString("nama_jurusan"));
                    major.setAbbreviation(resultSet.getString("singkatan"));
                    item.setMajor(major);
                }

                items.add(item);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return items;
    }

    @Override
    public List<Subject> getSubjectReport(int majorId) {
        List<Subject> subjects = new ArrayList<>();
 
        String sql = "SELECT m.kode_mapel, m.nama_mapel, m.tingkat, m.jenis, m.kkm "
                + "FROM mst_mapel m "
                + "WHERE m.id_jurusan = ? "
                + "ORDER BY m.kode_mapel";
 
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
 
            preparedStatement.setInt(1, majorId);
 
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Subject subject = new Subject();
                    subject.setCode(resultSet.getString("kode_mapel"));
                    subject.setName(resultSet.getString("nama_mapel"));
                    subject.setGrade(resultSet.getString("tingkat"));
                    subject.setType(resultSet.getString("jenis"));
                    subject.setPassingGrade(resultSet.getInt("kkm"));
                    subjects.add(subject);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
 
        return subjects;
    }
}