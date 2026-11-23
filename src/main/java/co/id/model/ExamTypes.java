package co.id.model;

import java.util.List;

/**
 * Daftar jenis ujian yang dipakai di seluruh aplikasi (form jadwal, laporan, import jadwal).
 * Tambah jenis baru cukup di sini.
 */
public final class ExamTypes {

    public static final List<String> ALL = List.of("UTS", "UAS", "AAS", "UKK", "PRAKTIK");

    private ExamTypes() {
    }

    /** Nama lengkap untuk judul laporan, mis. AAS -> ASESMEN AKHIR SEMESTER. */
    public static String label(String examType) {
        if (examType == null) {
            return "";
        }
        return switch (examType) {
            case "UTS" -> "UJIAN TENGAH SEMESTER";
            case "UAS" -> "UJIAN AKHIR SEMESTER";
            case "AAS" -> "ASESMEN AKHIR SEMESTER";
            case "UKK" -> "UJIAN KENAIKAN KELAS";
            case "PRAKTIK" -> "UJIAN PRAKTIK";
            default -> examType;
        };
    }
}