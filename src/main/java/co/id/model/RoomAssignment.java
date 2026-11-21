package co.id.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Isi satu ruangan pada satu sesi ujian: siswa dari beberapa kelas dan satu pengawas. */
public class RoomAssignment {
    private final Room room;
    private Teacher supervisor;
    private final List<SeatAssignment> seats = new ArrayList<>();

    public RoomAssignment(Room room) {
        this.room = room;
    }

    public Room getRoom() { return room; }
    public Teacher getSupervisor() { return supervisor; }
    public void setSupervisor(Teacher supervisor) { this.supervisor = supervisor; }
    public List<SeatAssignment> getSeats() { return seats; }
    public int getFilled() { return seats.size(); }

    /** Jumlah siswa per kelas di ruangan ini, urut sesuai kemunculan. */
    public Map<String, Integer> getClassCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SeatAssignment seat : seats) {
            counts.merge(seat.getClassName(), 1, Integer::sum);
        }
        return counts;
    }

    /** Mis. "X TKJ 1 (18), XI TP (16)". */
    public String describeClasses() {
        StringBuilder sb = new StringBuilder();
        getClassCounts().forEach((name, count) -> {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(name).append(" (").append(count).append(")");
        });
        return sb.toString();
    }
}
