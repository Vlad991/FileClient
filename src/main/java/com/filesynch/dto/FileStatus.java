package com.filesynch.dto;

public enum FileStatus {
    NOT_TRANSFERRED("NOT_TRANSFERRED"),
    TRANSFER_PROCESS("TRANSFER_PROCESS"),
    TRANSFERRED("TRANSFERRED");

    private String status;

    private FileStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
