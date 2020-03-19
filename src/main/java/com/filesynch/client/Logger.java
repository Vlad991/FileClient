package com.filesynch.client;

import com.filesynch.rmi.ClientGuiInt;

import java.rmi.RemoteException;

public class Logger {
    public static ClientGuiInt clientGuiInt;

    public synchronized static void log(String stringToLog) {
        String COLOR = "\033[0;31m";
        String RESET = "\033[0m";
        System.out.println(COLOR + stringToLog + RESET);
        try {
            clientGuiInt.log(stringToLog);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
