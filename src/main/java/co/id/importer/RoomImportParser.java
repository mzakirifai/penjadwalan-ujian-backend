package co.id.importer;

import co.id.model.Room;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mengubah baris Excel/CSV menjadi data Ruangan + validasi.
 *
 * Kolom: Nama Ruangan | Lantai | Kapasitas
 * Wajib: Nama Ruangan dan Kapasitas (lebih dari 0, sama seperti form Ruangan).
 * Lantai boleh kosong (dianggap lantai 1). Kode ruangan dibuat otomatis (R001, R002, ...).
 * Ruangan dengan nama yang sama (spasi dan huruf besar/kecil diabaikan: "R 1" = "r1") tidak diimport ulang.
 */
public class RoomImportParser {

    public static final List<String> COLUMNS = List.of("Nama Ruangan", "Lantai", "Kapasitas");
    public static final List<String> REQUIRED = List.of("name", "capacity");
    public static final Map<String, List<String>> ALIASES = new LinkedHashMap<>();
    public static final int MAX_CAPACITY = 999;

    static {
        ALIASES.put("name", List.of("namaruangan", "ruangan", "namaruang", "ruang"));
        ALIASES.put("floor", List.of("lantai", "lt"));
        ALIASES.put("capacity", List.of("kapasitas", "dayatampung", "jumlahkursi", "kursi"));
    }

    public static List<ImportItem<Room>> parse(List<TableParser.Row> rows, Collection<Room> existing) {
        Set<String> existingNames = new HashSet<>();
        for (Room r : existing) {
            existingNames.add(TableParser.normalize(r.getName()));
        }

        Set<String> seen = new HashSet<>();
        List<ImportItem<Room>> result = new ArrayList<>();

        for (TableParser.Row row : rows) {
            String name = row.get("name").trim().replaceAll("\\s+", " ");
            String floorText = row.get("floor").trim();
            String capacityText = row.get("capacity").trim();

            if (name.isEmpty() && floorText.isEmpty() && capacityText.isEmpty()) {
                continue;
            }

            Integer floor = floorText.isEmpty() ? Integer.valueOf(1) : toInt(floorText);
            Integer capacity = toInt(capacityText);
            String error = null;

            if (name.isEmpty()) {
                error = "Nama Ruangan kosong";
            } else if (name.length() > 100) {
                error = "Nama Ruangan lebih dari 100 karakter";
            } else if (floor == null || floor < 0 || floor > 99) {
                error = "Lantai harus angka 0 - 99 (atau dikosongkan)";
            } else if (capacityText.isEmpty()) {
                error = "Kapasitas kosong";
            } else if (capacity == null || capacity <= 0 || capacity > MAX_CAPACITY) {
                error = "Kapasitas harus angka 1 - " + MAX_CAPACITY;
            } else if (existingNames.contains(TableParser.normalize(name))) {
                error = "Ruangan \"" + name + "\" sudah ada, dilewati";
            } else if (!seen.add(TableParser.normalize(name))) {
                error = "Ruangan \"" + name + "\" ganda di file";
            }

            Room room = new Room();
            room.setName(name);
            room.setFloor(floor == null ? 1 : floor);
            room.setCapacity(capacity == null ? 0 : capacity);

            result.add(new ImportItem<>(row.getSheet(), row.getRowNumber(),
                    List.of(name.isEmpty() ? "-" : name,
                            floor == null ? floorText : String.valueOf(floor),
                            capacity != null ? String.valueOf(capacity) : capacityText.isEmpty() ? "-" : capacityText),
                    room, error));
        }
        return result;
    }

    /** "36", "36.0" (angka dari Excel), atau "36 siswa" -> 36; selain itu null. */
    private static Integer toInt(String text) {
        String t = text.trim().replaceAll("(?i)\\s*(siswa|orang|kursi)$", "");
        if (t.matches("\\d+([.,]0+)?")) {
            try {
                return Integer.parseInt(t.replaceAll("[.,]0+$", ""));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }
}