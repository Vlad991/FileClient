package com.filesynch.dto;

public enum ClientStatus {
    NO_CONNECTION("NO_CONNECTION"),
    CLIENT_STANDBY("CLIENT_STANDBY"), // works, but doesn't receive or send any data
    CLIENT_FIRST("CLIENT_FIRST"), // at the first start, it tries to connect to the server and sends the PCname\external IP \ pc model () Waits for binding...(client_second mode)
    CLIENT_SECOND("CLIENT_SECOND"), // waits for server answer with its username (login), goes to client_work
    CLIENT_WORK("CLIENT_WORK"), // works in normal mode (sends or receives files)
    CLIENT_ARCHIVE("CLIENT_ARCHIVE"); // not logged in client (server rejected connection), or its work is done


    private String status;

    private ClientStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
