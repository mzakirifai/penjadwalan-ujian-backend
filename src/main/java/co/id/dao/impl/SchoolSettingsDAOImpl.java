package co.id.dao.impl;

import co.id.config.DatabaseConfiguration;
import co.id.dao.SchoolSettingsDAO;
import co.id.model.SchoolSettings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

public class SchoolSettingsDAOImpl extends DatabaseConfiguration implements SchoolSettingsDAO {

    @Override
    public SchoolSettings getSettings() {
        String sql = "SELECT * FROM pengaturan WHERE id = 1";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (resultSet.next()) {
                SchoolSettings settings = new SchoolSettings();
                settings.setId(resultSet.getInt("id"));
                settings.setSchoolName(resultSet.getString("nama_sekolah"));
                settings.setAddress(resultSet.getString("alamat"));
                settings.setPhone(resultSet.getString("telepon"));
                settings.setEmail(resultSet.getString("email"));
                settings.setPrincipalName(resultSet.getString("nama_kepala_sekolah"));
                settings.setPrincipalNip(resultSet.getString("nip_kepala_sekolah"));
                settings.setActiveSemester(resultSet.getString("semester_aktif"));
                settings.setActiveAcademicYear(resultSet.getString("tahun_akademik_aktif"));

                Timestamp updatedAt = resultSet.getTimestamp("tanggal_update");
                if (updatedAt != null) {
                    settings.setUpdatedAt(updatedAt.toLocalDateTime());
                }
                settings.setUpdatedBy(resultSet.getString("diubah_oleh"));

                return settings;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public void updateSettings(SchoolSettings settings) {
        String sql = "UPDATE pengaturan SET "
                + "nama_sekolah = ?, alamat = ?, telepon = ?, email = ?, "
                + "nama_kepala_sekolah = ?, nip_kepala_sekolah = ?, "
                + "semester_aktif = ?, tahun_akademik_aktif = ?, "
                + "tanggal_update = CURRENT_TIMESTAMP, diubah_oleh = ? "
                + "WHERE id = 1";

        try (Connection connection = getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            preparedStatement.setString(1, settings.getSchoolName());
            preparedStatement.setString(2, settings.getAddress());
            preparedStatement.setString(3, settings.getPhone());
            preparedStatement.setString(4, settings.getEmail());
            preparedStatement.setString(5, settings.getPrincipalName());
            preparedStatement.setString(6, settings.getPrincipalNip());
            preparedStatement.setString(7, settings.getActiveSemester());
            preparedStatement.setString(8, settings.getActiveAcademicYear());
            preparedStatement.setString(9, settings.getUpdatedBy());

            preparedStatement.executeUpdate();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}