package co.id.service;

import co.id.model.SchoolSettings;

public interface SchoolSettingsService {
    public SchoolSettings getSettings();
    public void updateSettings(SchoolSettings settings, String currentUsername);
}