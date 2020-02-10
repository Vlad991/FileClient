package com.filesynch.client;

import javax.swing.*;

public class Logger {
    public static JTextArea log;

    public static void log(String stringToLog) {
        String COLOR = "\033[0;31m";
        String RESET = "\033[0m";
        System.out.println(COLOR + stringToLog + RESET);
        log.append(stringToLog);
        log.append("\n");
    }
}
