package co.id.dao;

import co.id.model.ExamType;
import java.util.List;

public interface ExamTypeDAO {
    /** Semua jenis ujian (urut kolom urutan lalu kode), lengkap dengan jumlah jadwal yang memakainya. */
    public List<ExamType> getAll();
    public ExamType getByCode(String code);
    public ExamType getById(int id);
    public void saveOrUpdate(ExamType examType);
    public void delete(int id);
}