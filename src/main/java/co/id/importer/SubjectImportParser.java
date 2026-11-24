package co.id.importer;

import co.id.model.Major;
import co.id.model.Subject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Mengubah baris Excel menjadi data Mapel + validasi.
 *
 * Kolom Excel: Kode Mapel | Nama Mapel | Jenis | Jurusan | Tingkat | KKM
 * Wajib: Nama Mapel, Jenis. Aturannya sama dengan form Mapel:
 * - Jenis "Umum": Jurusan dikosongkan; "Kejuruan": Jurusan wajib (singkatan, mis. TKJ, atau nama lengkap).
 * - Tingkat X / XI / XII, atau kosong untuk semua tingkat.
 * - KKM 0-100; bila kosong dipakai {@link #DEFAULT_KKM} dan diberi catatan di pratinjau.
 * - Kode Mapel opsional. Bila diisi, kode itu yang dipakai (supaya sheet Pengampu di file yang sama bisa
 *   merujuk mapel lewat kodenya); bila kosong, kode dibuat otomatis seperti saat menambah lewat form.
 * Mapel yang sudah terdaftar (kode sama, atau nama + jurusan + tingkat sama) tidak diimpor ulang.
 */
public class SubjectImportParser {

    public static final int DEFAULT_KKM = 75;
    public static final List<String> COLUMNS =
            List.of("Kode Mapel", "Nama Mapel", "Jenis", "Jurusan", "Tingkat", "KKM");
    // Jenis ikut wajib supaya sheet lain di file yang sama (mis. Pengampu yang punya kolom "Mapel") tidak ikut terbaca
    public static final List<String> REQUIRED = List.of("name", "type");
    public static final Map<String, List<String>> ALIASES = new LinkedHashMap<>();

    static {
        ALIASES.put("code", List.of("kodemapel", "kode"));
        ALIASES.put("name", List.of("namamapel", "mapel", "matapelajaran", "namamatapelajaran"));
        ALIASES.put("type", List.of("jenis", "jenismapel", "kelompok"));
        ALIASES.put("major", List.of("jurusan", "kompetensikeahlian", "konsentrasikeahlian"));
        ALIASES.put("grade", List.of("tingkat", "kelas"));
        ALIASES.put("kkm", List.of("kkm", "kktp"));
    }

    public static List<ImportItem<Subject>> parse(List<TableParser.Row> rows, Collection<Subject> existing,
                                                  Collection<Major> majors) {
        Map<String, Subject> existingByCode = new HashMap<>();
        Map<String, Subject> existingByKey = new HashMap<>();
        for (Subject s : existing) {
            if (s.getCode() != null) {
                existingByCode.put(s.getCode().trim().toUpperCase(Locale.ROOT), s);
            }
            existingByKey.put(key(s.getName(), s.getMajor(), s.getGrade()), s);
        }

        Set<String> seenCodes = new HashSet<>();
        Set<String> seenKeys = new HashSet<>();
        List<ImportItem<Subject>> result = new ArrayList<>();

        for (TableParser.Row row : rows) {
            String code = row.get("code").replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
            String name = row.get("name").replaceAll("\\s+", " ");
            String typeText = row.get("type");
            String majorText = blankIfEmptyMarker(row.get("major"));
            String gradeText = blankIfEmptyMarker(row.get("grade"));
            String kkmText = row.get("kkm");

            // Baris rekap/catatan di bawah tabel dilewati: tanpa jenis dan kolom lain kosong, mis. "Jumlah mapel: 30"
            if (typeText.isEmpty() && code.isEmpty() && majorText.isEmpty() && gradeText.isEmpty() && kkmText.isEmpty()) {
                continue;
            }

            String type = normalizeType(typeText);
            Major major = findMajor(majorText, majors);
            String grade = normalizeGrade(gradeText);
            Integer kkm = parseKkm(kkmText);
            String warning = null;

            String error = null;
            if (name.isEmpty()) {
                error = "Nama Mapel kosong";
            } else if (name.length() > 100) {
                error = "Nama Mapel lebih dari 100 karakter";
            } else if (code.length() > 10) {
                error = "Kode Mapel lebih dari 10 karakter";
            } else if (type == null) {
                error = "Jenis harus Umum atau Kejuruan";
            } else if ("Umum".equals(type) && !majorText.isEmpty()) {
                error = "Mapel Umum tidak boleh punya Jurusan (kosongkan kolom Jurusan)";
            } else if ("Kejuruan".equals(type) && majorText.isEmpty()) {
                error = "Mapel Kejuruan wajib diisi Jurusan";
            } else if (!majorText.isEmpty() && major == null) {
                error = "Jurusan \"" + majorText + "\" tidak ditemukan";
            } else if (!gradeText.isEmpty() && grade == null) {
                error = "Tingkat harus X, XI, XII, atau kosong";
            } else if (!kkmText.isEmpty() && kkm == null) {
                error = "KKM harus angka 0 - 100";
            }

            if (error == null) {
                Subject sameCode = code.isEmpty() ? null : existingByCode.get(code);
                Subject sameKey = existingByKey.get(key(name, major, grade));
                if (sameCode != null && sameCode == sameKey) {
                    error = "Sudah terdaftar (" + sameCode.getCode() + "), dilewati";
                } else if (sameCode != null) {
                    // kode sudah terdaftar dengan nama/jurusan/tingkat berbeda: tidak ditimpa, cek manual di menu Mapel
                    error = "Kode " + code + " sudah terdaftar untuk mapel \"" + sameCode.getName() + "\", dilewati";
                } else if (sameKey != null) {
                    error = "Sudah terdaftar (" + sameKey.getCode() + "), dilewati";
                } else if (!code.isEmpty() && !seenCodes.add(code)) {
                    error = "Kode Mapel ganda di file";
                } else if (!seenKeys.add(key(name, major, grade))) {
                    error = "Mapel ganda di file (nama, jurusan, dan tingkat sama)";
                }
            }

            if (error == null && kkm == null) {
                kkm = DEFAULT_KKM;
                warning = "KKM kosong, dipakai " + DEFAULT_KKM;
            }

            Subject subject = new Subject();
            subject.setCode(code.isEmpty() ? null : code);
            subject.setName(name);
            subject.setType(type);
            subject.setMajor(major);
            subject.setGrade(grade);
            subject.setPassingGrade(kkm == null ? 0 : kkm);

            result.add(new ImportItem<>(row.getSheet(), row.getRowNumber(),
                    List.of(code.isEmpty() ? "(otomatis)" : code, name, type == null ? typeText : type,
                            major != null ? label(major) : majorText,
                            grade != null ? grade : (gradeText.isEmpty() ? "Semua" : gradeText),
                            kkm != null ? String.valueOf(kkm) : kkmText),
                    subject, error, warning));
        }
        return result;
    }

    /** Kunci pembanding mapel: nama (tanpa beda huruf besar/kecil & spasi) + jurusan + tingkat. */
    private static String key(String name, Major major, String grade) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        return n + "|" + (major == null ? "" : major.getId()) + "|" + (grade == null ? "" : grade);
    }

    private static String label(Major major) {
        return major.getAbbreviation() != null ? major.getAbbreviation() : major.getName();
    }

    /** "(kosong)", "-", "semua" dianggap kosong (dipakai di file contoh). */
    private static String blankIfEmptyMarker(String value) {
        String v = value.trim();
        String n = TableParser.normalize(v);
        if (v.equals("-") || n.equals("kosong") || n.startsWith("semua")) {
            return "";
        }
        return v;
    }

    private static String normalizeType(String value) {
        String v = TableParser.normalize(value);
        if (v.equals("umum") || v.equals("u") || v.equals("normatif") || v.equals("adaptif")) return "Umum";
        if (v.equals("kejuruan") || v.equals("k") || v.equals("produktif")) return "Kejuruan";
        return null;
    }

    private static Major findMajor(String value, Collection<Major> majors) {
        if (value.isEmpty()) {
            return null;
        }
        String v = TableParser.normalize(value);
        for (Major m : majors) {
            if (v.equals(TableParser.normalize(m.getAbbreviation())) || v.equals(TableParser.normalize(m.getName()))
                    || v.equals(TableParser.normalize(m.getCode()))) {
                return m;
            }
        }
        return null;
    }

    private static String normalizeGrade(String value) {
        String v = value.trim().toUpperCase(Locale.ROOT);
        switch (v) {
            case "X": case "10": return "X";
            case "XI": case "11": return "XI";
            case "XII": case "12": return "XII";
            default: return null;
        }
    }

    private static Integer parseKkm(String value) {
        if (value.isEmpty()) {
            return null;
        }
        try {
            double d = Double.parseDouble(value.replace(',', '.'));
            int kkm = (int) Math.round(d);
            return kkm >= 0 && kkm <= 100 ? kkm : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}