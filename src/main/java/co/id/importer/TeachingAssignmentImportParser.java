package co.id.importer;

import co.id.model.Classroom;
import co.id.model.Subject;
import co.id.model.Teacher;
import co.id.model.TeachingAssignment;
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
 * Mengubah baris Excel menjadi data Pengampu (guru x mapel x kelas) + validasi.
 *
 * Kolom Excel: Guru | NIP | Kode Mapel | Mapel | Kode Guru | Kelas  (satu baris = satu kelas)
 * Wajib: Guru, Kelas, dan Kode Mapel atau Mapel.
 * - Guru dicari lewat NIP bila diisi, selain itu lewat nama lengkap (gelar ikut dibandingkan).
 * - Mapel dicari lewat Kode Mapel; bila kosong lewat nama mapel (harus tepat satu mapel dengan nama itu).
 * - Kelas dicari lewat nama kelas (mis. X TKJ 1) atau kode kelas.
 * - Kode Guru 1-99 atau kosong/"-"; dalam satu mapel satu kode hanya untuk satu guru.
 * - Mapel harus berlaku untuk kelasnya (jurusan & tingkat), sama dengan aturan form Pengampu.
 * Pengampu yang sudah terdaftar (guru, mapel, kelas sama) tidak diimpor ulang.
 */
public class TeachingAssignmentImportParser {

    public static final List<String> COLUMNS = List.of("Guru", "NIP", "Kode Mapel", "Mapel", "Kode Guru", "Kelas");
    public static final List<String> REQUIRED = List.of("teacher", "classroom");
    public static final Map<String, List<String>> ALIASES = new LinkedHashMap<>();

    static {
        ALIASES.put("teacher", List.of("guru", "namaguru", "pengampu", "gurupengampu"));
        ALIASES.put("nip", List.of("nip", "nipguru"));
        ALIASES.put("subjectCode", List.of("kodemapel"));
        ALIASES.put("subjectName", List.of("mapel", "namamapel", "matapelajaran"));
        ALIASES.put("teacherCode", List.of("kodeguru", "kode"));
        ALIASES.put("classroom", List.of("kelas", "namakelas", "rombel"));
    }

