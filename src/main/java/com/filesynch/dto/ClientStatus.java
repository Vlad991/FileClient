package com.filesynch.dto;

public enum ClientStatus {
    NEW("NEW"),
    CLIENT_FIRST("CLIENT_FIRST"),
    CLIENT_WORK("CLIENT_WORK"),
    CLIENT_PAUSE("CLIENT_PAUSE"),
    CLIENT_ARCHIVE("CLIENT_ARCHIVE");


    private String status;

    private ClientStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
