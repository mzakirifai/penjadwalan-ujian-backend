package co.id.service.impl;

import co.id.dao.SchoolSettingsDAO;
import co.id.dao.impl.SchoolSettingsDAOImpl;
import co.id.model.SchoolSettings;
import co.id.service.SchoolSettingsService;

public class SchoolSettingsServiceImpl implements SchoolSettingsService {
    private final SchoolSettingsDAO schoolSettingsDAO;

    public SchoolSettingsServiceImpl() {
        schoolSettingsDAO = new SchoolSettingsDAOImpl();
    }

    @Override
    public SchoolSettings getSettings() {
        return schoolSettingsDAO.getSettings();
    }

    @Override
    public void updateSettings(SchoolSettings settings, String currentUsername) {
        if (settings.getSchoolName() == null || settings.getSchoolName().isBlank()) {
            throw new IllegalArgumentException("Nama sekolah wajib diisi.");
        }
        if (settings.getActiveSemester() == null || settings.getActiveSemester().isBlank()) {
            throw new IllegalArgumentException("Semester aktif wajib diisi.");
        }
        if (settings.getActiveAcademicYear() == null || settings.getActiveAcademicYear().isBlank()) {
            throw new IllegalArgumentException("Tahun akademik aktif wajib diisi.");
        }

        settings.setUpdatedBy(currentUsername);
        schoolSettingsDAO.updateSettings(settings);
    }
}