package com.filesynch.dto;

public enum ClientStatus {
    NEW("NEW"),
    CLIENT_WORK("CLIENT_WORK"),
    CLIENT_ARCHIVE("CLIENT_ARCHIVE");


    private String status;

    private ClientStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
