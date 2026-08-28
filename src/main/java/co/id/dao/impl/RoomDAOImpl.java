package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.RoomDAO;
import co.id.model.Room;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class RoomDAOImpl extends DatabaseConfiguration implements RoomDAO{

    @Override
    public List<Room> getAllRooms() {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM mst_ruangan";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                rooms.add(mapResultSetToRoom(resultSet));
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return rooms;
    }

    @Override
    public List<Room> getRoomBy(String keyword) {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM mst_ruangan WHERE nama_ruangan LIKE ? OR kode_ruangan LIKE ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, "%" + keyword + "%");
            preparedStatement.setString(2, "%" + keyword + "%");

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    rooms.add(mapResultSetToRoom(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return rooms;
    }

    @Override
    public List<Room> getRooms(int page, int size) {
        List<Room> rooms = new ArrayList<>();
        String sql = "SELECT * FROM mst_ruangan ORDER BY id_ruangan LIMIT ? OFFSET ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, size);
            preparedStatement.setInt(2, (page - 1) * size);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    rooms.add(mapResultSetToRoom(resultSet));
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return rooms;
    }

    @Override
    public int countRooms() {
        String sql = "SELECT COUNT(*) AS total FROM mst_ruangan";

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
    public Room getById(int id) {
        String sql = "SELECT * FROM mst_ruangan WHERE id_ruangan = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToRoom(resultSet);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public void saveOrUpdate(Room room) {
        String sqlInsert = "INSERT INTO mst_ruangan(nama_ruangan, lantai, kapasitas, diubah_oleh) VALUES (?, ?, ?, ?)";
        String sqlUpdate = "UPDATE mst_ruangan SET nama_ruangan = ?, lantai = ?, kapasitas = ?, diubah_oleh = ? WHERE id_ruangan = ?";

        try (Connection connection = getConnection()) {
            if (room.getId() == 0) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setString(1, room.getName());
                    preparedStatement.setInt(2, room.getFloor());
                    preparedStatement.setInt(3, room.getCapacity());
                    preparedStatement.setString(4, room.getUpdatedBy());

                    preparedStatement.executeUpdate();

                    try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int newId = generatedKeys.getInt(1);
                            String generatedCode = "R" + String.format("%03d", newId);

                            try (PreparedStatement updateCode = connection.prepareStatement(
                                    "UPDATE mst_ruangan SET kode_ruangan = ? WHERE id_ruangan = ?")) {
                                updateCode.setString(1, generatedCode);
                                updateCode.setInt(2, newId);
                                updateCode.executeUpdate();
                            }

                            room.setId(newId);
                            room.setCode(generatedCode);
                        }
                    }
                }
            } else {
                try (PreparedStatement preparedStatement = connection.prepareStatement(sqlUpdate)) {
                    preparedStatement.setString(1, room.getName());
                    preparedStatement.setInt(2, room.getFloor());
                    preparedStatement.setInt(3, room.getCapacity());
                    preparedStatement.setString(4, room.getUpdatedBy());
                    preparedStatement.setInt(5, room.getId());

                    preparedStatement.executeUpdate();
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM mst_ruangan WHERE id_ruangan = ?";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
    
    // Helper method untuk mapping ResultSet ke object Room
    private Room mapResultSetToRoom(ResultSet resultSet) throws Exception {
        Room room = new Room();
        room.setId(resultSet.getInt("id_ruangan"));
        room.setCode(resultSet.getString("kode_ruangan"));
        room.setName(resultSet.getString("nama_ruangan"));
        room.setFloor(resultSet.getInt("lantai"));
        room.setCapacity(resultSet.getInt("kapasitas"));

        Timestamp createdAt = resultSet.getTimestamp("tanggal_buat");
        if (createdAt != null) {
            room.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = resultSet.getTimestamp("tanggal_update");
        if (updatedAt != null) {
            room.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        room.setUpdatedBy(resultSet.getString("diubah_oleh"));

        return room;
    }
}
