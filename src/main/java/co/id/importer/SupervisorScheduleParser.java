package co.id.importer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Membaca file "Jadwal Pengawas" dalam format matriks yang dipakai sekolah:
 *
 *   HARI/TANGGAL | JAM | WAKTU         | ... | R 1 | R 2 | ... | R 10 |   NO | NAMA PENGAWAS | NO | NAMA PENGAWAS
 *   SENIN, 30 November 2026 | 1 | 07.00 - 08.30 | ... |  27 |   2 | ... |   24 |    1 | Hartrasno, S.Pd | 15 | ...
 *                           |   | 08.30 - 08.50 | ... | I S T I R A H A T          |    2 | ...
 *
 * - Kolom ruangan dikenali dari judul "R 1", "R1", "R I" (angka romawi juga boleh).
 * - Tanggal dicari di sel mana pun di kiri kolom ruangan (mis. "SENIN , 30 November 2026" atau 30/11/2026),
 *   dan berlaku untuk baris-baris di bawahnya sampai tanggal berikutnya.
 * - Baris sesi = baris yang punya jam "07.00 - 08.30" dan minimal satu nomor pengawas; baris istirahat dilewati.
 * - Daftar nama dibaca dari pasangan kolom "NO" + "NAMA PENGAWAS"; bagian setelah tulisan "PANITIA" tidak dipakai.
 * Hasilnya masih berupa nomor dan nama sesuai file; pencocokan ke data Guru dan ruangan dilakukan terpisah.
 */
public final class SupervisorScheduleParser {

    /** Satu sel nomor pengawas pada satu ruangan di satu sesi. */
    public record Entry(String sheet, int rowNumber, LocalDate date, LocalTime start, LocalTime end,
                        String roomLabel, int number) { }

    public static final class Result {
        private final String sheet;
        private final Map<Integer, String> legend = new TreeMap<>();
        private final List<Entry> entries = new ArrayList<>();
        private final List<String> problems = new ArrayList<>();

        Result(String sheet) {
            this.sheet = sheet;
        }

        public String getSheet() { return sheet; }
        /** Nomor pengawas -> nama sesuai file. */
        public Map<Integer, String> getLegend() { return legend; }
        public List<Entry> getEntries() { return entries; }
        /** Hal yang tidak bisa dibaca (ditampilkan sebagai catatan, tidak menghentikan impor). */
        public List<String> getProblems() { return problems; }
    }

