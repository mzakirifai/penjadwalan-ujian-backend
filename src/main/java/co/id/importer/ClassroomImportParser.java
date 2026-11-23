package co.id.importer;

import co.id.model.Classroom;
import co.id.model.Major;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mengubah baris Excel/CSV menjadi data Kelas + validasi.
 *
 * Kolom: Tingkat | Jurusan | Rombel | Nama Kelas
 * Nama kelas disusun sama seperti form Kelas: Tingkat + singkatan Jurusan + Rombel (mis. "X TKJ 1").
 * Bisa diisi dengan dua cara:
 *  1. Tingkat + Jurusan (+ Rombel bila ada), Nama Kelas boleh kosong; atau
 *  2. hanya Nama Kelas (mis. "XI DKV 2"); tingkat, jurusan, dan rombel dibaca dari namanya.
 * Kode kelas dibuat otomatis (K001, K002, ...) seperti saat menambah kelas lewat form.
 */
public class ClassroomImportParser {

    public static final List<String> COLUMNS = List.of("Tingkat", "Jurusan", "Rombel", "Nama Kelas");
    /** Header wajib cara 1. */
    public static final List<String> REQUIRED = List.of("grade", "major");
    /** Header wajib cara 2 (dipakai bila file tidak punya kolom Tingkat + Jurusan). */
    public static final List<String> REQUIRED_BY_NAME = List.of("name");
    public static final Map<String, List<String>> ALIASES = new LinkedHashMap<>();

    static {
        ALIASES.put("grade", List.of("tingkat", "kelastingkat", "level"));
        ALIASES.put("major", List.of("jurusan", "kompetensikeahlian", "programkeahlian", "konsentrasikeahlian"));
        ALIASES.put("rombel", List.of("rombel", "rombonganbelajar", "nomorrombel", "norombel"));
        ALIASES.put("name", List.of("namakelas", "kelas"));
    }

    public static List<ImportItem<Classroom>> parse(List<TableParser.Row> rows, Collection<Classroom> existing,
                                                   Collection<Major> majors) {
        Set<String> existingNames = new HashSet<>();
        for (Classroom c : existing) {
            existingNames.add(TableParser.normalize(c.getName()));
        }

        Set<String> seen = new HashSet<>();
        List<ImportItem<Classroom>> result = new ArrayList<>();

        for (TableParser.Row row : rows) {
            String gradeText = row.get("grade");
            String majorText = row.get("major");
            String rombel = row.get("rombel").trim().toUpperCase();
            String nameText = row.get("name").trim().replaceAll("\\s+", " ");

            if (gradeText.isEmpty() && majorText.isEmpty() && rombel.isEmpty() && nameText.isEmpty()) {
                continue;
            }

            String grade = normalizeGrade(gradeText);
            Major major = majorText.isEmpty() ? null : findMajor(majorText, majors);
            String error = null;
            String warning = null;

            // Cara 2: hanya nama kelas -> pecah menjadi tingkat, jurusan, rombel
            if (gradeText.isEmpty() && majorText.isEmpty() && !nameText.isEmpty()) {
                String[] parts = nameText.split(" ");
                grade = normalizeGrade(parts[0]);
                if (grade == null) {
                    error = "Nama kelas harus diawali tingkat X, XI, atau XII (mis. X TKJ 1)";
                } else if (parts.length < 2) {
                    error = "Nama kelas harus memuat jurusan (mis. X TKJ 1)";
                } else {
                    String last = parts[parts.length - 1];
                    String middleWithRombel = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
                    String middleWithoutRombel = parts.length > 2
                            ? String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 1)) : null;
                    // "X TKJ 1": jurusan TKJ + rombel 1; "X TKJ": tanpa rombel
                    if (middleWithoutRombel != null && last.length() == 1
                            && (major = findMajor(middleWithoutRombel, majors)) != null) {
                        rombel = last.toUpperCase();
                    } else {
                        major = findMajor(middleWithRombel, majors);
                        rombel = "";
                    }
                    if (major == null) {
                        error = "Jurusan pada nama kelas \"" + nameText + "\" tidak ditemukan di menu Jurusan";
                    }
                }
            } else if (grade == null) {
                error = gradeText.isEmpty() ? "Tingkat kosong" : "Tingkat harus X, XI, atau XII";
            } else if (majorText.isEmpty()) {
                error = "Jurusan kosong";
            } else if (major == null) {
                error = "Jurusan \"" + majorText + "\" tidak ditemukan di menu Jurusan";
            }

            if (error == null && !rombel.isEmpty() && !rombel.matches("[0-9A-Z]")) {
                error = "Rombel harus 1 angka/huruf (mis. 1, 2) atau kosong";
            }

            String name = error == null ? buildName(grade, major, rombel) : nameText;
            if (error == null) {
                if (name.length() > 100) {
                    error = "Nama kelas lebih dari 100 karakter";
                } else if (existingNames.contains(TableParser.normalize(name))) {
                    error = "Kelas \"" + name + "\" sudah ada, dilewati";
                } else if (!seen.add(TableParser.normalize(name))) {
                    error = "Kelas \"" + name + "\" ganda di file";
                } else if (!nameText.isEmpty() && !TableParser.normalize(nameText).equals(TableParser.normalize(name))) {
                    warning = "disimpan sebagai \"" + name + "\"";
                }
            }

            Classroom classroom = new Classroom();
            classroom.setName(name);
            classroom.setGrade(grade);
            classroom.setMajor(major);

            result.add(new ImportItem<>(row.getSheet(), row.getRowNumber(),
                    List.of(grade == null ? gradeText : grade,
                            major == null ? majorText : label(major),
                            rombel.isEmpty() ? "-" : rombel,
                            name.isEmpty() ? "-" : name),
                    classroom, error, warning));
        }
        return result;
    }

    /** Sama dengan form Kelas: "X TKJ 1", atau "X TKJ" bila tanpa rombel. */
    public static String buildName(String grade, Major major, String rombel) {
        String base = grade + " " + label(major);
        return rombel == null || rombel.isEmpty() ? base : base + " " + rombel;
    }

    private static String label(Major major) {
        return major.getAbbreviation() != null && !major.getAbbreviation().isBlank()
                ? major.getAbbreviation() : major.getName();
    }

    /** Jurusan dicari dari singkatan (TKJ) atau nama lengkap (Teknik Komputer dan Jaringan). */
    static Major findMajor(String text, Collection<Major> majors) {
        String key = TableParser.normalize(text);
        if (key.isEmpty()) {
            return null;
        }
        for (Major m : majors) {
            if (key.equals(TableParser.normalize(m.getAbbreviation()))) {
                return m;
            }
        }
        for (Major m : majors) {
            if (key.equals(TableParser.normalize(m.getName())) || key.equals(TableParser.normalize(m.getCode()))) {
                return m;
            }
        }
        return null;
    }

    static String normalizeGrade(String value) {
        String v = value == null ? "" : value.trim().toUpperCase();
        return switch (v) {
            case "X", "10" -> "X";
            case "XI", "11" -> "XI";
            case "XII", "12" -> "XII";
            default -> null;
        };
    }
}