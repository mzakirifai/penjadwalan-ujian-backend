package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.ReportDAO;
import co.id.dao.RoomDAO;
import co.id.dao.impl.RoomDAOImpl;
import co.id.model.Room;
import co.id.model.report.MajorReportItem;
import java.sql.Connection;
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
}