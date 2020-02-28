package com.filesynch.client;

import javax.swing.*;

public class Logger {
    public static JTextArea logArea;

    public synchronized static void log(String stringToLog) {
        String COLOR = "\033[0;31m";
        String RESET = "\033[0m";
        System.out.println(COLOR + stringToLog + RESET);
        logArea.append(stringToLog);
        logArea.append("\n");
    }
}
