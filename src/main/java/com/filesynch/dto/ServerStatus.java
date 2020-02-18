package com.filesynch.dto;

public enum ServerStatus {
    SERVER_STOP("SERVER_STOP"),
    SERVER_WORK("SERVER_WORK");

    private String status;

    private ServerStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
