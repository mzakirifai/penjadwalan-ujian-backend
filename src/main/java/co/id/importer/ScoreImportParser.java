package co.id.importer;

import co.id.model.ExamParticipant;
import co.id.model.ExamSchedule;
import co.id.model.ExamScore;
import co.id.model.Student;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mengubah baris Excel/CSV menjadi data nilai satu ujian + validasi.
 *
 * Kolom file : NIS (atau NISN / No. Peserta) | Nama (opsional, hanya pembanding) | Nilai
 * Siswa dicocokkan dengan peserta ujian yang dipilih, lewat NIS, NISN, atau nomor peserta.
 * Nilai di database bilangan bulat 0-100, jadi nilai desimal (85,5 / 85.5) dibulatkan dan diberi catatan.
 */
public class ScoreImportParser {

    public static final List<String> COLUMNS = List.of("NIS / No. Peserta", "Nama", "Nilai", "Nilai Lama");
    public static final List<String> REQUIRED = List.of("score");
    public static final Map<String, List<String>> ALIASES = new LinkedHashMap<>();

    static {
        ALIASES.put("nis", List.of("nis", "nomorinduk", "noinduk", "nomorinduksiswa"));
        ALIASES.put("nisn", List.of("nisn"));
        ALIASES.put("participantNo", List.of("nopeserta", "nomorpeserta", "nopesertaujian", "nomorpesertaujian",
                "noujian", "nomorujian", "idpeserta"));
        ALIASES.put("name", List.of("nama", "namasiswa", "namapeserta"));
        ALIASES.put("score", List.of("nilai", "skor", "score", "hasil", "nilaiakhir", "nilaiujian", "totalnilai"));
    }

    public static List<ImportItem<ExamScore>> parse(List<TableParser.Row> rows, ExamSchedule schedule,
                                                    List<ExamParticipant> participants,
                                                    List<ExamScore> existingScores) {
        Map<String, ExamParticipant> byNis = new HashMap<>();
        Map<String, ExamParticipant> byNisn = new HashMap<>();
        Map<String, ExamParticipant> byNumber = new HashMap<>();
        for (ExamParticipant p : participants) {
            if (p.getStudent() != null && p.getStudent().getNis() != null) {
                byNis.put(cleanId(p.getStudent().getNis()), p);
            }
            if (p.getStudent() != null && p.getStudent().getNisn() != null) {
                byNisn.put(cleanId(p.getStudent().getNisn()), p);
            }
            if (p.getParticipantNumber() != null) {
                byNumber.put(cleanId(p.getParticipantNumber()), p);
            }
        }

        Map<Integer, Integer> oldScoreByStudent = new HashMap<>();
        for (ExamScore es : existingScores) {
            if (es.getStudent() != null) {
                oldScoreByStudent.put(es.getStudent().getId(), es.getScore());
            }
        }

        Set<Integer> seenStudents = new HashSet<>();
        List<ImportItem<ExamScore>> result = new ArrayList<>();

        for (TableParser.Row row : rows) {
            String nis = cleanId(row.get("nis"));
            String nisn = cleanId(row.get("nisn"));
            String number = cleanId(row.get("participantNo"));
            String fileName = row.get("name");
            String scoreText = row.get("score");

            String id = !nis.isEmpty() ? nis : (!number.isEmpty() ? number : nisn);
            String shownId = firstNonEmpty(compact(row.get("nis")), compact(row.get("participantNo")), compact(row.get("nisn")));

            ExamParticipant participant = null;
            if (!nis.isEmpty()) {
                participant = byNis.get(nis);
            }
            if (participant == null && !number.isEmpty()) {
                participant = byNumber.get(number);
            }
            if (participant == null && !nisn.isEmpty()) {
                participant = byNisn.get(nisn);
            }
            // kolom NIS kadang berisi nomor peserta (file scanner)
            if (participant == null && !nis.isEmpty()) {
                participant = byNumber.get(nis);
            }

            String error = null;
            String warning = null;
            Integer score = null;

            BigDecimal parsed = parseScore(scoreText);
            if (id.isEmpty()) {
                error = "NIS / No. Peserta kosong";
            } else if (participant == null) {
                error = "Bukan peserta ujian ini (NIS / No. Peserta tidak ditemukan)";
            } else if (scoreText.isEmpty()) {
                error = "Nilai kosong";
            } else if (parsed == null) {
                error = "Nilai bukan angka";
            } else if (parsed.compareTo(BigDecimal.ZERO) < 0 || parsed.compareTo(BigDecimal.valueOf(100)) > 0) {
                error = "Nilai harus 0 - 100";
            } else if (!seenStudents.add(participant.getStudent().getId())) {
                error = "Siswa ada dua kali di file";
            }

            Integer oldScore = null;
            if (error == null) {
                score = parsed.setScale(0, RoundingMode.HALF_UP).intValue();
                if (parsed.stripTrailingZeros().scale() > 0) {
                    warning = "dibulatkan dari " + parsed.stripTrailingZeros().toPlainString();
                }
                oldScore = oldScoreByStudent.get(participant.getStudent().getId());
                if (oldScore != null) {
                    String note = oldScore.equals(score) ? "sama dengan nilai lama" : "menimpa nilai lama " + oldScore;
                    warning = warning == null ? note : warning + "; " + note;
                }
            }

            Student student = participant != null ? participant.getStudent() : null;
            String shownName = student != null ? student.getName() : fileName;

            ExamScore payload = null;
            if (error == null) {
                payload = new ExamScore();
                payload.setExamSchedule(schedule);
                payload.setStudent(student);
                payload.setScore(score);
            }

            result.add(new ImportItem<>(row.getSheet(), row.getRowNumber(),
                    List.of(shownId, shownName == null ? "" : shownName,
                            score != null ? String.valueOf(score) : scoreText,
                            oldScore != null ? String.valueOf(oldScore) : "-"),
                    payload, error, warning));
        }
        return result;
    }

    /** "85", "85,5", "85.5" -> angka; selain itu null. */
    static BigDecimal parseScore(String text) {
        String t = text == null ? "" : text.trim().replace(" ", "");
        if (t.matches("^\\d+,\\d+$")) {
            t = t.replace(',', '.');
        }
        if (!t.matches("^\\d+(\\.\\d+)?$")) {
            return null;
        }
        return new BigDecimal(t);
    }

    private static String compact(String value) {
        String v = value == null ? "" : value.replaceAll("\\s+", "");
        return v.matches("^\\d+\\.0+$") ? v.substring(0, v.indexOf('.')) : v;
    }

    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (!v.isEmpty()) {
                return v;
            }
        }
        return "";
    }

    /** Spasi dibuang; "123456.0" (angka dari Excel/CSV) menjadi "123456"; huruf besar/kecil disamakan. */
    static String cleanId(String value) {
        String v = value == null ? "" : value.replaceAll("\\s+", "");
        if (v.matches("^\\d+\\.0+$")) {
            v = v.substring(0, v.indexOf('.'));
        }
        return v.toLowerCase();
    }
}