    private static final Pattern ROOM = Pattern.compile("(?i)^r\\s*([0-9]{1,3}|[ivx]{1,5})$");
    private static final Pattern TIME_RANGE = Pattern.compile("(\\d{1,2})[.:](\\d{2})\\s*[-–]\\s*(\\d{1,2})[.:](\\d{2})");
    private static final Pattern DATE_TEXT = Pattern.compile("(\\d{1,2})\\s+([A-Za-z]+)\\s+(\\d{4})");
    private static final Pattern DATE_NUMERIC = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})|(\\d{4})-(\\d{1,2})-(\\d{1,2})");
    private static final Pattern NUMBER = Pattern.compile("^\\d{1,3}(\\.0+)?$");
    private static final Map<String, Integer> MONTHS = new LinkedHashMap<>();

    static {
        String[][] names = {
                {"januari", "january", "jan"}, {"februari", "february", "feb", "pebruari"}, {"maret", "march", "mar"},
                {"april", "apr"}, {"mei", "may"}, {"juni", "june", "jun"}, {"juli", "july", "jul"},
                {"agustus", "august", "agu", "aug", "agt"}, {"september", "sep", "sept"},
                {"oktober", "october", "okt", "oct"}, {"november", "nopember", "nov"}, {"desember", "december", "des", "dec"}};
        for (int m = 0; m < names.length; m++) {
            for (String n : names[m]) {
                MONTHS.put(n, m + 1);
            }
        }
    }

    private SupervisorScheduleParser() {
    }

    /** Pakai sheet pertama yang punya judul kolom ruangan (R 1, R 2, ...) dan kolom "NAMA PENGAWAS". */
    public static Result parse(Map<String, List<List<String>>> sheets) {
        Result fallback = null;
        for (Map.Entry<String, List<List<String>>> sheet : sheets.entrySet()) {
            Result result = parseSheet(sheet.getKey(), sheet.getValue());
            if (result != null && !result.getEntries().isEmpty() && !result.getLegend().isEmpty()) {
                return result;
            }
            if (fallback == null && result != null) {
                fallback = result;
            }
        }
        return fallback;
    }

    static Result parseSheet(String sheetName, List<List<String>> grid) {
        // 1. baris judul ruangan
        int headerRow = -1;
        Map<Integer, String> roomColumns = new LinkedHashMap<>();
        for (int r = 0; r < grid.size() && headerRow < 0; r++) {
            Map<Integer, String> found = new LinkedHashMap<>();
            List<String> row = grid.get(r);
            for (int c = 0; c < row.size(); c++) {
                String label = roomLabel(row.get(c));
                if (label != null) {
                    found.put(c, label);
                }
            }
            if (found.size() >= 2) {
                headerRow = r;
                roomColumns = found;
            }
        }
        if (headerRow < 0) {
            return null;
        }
        Result result = new Result(sheetName);
        int firstRoomCol = roomColumns.keySet().iterator().next();

        // 2. daftar nomor -> nama (pasangan kolom NO + NAMA PENGAWAS)
        readLegend(grid, result);

        // 3. baris sesi
        LocalDate currentDate = null;
        for (int r = headerRow + 1; r < grid.size(); r++) {
            List<String> row = grid.get(r);
            LocalTime start = null;
            LocalTime end = null;
            for (int c = 0; c < Math.min(firstRoomCol, row.size()); c++) {
                String text = clean(row.get(c));
                LocalDate date = parseDate(text);
                if (date != null) {
                    currentDate = date;
                }
                Matcher time = TIME_RANGE.matcher(text);
                if (start == null && time.find()) {
                    start = time(time.group(1), time.group(2));
                    end = time(time.group(3), time.group(4));
                }
            }
            if (start == null) {
                continue;
            }
            List<Entry> rowEntries = new ArrayList<>();
            for (Map.Entry<Integer, String> room : roomColumns.entrySet()) {
                String cell = room.getKey() < row.size() ? clean(row.get(room.getKey())) : "";
                if (NUMBER.matcher(cell).matches()) {
                    rowEntries.add(new Entry(sheetName, r + 1, currentDate, start, end, room.getValue(),
                            Integer.parseInt(cell.replaceAll("\\.0+$", ""))));
                }
            }
            if (rowEntries.isEmpty()) {
                continue; // baris istirahat
            }
            if (currentDate == null) {
                result.problems.add("Baris " + (r + 1) + ": tanggal tidak ditemukan, baris dilewati.");
                continue;
            }
            if (start == null || end == null || !end.isAfter(start)) {
                result.problems.add("Baris " + (r + 1) + ": jam tidak valid, baris dilewati.");
                continue;
            }
            result.entries.addAll(rowEntries);
        }
        return result;
    }

    private static void readLegend(List<List<String>> grid, Result result) {
        for (int r = 0; r < grid.size(); r++) {
            List<String> row = grid.get(r);
            for (int c = 1; c < row.size(); c++) {
                if (!norm(row.get(c)).equals("namapengawas")) {
                    continue;
                }
                int noCol = c - 1;
                for (int rr = r + 1; rr < grid.size(); rr++) {
                    List<String> data = grid.get(rr);
                    String name = c < data.size() ? clean(data.get(c)) : "";
                    String no = noCol < data.size() ? clean(data.get(noCol)) : "";
                    if (norm(name).contains("panitia") || norm(no).contains("panitia")) {
                        break; // daftar panitia bukan pengawas ruangan
                    }
                    if (NUMBER.matcher(no).matches() && !name.isEmpty()) {
                        int number = Integer.parseInt(no.replaceAll("\\.0+$", ""));
                        String old = result.legend.putIfAbsent(number, name);
                        if (old != null && !norm(old).equals(norm(name))) {
                            result.problems.add("Nomor pengawas " + number + " dipakai dua nama: \"" + old
                                    + "\" dan \"" + name + "\". Yang dipakai: \"" + old + "\".");
                        }
                    }
                }
            }
        }
    }

    /** "R   I" -> "R 1", "R10" -> "R 10"; selain itu null. */
    public static String roomLabel(String text) {
        Matcher m = ROOM.matcher(clean(text).replaceAll("\\s+", " "));
        if (!m.matches()) {
            return null;
        }
        String n = m.group(1);
        int number = n.chars().allMatch(Character::isDigit) ? Integer.parseInt(n) : roman(n.toUpperCase(Locale.ROOT));
        return number > 0 ? "R " + number : null;
    }

    private static int roman(String s) {
        int total = 0;
        int prev = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            int v = switch (s.charAt(i)) {
                case 'I' -> 1;
                case 'V' -> 5;
                case 'X' -> 10;
                default -> 0;
            };
            total += v < prev ? -v : v;
            prev = Math.max(prev, v);
        }
        return total;
    }

    static LocalDate parseDate(String text) {
        try {
            Matcher t = DATE_TEXT.matcher(text);
            while (t.find()) {
                Integer month = MONTHS.get(t.group(2).toLowerCase(Locale.ROOT));
                if (month != null) {
                    return LocalDate.of(Integer.parseInt(t.group(3)), month, Integer.parseInt(t.group(1)));
                }
            }
            Matcher n = DATE_NUMERIC.matcher(text);
            if (n.find()) {
                return n.group(1) != null
                        ? LocalDate.of(Integer.parseInt(n.group(3)), Integer.parseInt(n.group(2)), Integer.parseInt(n.group(1)))
                        : LocalDate.of(Integer.parseInt(n.group(4)), Integer.parseInt(n.group(5)), Integer.parseInt(n.group(6)));
            }
        } catch (java.time.DateTimeException ex) {
            return null;
        }
        return null;
    }

    private static LocalTime time(String h, String m) {
        try {
            return LocalTime.of(Integer.parseInt(h), Integer.parseInt(m));
        } catch (java.time.DateTimeException ex) {
            return null;
        }
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace(' ', ' ').trim();
    }

    private static String norm(String s) {
        return clean(s).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}