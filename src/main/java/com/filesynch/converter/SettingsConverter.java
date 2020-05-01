package com.filesynch.converter;

import com.filesynch.dto.SettingsDTO;
import com.filesynch.entity.Settings;

public class SettingsConverter {

    public SettingsConverter() {
    }

    public SettingsDTO convertToDto(Settings settings) {
        SettingsDTO settingsDTO = new SettingsDTO();
        settingsDTO.setOutputFilesDirectory(settings.getOutputFilesDirectory());
        settingsDTO.setInputFilesDirectory(settings.getInputFilesDirectory());
        return settingsDTO;
    }

    public Settings convertToEntity(SettingsDTO settingsDTO) {
        Settings settings = new Settings();
        settings.setOutputFilesDirectory(settingsDTO.getOutputFilesDirectory());
        settings.setInputFilesDirectory(settingsDTO.getInputFilesDirectory());
        return settings;
    }
}