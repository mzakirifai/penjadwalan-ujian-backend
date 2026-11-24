package co.id.service.impl;

import co.id.dao.ExamTypeDAO;
import co.id.dao.impl.ExamTypeDAOImpl;
import co.id.model.ExamType;
import co.id.service.ExamTypeService;
import java.util.List;
import java.util.Locale;

public class ExamTypeServiceImpl implements ExamTypeService {

    /** Kode: huruf besar/angka, boleh spasi, titik, garis bawah, tanda hubung; maks. 20 karakter (kolom jadwal). */
    public static final String CODE_PATTERN = "[A-Z0-9][A-Z0-9 ._-]{0,19}";

    private final ExamTypeDAO examTypeDAO = new ExamTypeDAOImpl();

    @Override
    public List<ExamType> getAll() {
        return examTypeDAO.getAll();
    }

    @Override
    public List<String> getActiveCodes() {
        return examTypeDAO.getAll().stream().filter(ExamType::isActive).map(ExamType::getCode).toList();
    }

    @Override
    public List<String> getAllCodes() {
        return examTypeDAO.getAll().stream().map(ExamType::getCode).toList();
    }

    @Override
    public String label(String code) {
        if (code == null || code.isBlank()) {
            return "";
        }
        try {
            ExamType type = examTypeDAO.getByCode(code.trim());
            return (type != null ? type.getFullName() : code).toUpperCase(new Locale("id", "ID"));
        } catch (RuntimeException ex) {
            return code.toUpperCase(new Locale("id", "ID")); // laporan tetap bisa dicetak walau tabel bermasalah
        }
    }

    @Override
    public ExamType findByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return examTypeDAO.getByCode(normalizeCode(code));
    }

    @Override
    public void save(ExamType examType, String currentUsername, String currentUserRole) {
        ensureIsAdmin(currentUserRole);

        String code = normalizeCode(examType.getCode());
        String fullName = examType.getFullName() == null ? "" : examType.getFullName().trim().replaceAll("\\s+", " ");
        if (code.isEmpty()) {
            throw new IllegalArgumentException("Kode jenis ujian wajib diisi.");
        }
        if (!code.matches(CODE_PATTERN)) {
            throw new IllegalArgumentException("Kode jenis ujian maks. 20 karakter: huruf, angka, spasi, titik, - atau _.");
        }
        if (fullName.isEmpty()) {
            throw new IllegalArgumentException("Nama lengkap jenis ujian wajib diisi (dipakai di judul laporan).");
        }
        if (fullName.length() > 100) {
            throw new IllegalArgumentException("Nama lengkap jenis ujian maks. 100 karakter.");
        }
        if (examType.getOrder() < 0) {
            throw new IllegalArgumentException("Urutan tidak boleh negatif.");
        }

        ExamType sameCode = examTypeDAO.getByCode(code);
        if (sameCode != null && sameCode.getId() != examType.getId()) {
            throw new IllegalArgumentException("Kode jenis ujian \"" + code + "\" sudah ada.");
        }

        if (examType.getId() != 0 && !examType.isActive()) {
            ExamType old = examTypeDAO.getById(examType.getId());
            long otherActive = examTypeDAO.getAll().stream()
                    .filter(t -> t.isActive() && t.getId() != examType.getId()).count();
            if (old != null && old.isActive() && otherActive == 0) {
                throw new IllegalArgumentException("Minimal harus ada 1 jenis ujian yang aktif.");
            }
        }

        examType.setCode(code);
        examType.setFullName(fullName);
        examType.setUpdatedBy(currentUsername);
        examTypeDAO.saveOrUpdate(examType);
    }

    @Override
    public void delete(int id, String currentUserRole) {
        ensureIsAdmin(currentUserRole);
        ExamType type = examTypeDAO.getById(id);
        if (type == null) {
            return;
        }
        if (type.getUsageCount() > 0) {
            throw new IllegalStateException("Jenis ujian " + type.getCode() + " masih dipakai " + type.getUsageCount()
                    + " jadwal ujian, jadi tidak bisa dihapus. Nonaktifkan saja supaya tidak muncul di pilihan jadwal baru.");
        }
        if (type.isActive() && examTypeDAO.getAll().stream().filter(t -> t.isActive() && t.getId() != id).count() == 0) {
            throw new IllegalStateException("Jenis ujian terakhir yang aktif tidak bisa dihapus.");
        }
        examTypeDAO.delete(id);
    }

    static String normalizeCode(String code) {
        return code == null ? "" : code.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private void ensureIsAdmin(String currentUserRole) {
        if (!"admin".equalsIgnoreCase(currentUserRole)) {
            throw new SecurityException("Hanya admin yang boleh mengubah jenis ujian.");
        }
    }
}