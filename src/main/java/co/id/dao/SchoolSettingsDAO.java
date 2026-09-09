package co.id.dao;

import co.id.model.SchoolSettings;

public interface SchoolSettingsDAO {
    public SchoolSettings getSettings();
    public void updateSettings(SchoolSettings settings);
}