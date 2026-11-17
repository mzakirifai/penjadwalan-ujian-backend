package co.id.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pembaca tabel Excel yang umum dipakai (satu tabel per sheet):
 * - baris judul dicari otomatis (baris pertama yang memuat semua kolom wajib),
 * - kolom dikenali dari nama header (huruf besar/kecil, spasi, titik diabaikan),
 * - baris yang seluruh kolomnya kosong dilewati.
 *
 * Tidak bergantung pada library Excel, jadi mudah diuji.
 */
public class TableParser {

    public static class Row {
        private final String sheet;
        private final int rowNumber;
        private final Map<String, String> values;

        Row(String sheet, int rowNumber, Map<String, String> values) {
            this.sheet = sheet;
            this.rowNumber = rowNumber;
            this.values = values;
        }

        public String getSheet() { return sheet; }
        public int getRowNumber() { return rowNumber; }

        /** Nilai kolom (sudah di-trim); string kosong jika kolom tidak ada. */
        public String get(String field) {
            String v = values.get(field);
            return v == null ? "" : v;
        }
    }

    public static List<Row> parse(String sheet, List<List<String>> grid,
                                  Map<String, List<String>> aliases, List<String> required) {
        return parse(sheet, grid, aliases, required, Set.of());
    }

    /**
     * @param aliases       field -> daftar nama header yang diterima (sudah dalam bentuk normal, lihat {@link #normalize})
     * @param required      field yang harus ada di baris header
     * @param contextFields field yang boleh diambil dari baris judul di atas tabel, mis. "Jurusan : TKJ"
     *                      (dipakai sebagai nilai default untuk baris yang kolomnya kosong / tidak ada)
     */
    public static List<Row> parse(String sheet, List<List<String>> grid,
                                  Map<String, List<String>> aliases, List<String> required,
                                  Set<String> contextFields) {
        List<Row> result = new ArrayList<>();

        Map<String, String> headerToField = new HashMap<>();
        for (Map.Entry<String, List<String>> e : aliases.entrySet()) {
            for (String alias : e.getValue()) {
                headerToField.put(alias, e.getKey());
            }
        }

        // cari baris header
        int headerRow = -1;
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (int r = 0; r < grid.size() && headerRow < 0; r++) {
            Map<String, Integer> found = new LinkedHashMap<>();
            List<String> row = grid.get(r);
            for (int c = 0; c < row.size(); c++) {
                String field = headerToField.get(normalize(row.get(c)));
                if (field != null) {
                    found.putIfAbsent(field, c);
                }
            }
            if (found.keySet().containsAll(required)) {
                headerRow = r;
                columns = found;
            }
        }
        if (headerRow < 0) {
            return result;
        }

        Map<String, String> defaults = readContext(grid, headerRow, headerToField, contextFields);

        for (int r = headerRow + 1; r < grid.size(); r++) {
            List<String> row = grid.get(r);
            Map<String, String> values = new HashMap<>();
            boolean allBlank = true;

            for (Map.Entry<String, Integer> col : columns.entrySet()) {
                String v = col.getValue() < row.size() && row.get(col.getValue()) != null
                        ? row.get(col.getValue()).trim() : "";
                values.put(col.getKey(), v);
                if (!v.isEmpty()) {
                    allBlank = false;
                }
            }
            if (!allBlank) {
                for (Map.Entry<String, String> d : defaults.entrySet()) {
                    String current = values.get(d.getKey());
                    if (current == null || current.isEmpty()) {
                        values.put(d.getKey(), d.getValue());
                    }
                }
                result.add(new Row(sheet, r + 1, values));
            }
        }
        return result;
    }

    /**
     * Membaca baris di atas header yang berbentuk "Label : Nilai" (satu sel, atau Label dan Nilai di sel
     * bersebelahan), mis. "Jurusan : Teknik Komputer dan Jaringan". Hanya field di contextFields yang diambil,
     * supaya baris kop surat seperti "Email: sekolah@gmail.com" tidak ikut terbaca sebagai data.
     */
    private static Map<String, String> readContext(List<List<String>> grid, int headerRow,
                                                   Map<String, String> headerToField, Set<String> contextFields) {
        Map<String, String> defaults = new HashMap<>();
        if (contextFields.isEmpty()) {
            return defaults;
        }

        for (int r = 0; r < headerRow; r++) {
            List<String> row = grid.get(r);
            for (int c = 0; c < row.size(); c++) {
                String text = row.get(c) == null ? "" : row.get(c).trim();
                int colon = text.indexOf(':');
                String label = colon >= 0 ? text.substring(0, colon) : text;
                String field = headerToField.get(normalize(label));
                if (field == null || !contextFields.contains(field) || defaults.containsKey(field)) {
                    continue;
                }

                String value = colon >= 0 ? text.substring(colon + 1).trim() : "";
                for (int n = c + 1; value.isEmpty() && n < row.size(); n++) {
                    String next = row.get(n) == null ? "" : row.get(n).trim();
                    if (!next.isEmpty() && !next.equals(":")) {
                        value = next;
                    }
                }
                if (!value.isEmpty()) {
                    defaults.put(field, value);
                }
            }
        }
        return defaults;
    }

    /** "No. HP" -> "nohp", "Jenis Kelamin" -> "jeniskelamin" */
    public static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}