package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.ExamTypeDAO;
import co.id.model.ExamType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ExamTypeDAOImpl extends DatabaseConfiguration implements ExamTypeDAO {

    private static final String SELECT_WITH_USAGE =
            "SELECT m.*, (SELECT COUNT(*) FROM trx_jadwal j WHERE j.jenis_ujian = m.kode) AS jumlah_jadwal "
            + "FROM mst_jenis_ujian m ";

    @Override
    public List<ExamType> getAll() {
        List<ExamType> types = new ArrayList<>();
        String sql = SELECT_WITH_USAGE + "ORDER BY m.urutan, m.kode";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                types.add(map(resultSet));
            }
        } catch (SQLException exception) {
            throw readError(exception);
        }
        return types;
    }

    @Override
    public ExamType getByCode(String code) {
        return getOne(SELECT_WITH_USAGE + "WHERE m.kode = ?", code);
    }

    @Override
    public ExamType getById(int id) {
        return getOne(SELECT_WITH_USAGE + "WHERE m.id_jenis_ujian = ?", id);
    }

    private ExamType getOne(String sql, Object key) {
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setObject(1, key);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() ? map(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw readError(exception);
        }
    }

    // Ganti kode jenis: jadwal yang memakai kode lama ikut berubah lewat foreign key ON UPDATE CASCADE.
    @Override
    public void saveOrUpdate(ExamType examType) {
        String sqlInsert = "INSERT INTO mst_jenis_ujian(kode, nama_lengkap, aktif, urutan, diubah_oleh) VALUES (?, ?, ?, ?, ?)";
        String sqlUpdate = "UPDATE mst_jenis_ujian SET kode = ?, nama_lengkap = ?, aktif = ?, urutan = ?, diubah_oleh = ? "
                + "WHERE id_jenis_ujian = ?";

        try (Connection connection = getConnection()) {
            boolean isNew = examType.getId() == 0;
            try (PreparedStatement preparedStatement = connection.prepareStatement(isNew ? sqlInsert : sqlUpdate,
                    Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, examType.getCode());
                preparedStatement.setString(2, examType.getFullName());
                preparedStatement.setBoolean(3, examType.isActive());
                preparedStatement.setInt(4, examType.getOrder());
                preparedStatement.setString(5, examType.getUpdatedBy());
                if (!isNew) {
                    preparedStatement.setInt(6, examType.getId());
                }
                preparedStatement.executeUpdate();
                if (isNew) {
                    try (ResultSet keys = preparedStatement.getGeneratedKeys()) {
                        if (keys.next()) {
                            examType.setId(keys.getInt(1));
                        }
                    }
                }
            }
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 1062) {
                throw new IllegalStateException("Kode jenis ujian \"" + examType.getCode() + "\" sudah ada.", exception);
            }
            throw new IllegalStateException("Gagal menyimpan jenis ujian: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM mst_jenis_ujian WHERE id_jenis_ujian = ?";
        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 1451) {
                throw new IllegalStateException("Jenis ujian ini masih dipakai jadwal ujian, jadi tidak bisa dihapus. "
                        + "Nonaktifkan saja supaya tidak muncul di pilihan jadwal baru.", exception);
            }
            throw new IllegalStateException("Gagal menghapus jenis ujian: " + exception.getMessage(), exception);
        }
    }

    // Tabel belum ada = database belum dimigrasi; beri pesan yang jelas, jangan diam-diam mengembalikan daftar kosong
    private static IllegalStateException readError(SQLException exception) {
        if (exception.getErrorCode() == 1146) {
            return new IllegalStateException("Tabel jenis ujian belum ada di database. Jalankan UPDATE-DATABASE.bat "
                    + "(migrasi_jenis_ujian.sql) di komputer server.", exception);
        }
        return new IllegalStateException("Gagal membaca data jenis ujian: " + exception.getMessage(), exception);
    }

    private ExamType map(ResultSet resultSet) throws SQLException {
        ExamType type = new ExamType();
        type.setId(resultSet.getInt("id_jenis_ujian"));
        type.setCode(resultSet.getString("kode"));
        type.setFullName(resultSet.getString("nama_lengkap"));
        type.setActive(resultSet.getBoolean("aktif"));
        type.setOrder(resultSet.getInt("urutan"));
        type.setUsageCount(resultSet.getInt("jumlah_jadwal"));
        Timestamp updated = resultSet.getTimestamp("tanggal_update");
        type.setUpdatedAt(updated != null ? updated.toLocalDateTime() : null);
        type.setUpdatedBy(resultSet.getString("diubah_oleh"));
        return type;
    }
}