    public static List<ImportItem<TeachingAssignment>> parse(List<TableParser.Row> rows, Collection<Teacher> teachers,
                                                             Collection<Subject> subjects, Collection<Classroom> classrooms,
                                                             Collection<TeachingAssignment> existing) {
        Map<String, List<Teacher>> teachersByName = new HashMap<>();
        Map<String, List<Teacher>> teachersByBaseName = new HashMap<>();
        Map<String, Teacher> teachersByNip = new HashMap<>();
        for (Teacher t : teachers) {
            teachersByName.computeIfAbsent(TableParser.normalize(t.getName()), k -> new ArrayList<>()).add(t);
            teachersByBaseName.computeIfAbsent(TeacherImportParser.baseName(t.getName()), k -> new ArrayList<>()).add(t);
            if (t.getNip() != null && !t.getNip().isBlank()) {
                teachersByNip.put(t.getNip().replaceAll("\\s+", ""), t);
            }
        }

        Map<String, Subject> subjectsByCode = new HashMap<>();
        Map<String, List<Subject>> subjectsByName = new HashMap<>();
        for (Subject s : subjects) {
            if (s.getCode() != null) {
                subjectsByCode.put(s.getCode().trim().toUpperCase(Locale.ROOT), s);
            }
            subjectsByName.computeIfAbsent(TableParser.normalize(s.getName()), k -> new ArrayList<>()).add(s);
        }

        Map<String, Classroom> classroomsByKey = new HashMap<>();
        for (Classroom c : classrooms) {
            classroomsByKey.put(TableParser.normalize(c.getName()), c);
            if (c.getCode() != null) {
                classroomsByKey.putIfAbsent(TableParser.normalize(c.getCode()), c);
            }
        }

        // Pengampu yang sudah ada: kombinasi guru-mapel-kelas, dan pemakai kode per mapel
        Map<String, TeachingAssignment> existingByCombo = new HashMap<>();
        Map<String, Teacher> codeOwners = new HashMap<>();
        for (TeachingAssignment ta : existing) {
            existingByCombo.put(combo(ta.getTeacher().getId(), ta.getSubject().getId(), ta.getClassroom().getId()), ta);
            if (ta.getTeacherCode() != null) {
                codeOwners.putIfAbsent(ta.getSubject().getId() + "#" + ta.getTeacherCode(), ta.getTeacher());
            }
        }

        Set<String> seen = new HashSet<>();
        List<ImportItem<TeachingAssignment>> result = new ArrayList<>();

        for (TableParser.Row row : rows) {
            String teacherName = row.get("teacher").replaceAll("\\s+", " ");
            String nip = row.get("nip").replaceAll("\\s+", "");
            String subjectCode = row.get("subjectCode").replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
            String subjectName = row.get("subjectName").replaceAll("\\s+", " ");
            String codeText = row.get("teacherCode").trim();
            String classroomName = row.get("classroom").replaceAll("\\s+", " ");

            // Baris rekap/catatan di bawah tabel dilewati, mis. "JUMLAH" tanpa kelas dan mapel
            if (classroomName.isEmpty() && subjectCode.isEmpty() && subjectName.isEmpty()) {
                continue;
            }

            Teacher teacher = null;
            String error = null;
            String warning = null;

            if (!nip.isEmpty()) {
                teacher = teachersByNip.get(nip);
            }
            if (teacher == null && !teacherName.isEmpty()) {
                List<Teacher> found = teachersByName.getOrDefault(TableParser.normalize(teacherName), List.of());
                if (found.size() == 1) {
                    teacher = found.get(0);
                } else if (found.size() > 1) {
                    error = "Ada " + found.size() + " guru bernama \"" + teacherName + "\", isi kolom NIP";
                } else {
                    // Penulisan gelar sering berbeda (A.Md vs A.Md.I.Kom.): cocokkan nama tanpa gelar bila hasilnya tunggal
                    List<Teacher> byBase = teachersByBaseName.getOrDefault(TeacherImportParser.baseName(teacherName), List.of());
                    if (byBase.size() == 1) {
                        teacher = byBase.get(0);
                        warning = "nama guru dicocokkan tanpa gelar: " + teacher.getName();
                    }
                }
            }

            Subject subject = null;
            if (!subjectCode.isEmpty()) {
                subject = subjectsByCode.get(subjectCode);
            } else if (!subjectName.isEmpty()) {
                List<Subject> found = subjectsByName.getOrDefault(TableParser.normalize(subjectName), List.of());
                if (found.size() == 1) {
                    subject = found.get(0);
                } else if (found.size() > 1 && error == null) {
                    error = "Ada " + found.size() + " mapel bernama \"" + subjectName + "\", isi kolom Kode Mapel";
                }
            }

            Classroom classroom = classroomsByKey.get(TableParser.normalize(classroomName));
            Integer teacherCode = parseCode(codeText);

            if (error != null) {
                // sudah terisi (nama ganda)
            } else if (teacherName.isEmpty() && nip.isEmpty()) {
                error = "Guru kosong";
            } else if (teacher == null) {
                error = "Guru \"" + (teacherName.isEmpty() ? nip : teacherName) + "\" tidak ditemukan di data Guru";
            } else if (subjectCode.isEmpty() && subjectName.isEmpty()) {
                error = "Kode Mapel / Mapel kosong";
            } else if (subject == null) {
                error = !subjectCode.isEmpty()
                        ? "Kode Mapel " + subjectCode + " tidak ditemukan (impor Mapel dulu)"
                        : "Mapel \"" + subjectName + "\" tidak ditemukan (impor Mapel dulu)";
            } else if (classroomName.isEmpty()) {
                error = "Kelas kosong";
            } else if (classroom == null) {
                error = "Kelas \"" + classroomName + "\" tidak ditemukan";
            } else if (!codeText.isEmpty() && !codeText.equals("-") && teacherCode == null) {
                error = "Kode Guru harus angka 1 - 99 atau kosong";
            } else if (!isSubjectApplicable(subject, classroom)) {
                error = "Mapel " + subjectLabel(subject) + " tidak berlaku untuk kelas " + classroom.getName();
            } else {
                TeachingAssignment same = existingByCombo.get(combo(teacher.getId(), subject.getId(), classroom.getId()));
                Teacher codeOwner = teacherCode == null ? null : codeOwners.get(subject.getId() + "#" + teacherCode);
                if (same != null) {
                    error = "Sudah terdaftar, dilewati";
                } else if (!seen.add(combo(teacher.getId(), subject.getId(), classroom.getId()))) {
                    error = "Baris ganda di file";
                } else if (codeOwner != null && codeOwner.getId() != teacher.getId()) {
                    error = "Kode " + teacherCode + " pada mapel " + subject.getName() + " sudah dipakai "
                            + codeOwner.getName();
                }
            }

            // Kode yang lolos dicatat supaya baris berikutnya di file tidak memakai kode yang sama untuk guru lain
            if (error == null && teacherCode != null) {
                codeOwners.putIfAbsent(subject.getId() + "#" + teacherCode, teacher);
            }

            TeachingAssignment ta = new TeachingAssignment();
            ta.setTeacher(teacher);
            ta.setSubject(subject);
            ta.setClassroom(classroom);
            ta.setTeacherCode(teacherCode);

            result.add(new ImportItem<>(row.getSheet(), row.getRowNumber(),
                    List.of(teacher != null ? teacher.getName() : teacherName, nip.isEmpty() ? "-" : nip,
                            subject != null && subject.getCode() != null ? subject.getCode() : subjectCode,
                            subject != null ? subject.getName() : subjectName,
                            teacherCode != null ? String.valueOf(teacherCode) : "-",
                            classroom != null ? classroom.getName() : classroomName),
                    ta, error, error == null ? warning : null));
        }
        return result;
    }

    /** Sama dengan aturan form Pengampu: mapel Umum untuk semua jurusan, Kejuruan hanya untuk jurusannya; tingkat harus cocok. */
    public static boolean isSubjectApplicable(Subject subject, Classroom classroom) {
        boolean typeMatch = "Umum".equalsIgnoreCase(subject.getType())
                || (subject.getMajor() != null && classroom.getMajor() != null
                    && subject.getMajor().getId() == classroom.getMajor().getId());
        boolean gradeMatch = subject.getGrade() == null || subject.getGrade().equalsIgnoreCase(classroom.getGrade());
        return typeMatch && gradeMatch;
    }

    private static String subjectLabel(Subject subject) {
        StringBuilder label = new StringBuilder(subject.getName());
        if (subject.getMajor() != null) {
            label.append(" ").append(subject.getMajor().getAbbreviation() != null
                    ? subject.getMajor().getAbbreviation() : subject.getMajor().getName());
        }
        if (subject.getGrade() != null) {
            label.append(" (").append(subject.getGrade()).append(")");
        }
        return label.toString();
    }

    private static String combo(int teacherId, int subjectId, int classroomId) {
        return teacherId + "-" + subjectId + "-" + classroomId;
    }

    private static Integer parseCode(String value) {
        if (value.isEmpty() || value.equals("-")) {
            return null;
        }
        try {
            double d = Double.parseDouble(value.replace(',', '.'));
            if (d != Math.rint(d)) {
                return null;
            }
            int code = (int) d;
            return code >= 1 && code <= 99 ? code : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}