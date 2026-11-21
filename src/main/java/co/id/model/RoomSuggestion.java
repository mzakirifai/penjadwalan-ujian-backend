package co.id.model;

/** Ruangan yang kosong pada waktu tertentu, lengkap dengan info muat/tidaknya untuk satu kelas. */
public class RoomSuggestion {
    private final Room room;
    private final int studentCount; // jumlah siswa kelas yang akan memakai ruangan (0 jika tidak diketahui)

    public RoomSuggestion(Room room, int studentCount) {
        this.room = room;
        this.studentCount = studentCount;
    }

    public Room getRoom() { return room; }
    public int getStudentCount() { return studentCount; }
    public boolean isEnoughCapacity() { return room.getCapacity() >= studentCount; }

    @Override
    public String toString() {
        String info = "kapasitas " + room.getCapacity();
        if (studentCount > 0) {
            info += ", siswa " + studentCount + (isEnoughCapacity() ? "" : " - KAPASITAS KURANG");
        }
        return room.getName() + " (" + info + ")";
    }
}
