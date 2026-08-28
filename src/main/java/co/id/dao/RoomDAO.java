package co.id.dao;

import co.id.model.Room;
import java.util.List;

public interface RoomDAO {
    public List<Room> getAllRooms();
    public List<Room> getRoomBy(String keyword);
    public List<Room> getRooms(int page, int size);
    public int countRooms();
    public Room getById(int id);
    public void saveOrUpdate(Room room);
    public void delete(int id);
}
