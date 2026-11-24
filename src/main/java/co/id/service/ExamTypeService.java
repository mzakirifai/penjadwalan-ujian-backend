package co.id.service;

import co.id.model.ExamType;
import java.util.List;

/**
 * Jenis ujian (menu Master > Jenis Ujian). Dipakai oleh form jadwal, filter laporan, judul laporan,
 * dan impor jadwal, sehingga sekolah bisa menambah/mengubah jenis ujian tanpa mengubah program.
 */
public interface ExamTypeService {
    /** Semua jenis (aktif dan nonaktif) beserta jumlah jadwal yang memakainya. */
    public List<ExamType> getAll();
    /** Kode jenis yang aktif, untuk pilihan jadwal baru dan impor. */
    public List<String> getActiveCodes();
    /** Semua kode (termasuk nonaktif), untuk filter laporan supaya jadwal lama tetap bisa dicetak. */
    public List<String> getAllCodes();
    /** Nama lengkap huruf besar untuk judul laporan, mis. "AAS" -> "ASESMEN AKHIR SEMESTER". */
    public String label(String code);
    /** Kode resmi untuk teks bebas (mis. "aas" dari file impor -> "AAS"); null bila tidak terdaftar. */
    public ExamType findByCode(String code);
    public void save(ExamType examType, String currentUsername, String currentUserRole);
    public void delete(int id, String currentUserRole);
}