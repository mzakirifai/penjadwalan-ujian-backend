package co.id.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mengubah isi satu sheet (berupa grid teks) menjadi daftar StudentImportRow.
 *
 * Kelebihan:
 * - Header dicari otomatis (baris yang memuat "NIS" dan "Nama"), jadi judul/logo di atas tabel tidak masalah.
 * - Mendukung beberapa blok tabel berdampingan dalam satu sheet (seperti file daftar nama sekolah:
 *   lima blok "No | NIS | NISN | Nama | ..." ke samping).
 * - Kolom dikenali dari nama header, bukan dari posisi, sehingga sheet yang tidak punya kolom L/P
 *   (kelas XI dan XII) tetap terbaca.
 * - Baris kosong dan baris rekap di bawah tabel (mis. "34 | tkj") dilewati diam-diam. Baris yang
 *   punya Nama tapi NIS kosong (atau NIS angka tapi Nama kosong) tetap dibaca agar bisa ditandai error.
 *
 * Kelas ini tidak bergantung pada library Excel apa pun, sehingga mudah diuji.
 */
public class StudentSheetParser {

    private static final int FIELD_NIS = 0, FIELD_NAME = 1, FIELD_GENDER = 2,
            FIELD_CLASS = 3, FIELD_NISN = 4;

    /** Nama sheet yang dianggap tingkat kelas. */
    public static String gradeFromSheetName(String sheetName) {
        if (sheetName == null) {
            return null;
        }
        String s = sheetName.trim().toUpperCase();
        return switch (s) {
            case "X", "10" -> "X";
            case "XI", "11" -> "XI";
            case "XII", "12" -> "XII";
            default -> null;
        };
    }

    public static List<StudentImportRow> parse(String sheetName, List<List<String>> grid) {
        List<StudentImportRow> result = new ArrayList<>();
        String grade = gradeFromSheetName(sheetName);

        int headerRow = findHeaderRow(grid);
        if (headerRow < 0) {
            return result;
        }

        List<String> header = grid.get(headerRow);
        List<Map<Integer, Integer>> blocks = detectBlocks(header);

        for (int r = headerRow + 1; r < grid.size(); r++) {
            List<String> row = grid.get(r);

            for (Map<Integer, Integer> block : blocks) {
                String nis = cell(row, block.get(FIELD_NIS));
                String name = cell(row, block.get(FIELD_NAME));

                // Lewati baris kosong dan baris rekap di bawah tabel (mis. "34 | tkj"):
                // baris dianggap data siswa jika ada Nama, atau NIS berupa angka.
                if (name.isEmpty() && !nis.matches("\\d+")) {
                    continue;
                }

                result.add(new StudentImportRow(
                        sheetName,
                        r + 1,
                        nis,
                        cell(row, block.get(FIELD_NISN)),
                        name,
                        normalizeGender(cell(row, block.get(FIELD_GENDER))),
                        grade,
                        cell(row, block.get(FIELD_CLASS))
                ));
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- header

    private static int findHeaderRow(List<List<String>> grid) {
        for (int r = 0; r < grid.size(); r++) {
            boolean hasNis = false, hasName = false;
            for (String value : grid.get(r)) {
                int field = fieldOf(value);
                if (field == FIELD_NIS) hasNis = true;
                if (field == FIELD_NAME) hasName = true;
            }
            if (hasNis && hasName) {
                return r;
            }
        }
        return -1;
    }

    /**
     * Membagi baris header menjadi blok. Satu blok dimulai setiap kali kolom NIS
     * muncul lagi; di dalam blok, tiap kolom dipetakan ke field-nya.
     */
    private static List<Map<Integer, Integer>> detectBlocks(List<String> header) {
        List<Map<Integer, Integer>> blocks = new ArrayList<>();
        Map<Integer, Integer> current = null;

        for (int c = 0; c < header.size(); c++) {
            int field = fieldOf(header.get(c));
            if (field < 0) {
                continue;
            }
            // NIS kedua dst. (atau field yang sudah terisi) berarti blok baru dimulai
            if (current == null || current.containsKey(field)) {
                current = new HashMap<>();
                blocks.add(current);
            }
            current.put(field, c);
        }

        // buang blok yang tidak punya NIS/Nama
        blocks.removeIf(b -> !b.containsKey(FIELD_NIS) || !b.containsKey(FIELD_NAME));
        return blocks;
    }

    private static int fieldOf(String headerText) {
        String key = headerText == null ? "" : headerText.toLowerCase().replaceAll("[^a-z0-9]", "");
        return switch (key) {
            case "nis" -> FIELD_NIS;
            case "nisn" -> FIELD_NISN;
            case "nama", "namasiswa" -> FIELD_NAME;
            case "lp", "jk", "jeniskelamin", "gender" -> FIELD_GENDER;
            case "kelas", "namakelas" -> FIELD_CLASS;
            default -> -1;
        };
    }

    // ---------------------------------------------------------------- helper

    private static String cell(List<String> row, Integer index) {
        if (index == null || index >= row.size()) {
            return "";
        }
        String value = row.get(index);
        return value == null ? "" : value.trim();
    }

    private static String normalizeGender(String value) {
        String v = value.trim().toLowerCase();
        if (v.equals("l") || v.startsWith("laki")) return "Laki-laki";
        if (v.equals("p") || v.startsWith("perempuan") || v.startsWith("wanita")) return "Perempuan";
        return null;
    }
}