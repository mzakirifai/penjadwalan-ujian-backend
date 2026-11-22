package co.id.importer;

import co.id.model.Student;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Mengubah baris Excel dari bagian keuangan menjadi perubahan status administrasi siswa.
 *
 * Kolom Excel: NIS | Nama Siswa | Kelas | Status | Keterangan
 * Wajib: NIS, Status. Nama Siswa dan Kelas hanya untuk memudahkan pengisian (tidak mengubah data siswa).
 * - Status: "Lunas" atau "Belum Lunas" (juga dikenali: L, BL, Sudah, Belum, Tidak, dst).
 * - Siswa dicari lewat NIS; NIS yang tidak terdaftar ditolak.
 * - Baris yang status & keterangannya sama dengan data sekarang tidak ditampilkan (tidak ada yang berubah),
 *   supaya template berisi semua siswa tidak memenuhi pratinjau.
 */
public class AdminStatusImportParser {

    public static final List<String> COLUMNS = List.of("NIS", "Nama Siswa", "Kelas", "Status", "Keterangan");
    public static final List<String> REQUIRED = List.of("nis", "status");
    public static final Map<String, List<String>> ALIASES = new LinkedHashMap<>();

    static {
        ALIASES.put("nis", List.of("nis", "nomorinduksiswa", "noinduk"));
        ALIASES.put("name", List.of("namasiswa", "nama"));
        ALIASES.put("classroom", List.of("kelas", "rombel"));
        ALIASES.put("status", List.of("status", "statusadministrasi", "administrasi", "statuspembayaran", "keuangan"));
        ALIASES.put("note", List.of("keterangan", "catatan", "ket"));
    }

    public static List<ImportItem<Student>> parse(List<TableParser.Row> rows, Collection<Student> students) {
        Map<String, Student> byNis = new HashMap<>();
        for (Student s : students) {
            if (s.getNis() != null) {
                byNis.put(s.getNis().trim(), s);
            }
        }

        Set<String> seen = new HashSet<>();
        List<ImportItem<Student>> result = new ArrayList<>();

        for (TableParser.Row row : rows) {
            String nis = row.get("nis").replaceAll("\\s+", "");
            String statusText = row.get("status");
            String note = row.get("note").trim();

            // baris rekap/catatan di bawah tabel (tanpa NIS dan status) dilewati
            if (nis.isEmpty() && statusText.isEmpty()) {
                continue;
            }

            Student existing = byNis.get(nis);
            String status = normalizeStatus(statusText);
            String error = null;

            if (nis.isEmpty()) {
                error = "NIS kosong";
            } else if (existing == null) {
                error = "NIS tidak terdaftar di data siswa";
            } else if (status == null) {
                error = "Status harus Lunas atau Belum Lunas";
            } else if (note.length() > 255) {
                error = "Keterangan lebih dari 255 karakter";
            } else if (!seen.add(nis)) {
                error = "NIS ganda di file";
            } else if (status.equals(existing.getAdminStatus())
                    && Objects.equals(note.isEmpty() ? null : note, blankToNull(existing.getAdminNote()))) {
                continue;
            }

            Student change = new Student();
            if (existing != null) {
                change.setId(existing.getId());
                change.setNis(existing.getNis());
                change.setName(existing.getName());
            }
            change.setAdminStatus(status);
            change.setAdminNote(note.isEmpty() ? null : note);

            String warning = null;
            if (error == null && existing != null && Student.ADMIN_UNPAID.equals(existing.getAdminStatus())
                    && Student.ADMIN_PAID.equals(status)) {
                warning = "berubah menjadi Lunas";
            } else if (error == null && existing != null && Student.ADMIN_PAID.equals(existing.getAdminStatus())
                    && Student.ADMIN_UNPAID.equals(status)) {
                warning = "berubah menjadi Belum Lunas";
            }

            result.add(new ImportItem<>(row.getSheet(), row.getRowNumber(),
                    List.of(nis,
                            existing != null ? existing.getName() : row.get("name"),
                            existing != null && existing.getClassroom() != null ? existing.getClassroom().getName() : row.get("classroom"),
                            status != null ? status : statusText,
                            note),
                    change, error, warning));
        }
        return result;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /** "Lunas" / "Belum Lunas", atau null bila tidak dikenali. */
    public static String normalizeStatus(String value) {
        String v = TableParser.normalize(value);
        switch (v) {
            case "lunas": case "l": case "sudah": case "sudahlunas": case "ya": case "y": case "ok":
                return Student.ADMIN_PAID;
            case "belumlunas": case "belum": case "bl": case "tidak": case "tidaklunas": case "t":
            case "menunggak": case "tunggakan": case "nunggak":
                return Student.ADMIN_UNPAID;
            default:
                return null;
        }
    }
}