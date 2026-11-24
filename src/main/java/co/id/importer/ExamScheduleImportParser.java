package co.id.importer;

import co.id.model.Classroom;
import co.id.model.ExamSchedule;
import co.id.model.Subject;
import co.id.model.TeachingAssignment;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Mengubah baris Excel menjadi Jadwal Ujian + validasi (aturannya sama dengan form Jadwal Ujian).
 *
 * Kolom: Tanggal | Jam Mulai | Jam Selesai | Kelas | Kode Mapel | Mapel | Kode Guru | Jenis Ujian | Semester
 *        | Tahun Akademik | Mode Ujian | Keterangan   (satu baris = satu mapel diujikan di satu kelas)
 * - Mode Ujian: Offline (di ruangan, default bila kosong) atau Online (tanpa ruangan & pengawas).
 * - Tanggal: 2026-11-30 atau 30/11/2026 atau 30-11-2026. Jam: 07:00 atau 07.00.
 * - Mapel dicari lewat Kode Mapel; bila kosong lewat nama mapel (harus tepat satu). Harus berlaku untuk kelasnya.
 * - Kode Guru wajib bila mapel + kelas itu punya pengampu berkode (pilihannya diambil dari data Pengampu);
 *   bila tidak ada pengampu berkode, kode diabaikan.
 * - Jadwal yang persis sama sudah ada dilewati; kelas yang sama pada jam bertumpuk ditolak (bentrok).
 */
public class ExamScheduleImportParser {

    public static final List<String> COLUMNS = List.of("Tanggal", "Jam Mulai", "Jam Selesai", "Kelas", "Kode Mapel",
            "Mapel", "Kode Guru", "Jenis Ujian", "Semester", "Tahun Akademik", "Mode Ujian", "Keterangan");
    public static final List<String> REQUIRED = List.of("date", "startTime", "endTime", "classroom");
    public static final Map<String, List<String>> ALIASES = new LinkedHashMap<>();

    static {
        ALIASES.put("date", List.of("tanggal", "tgl", "tanggalujian"));
        ALIASES.put("startTime", List.of("jammulai", "mulai"));
        ALIASES.put("endTime", List.of("jamselesai", "selesai"));
        ALIASES.put("classroom", List.of("kelas", "namakelas", "rombel"));
        ALIASES.put("subjectCode", List.of("kodemapel"));
        ALIASES.put("subjectName", List.of("mapel", "namamapel", "matapelajaran"));
        ALIASES.put("teacherCode", List.of("kodeguru", "kode"));
        ALIASES.put("examType", List.of("jenisujian", "jenis", "ujian"));
        ALIASES.put("semester", List.of("semester"));
        ALIASES.put("academicYear", List.of("tahunakademik", "tahunajaran", "tahunpelajaran", "tahun"));
        ALIASES.put("mode", List.of("modeujian", "mode", "pelaksanaan", "onlineoffline"));
        ALIASES.put("notes", List.of("keterangan", "catatan"));
    }

