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
        settingsDTO.setFilePartSize(settings.getFilePartSize());
        settingsDTO.setHandlersCount(settings.getHandlersCount());
        settingsDTO.setHandlerTimeout(settings.getHandlerTimeout());
        settingsDTO.setThreadsCount(settings.getThreadsCount());
        return settingsDTO;
    }

    public Settings convertToEntity(SettingsDTO settingsDTO) {
        Settings settings = new Settings();
        settings.setOutputFilesDirectory(settingsDTO.getOutputFilesDirectory());
        settings.setInputFilesDirectory(settingsDTO.getInputFilesDirectory());
        settings.setFilePartSize(settingsDTO.getFilePartSize());
        settings.setHandlersCount(settingsDTO.getHandlersCount());
        settings.setHandlerTimeout(settingsDTO.getHandlerTimeout());
        settings.setThreadsCount(settingsDTO.getThreadsCount());
        return settings;
    }
}
