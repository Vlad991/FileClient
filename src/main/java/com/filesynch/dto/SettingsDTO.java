package com.filesynch.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class SettingsDTO implements Serializable {
    static final long serialVersionUID = 60L;
    private String outputFilesDirectory;
    private String inputFilesDirectory;
    private int filePartSize;
    private int handlersCount;
    private int handlerTimeout;
    private int threadsCount;
}