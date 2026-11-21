package co.id.model;

/**
 * Ketersediaan satu ruangan untuk sebuah sesi ujian.
 * usedBy berisi keterangan pemakai bila ruangan dipakai sesi lain yang waktunya bertumpuk,
 * mis. "X DKV (07:30-09:00)"; null bila ruangan kosong.
 */
public class RoomAvailability {

    private final Room room;
    private final String usedBy;

    public RoomAvailability(Room room, String usedBy) {
        this.room = room;
        this.usedBy = usedBy;
    }

    public Room getRoom() { return room; }
    public String getUsedBy() { return usedBy; }

    public boolean isAvailable() {
        return usedBy == null;
    }
}