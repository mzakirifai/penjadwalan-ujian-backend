package co.id.dao;

import co.id.model.Room;
import co.id.model.SessionOccupancy;
import java.util.List;

public interface RoomDAO {
    public List<Room> getAllRooms();
    public List<Room> getRoomBy(String keyword);
    public List<Room> getRooms(int page, int size);
    public int countRooms();
    public Room getById(int id);
    public void saveOrUpdate(Room room);
    public void delete(int id);
    /** Sesi mulai hari ini yang kursinya di ruangan ini lebih banyak dari kapasitas, terbanyak dulu. */
    public List<SessionOccupancy> findUpcomingOverCapacity(int roomId, int capacity);
}