    // STRICT: tanggal mustahil (mis. 31/02/2026) ditolak, bukan digeser diam-diam ke 28 Februari
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            strict("uuuu-M-d"), strict("d/M/uuuu"), strict("d-M-uuuu"), strict("d.M.uuuu"));

    private static DateTimeFormatter strict(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withResolverStyle(java.time.format.ResolverStyle.STRICT);
    }

    public static List<ImportItem<ExamSchedule>> parse(List<TableParser.Row> rows, Collection<Subject> subjects,
                                                       Collection<Classroom> classrooms,
                                                       Collection<TeachingAssignment> teachingAssignments,
                                                       Collection<ExamSchedule> existing,
                                                       Collection<String> examTypes) {
        // Jenis ujian aktif dari menu Master > Jenis Ujian; dicocokkan tanpa spasi & huruf besar/kecil
        Map<String, String> examTypeByKey = new LinkedHashMap<>();
        for (String code : examTypes) {
            examTypeByKey.put(code.replaceAll("\\s+", "").toUpperCase(Locale.ROOT), code);
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

        // Kode guru yang tersedia per mapel + kelas (sama dengan pilihan "Kode guru" di form Jadwal Ujian)
        Map<String, TreeSet<Integer>> codesBySubjectClass = new HashMap<>();
        for (TeachingAssignment ta : teachingAssignments) {
            if (ta.getTeacherCode() != null && ta.getSubject() != null && ta.getClassroom() != null) {
                codesBySubjectClass.computeIfAbsent(ta.getSubject().getId() + "-" + ta.getClassroom().getId(),
                        k -> new TreeSet<>()).add(ta.getTeacherCode());
            }
        }

        // Jadwal pembanding: yang sudah ada di aplikasi + baris file yang sudah lolos
        List<ExamSchedule> accepted = new ArrayList<>();
        Map<ExamSchedule, Integer> fileRowOf = new java.util.IdentityHashMap<>();
        for (ExamSchedule e : existing) {
            if (e.getClassroom() != null && e.getDate() != null) {
                accepted.add(e);
            }
        }

        List<ImportItem<ExamSchedule>> result = new ArrayList<>();
        for (TableParser.Row row : rows) {
            String dateText = row.get("date").trim();
            String startText = row.get("startTime").trim();
            String endText = row.get("endTime").trim();
            String classroomName = row.get("classroom").replaceAll("\\s+", " ");
            String subjectCode = row.get("subjectCode").replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
            String subjectName = row.get("subjectName").replaceAll("\\s+", " ");
            String codeText = row.get("teacherCode").trim();
            String examTypeText = row.get("examType").trim();
            String examType = examTypeByKey.getOrDefault(examTypeText.replaceAll("\\s+", "").toUpperCase(Locale.ROOT),
                    examTypeText);
            String semester = capitalize(row.get("semester").trim());
            String academicYear = row.get("academicYear").replaceAll("\\s+", "");
            String notes = row.get("notes").trim();
            String modeText = row.get("mode").trim();
            String mode = normalizeMode(modeText);

            if (dateText.isEmpty() && classroomName.isEmpty() && subjectCode.isEmpty() && subjectName.isEmpty()) {
                continue;
            }

            LocalDate date = parseDate(dateText);
            LocalTime start = parseTime(startText);
            LocalTime end = parseTime(endText);
            Classroom classroom = classroomsByKey.get(TableParser.normalize(classroomName));
            Integer teacherCode = parseCode(codeText);

            Subject subject = null;
            String error = null;
            String warning = null;
            if (!subjectCode.isEmpty()) {
                subject = subjectsByCode.get(subjectCode);
            } else if (!subjectName.isEmpty()) {
                List<Subject> found = subjectsByName.getOrDefault(TableParser.normalize(subjectName), List.of());
                if (found.size() == 1) {
                    subject = found.get(0);
                } else if (found.size() > 1) {
                    error = "Ada " + found.size() + " mapel bernama \"" + subjectName + "\", isi kolom Kode Mapel";
                }
            }

            if (error != null) {
                // nama mapel ganda
            } else if (date == null) {
                error = dateText.isEmpty() ? "Tanggal kosong" : "Tanggal \"" + dateText + "\" tidak dikenali (pakai 2026-11-30)";
            } else if (start == null || end == null) {
                error = "Jam mulai/selesai kosong atau salah (pakai 07:00)";
            } else if (!end.isAfter(start)) {
                error = "Jam selesai harus setelah jam mulai";
            } else if (!examTypeByKey.containsValue(examType)) {
                error = examTypeText.isEmpty() ? "Jenis Ujian kosong"
                        : "Jenis Ujian \"" + examTypeText + "\" tidak terdaftar/aktif. Pilihan: " + String.join(", ", examTypes);
            } else if (!semester.equals("Ganjil") && !semester.equals("Genap")) {
                error = "Semester harus Ganjil atau Genap";
            } else if (!academicYear.matches("\\d{4}/\\d{4}")) {
                error = "Tahun Akademik harus seperti 2026/2027";
            } else if (mode == null) {
                error = "Mode Ujian harus Offline atau Online (kosong = Offline)";
            } else if (classroomName.isEmpty()) {
                error = "Kelas kosong";
            } else if (classroom == null) {
                error = "Kelas \"" + classroomName + "\" tidak ditemukan";
            } else if (subjectCode.isEmpty() && subjectName.isEmpty()) {
                error = "Kode Mapel / Mapel kosong";
            } else if (subject == null) {
                error = !subjectCode.isEmpty() ? "Kode Mapel " + subjectCode + " tidak ditemukan"
                        : "Mapel \"" + subjectName + "\" tidak ditemukan";
            } else if (!codeText.isEmpty() && !codeText.equals("-") && teacherCode == null) {
                error = "Kode Guru harus angka 1 - 99 atau kosong";
            } else if (!TeachingAssignmentImportParser.isSubjectApplicable(subject, classroom)) {
                error = "Mapel " + subject.getName() + " (" + subject.getCode() + ") tidak berlaku untuk kelas "
                        + classroom.getName();
            } else {
                TreeSet<Integer> codes = codesBySubjectClass.getOrDefault(subject.getId() + "-" + classroom.getId(),
                        new TreeSet<>());
                if (!codes.isEmpty() && teacherCode == null) {
                    error = "Kode Guru wajib diisi, pilihan di data Pengampu: " + join(codes);
                } else if (!codes.isEmpty() && !codes.contains(teacherCode)) {
                    error = "Kode Guru " + teacherCode + " tidak ada di Pengampu " + subject.getName() + " kelas "
                            + classroom.getName() + " (pilihan: " + join(codes) + ")";
                } else if (codes.isEmpty() && teacherCode != null) {
                    warning = "kode guru diabaikan (mapel ini tidak punya pengampu berkode di kelas ini)";
                    teacherCode = null;
                }
            }

            ExamSchedule schedule = new ExamSchedule();
            schedule.setExamType(examType);
            schedule.setSemester(semester);
            schedule.setAcademicYear(academicYear);
            schedule.setDate(date);
            schedule.setStartTime(start);
            schedule.setEndTime(end);
            schedule.setClassroom(classroom);
            schedule.setSubject(subject);
            schedule.setTeacherCode(teacherCode);
            schedule.setNotes(notes.isEmpty() ? null : notes);
            schedule.setMode(mode);

            if (error == null) {
                for (ExamSchedule other : accepted) {
                    if (other.getClassroom().getId() != classroom.getId()) {
                        continue;
                    }
                    boolean samePeriod = examType.equals(other.getExamType()) && semester.equals(other.getSemester())
                            && academicYear.equals(other.getAcademicYear());
                    boolean sameSubject = other.getSubject() != null && other.getSubject().getId() == subject.getId()
                            && Objects.equals(other.getTeacherCode(), teacherCode);
                    boolean overlap = date.equals(other.getDate()) && start.isBefore(other.getEndTime())
                            && other.getStartTime().isBefore(end);
                    Integer otherRow = fileRowOf.get(other);
                    if (overlap && samePeriod && sameSubject && start.equals(other.getStartTime())
                            && end.equals(other.getEndTime())) {
                        if (otherRow == null && other.isOnline() != schedule.isOnline()) {
                            error = "Sudah terdaftar sebagai " + other.getMode() + "; ubah mode lewat tombol Mode Ujian";
                            break;
                        }
                        error = otherRow == null ? "Sudah terdaftar, dilewati" : "Baris ganda di file (baris " + otherRow + ")";
                        break;
                    }
                    if (overlap) {
                        error = "Bentrok: kelas " + classroom.getName() + " sudah ujian " + describe(other)
                                + (otherRow == null ? "" : " (baris " + otherRow + ")");
                        break;
                    }
                    if (samePeriod && sameSubject && warning == null) {
                        warning = "mapel ini sudah dijadwalkan untuk kelas ini pada " + describe(other)
                                + (otherRow == null ? "" : " (baris " + otherRow + ")");
                    }
                }
            }

            if (error == null) {
                accepted.add(schedule);
                fileRowOf.put(schedule, row.getRowNumber());
            }

            result.add(new ImportItem<>(row.getSheet(), row.getRowNumber(),
                    List.of(date != null ? date.toString() : dateText,
                            start != null ? start.toString() : startText,
                            end != null ? end.toString() : endText,
                            classroom != null ? classroom.getName() : classroomName,
                            subject != null && subject.getCode() != null ? subject.getCode() : subjectCode,
                            subject != null ? subject.getName() : subjectName,
                            teacherCode != null ? String.valueOf(teacherCode) : "-",
                            examType, semester, academicYear, mode == null ? modeText : mode,
                            notes.isEmpty() ? "-" : notes),
                    schedule, error, error == null ? warning : null));
        }
        return result;
    }

    /** Offline/Online; kosong = Offline; null bila tidak dikenali. */
    static String normalizeMode(String text) {
        String t = text.replaceAll("[\\s-]", "").toLowerCase(Locale.ROOT);
        if (t.isEmpty() || t.equals("offline") || t.equals("luring") || t.equals("tatapmuka") || t.equals("ruangan")) {
            return ExamSchedule.MODE_OFFLINE;
        }
        if (t.equals("online") || t.equals("daring")) {
            return ExamSchedule.MODE_ONLINE;
        }
        return null;
    }

    private static String describe(ExamSchedule s) {
        String subject = s.getSubject() == null ? "?" : s.getSubject().getName()
                + (s.getTeacherCode() == null ? "" : " (" + s.getTeacherCode() + ")");
        return subject + " " + s.getDate() + " " + s.getStartTime() + "-" + s.getEndTime();
    }

    private static String join(TreeSet<Integer> codes) {
        StringBuilder sb = new StringBuilder();
        for (Integer c : codes) {
            sb.append(sb.length() == 0 ? "" : ", ").append(c);
        }
        return sb.toString();
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT);
    }

    static LocalDate parseDate(String text) {
        String t = text.trim();
        if (t.length() > 10 && t.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
            t = t.substring(0, 10); // "2026-11-30 00:00:00"
        }
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(t, f);
            } catch (DateTimeParseException ignored) {
                // coba format berikutnya
            }
        }
        return null;
    }

    static LocalTime parseTime(String text) {
        String t = text.trim().replace('.', ':');
        if (!t.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) {
            return null;
        }
        String[] p = t.split(":");
        int h = Integer.parseInt(p[0]);
        int m = Integer.parseInt(p[1]);
        return h <= 23 && m <= 59 ? LocalTime.of(h, m) : null;
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