package com.filesynch.dto;

public enum Command {
    CONTINUE_SENDING("CONTINUE_SENDING"),
    CONTINUE_RECEIVING("CONTINUE_RECEIVING");

    private String command;

    private Command(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
