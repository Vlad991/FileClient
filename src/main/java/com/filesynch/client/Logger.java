package com.filesynch.client;

import com.filesynch.rmi.ClientGuiInt;

import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Logger {
    public static ClientGuiInt clientGuiInt;
    private static ExecutorService pool = Executors.newFixedThreadPool(1);

    public synchronized static void log(String stringToLog) {
        pool.execute(new Thread(() -> {
            String COLOR = "\033[0;31m";
            String RESET = "\033[0m";
            System.out.println(COLOR + stringToLog + RESET);
            try {
                clientGuiInt.log(stringToLog);
            } catch (RemoteException e) {
                //e.printStackTrace();
            }
        }));
    }

    public static void logYellow(String stringToLog) {
        pool.execute(new Thread(() -> {
            String COLOR = "\033[0;31m";
            String RESET = "\033[0m";
            System.out.println(COLOR + stringToLog + RESET);
            try {
                clientGuiInt.logYellow(stringToLog);
            } catch (RemoteException e) {
                //e.printStackTrace();
            }
        }));
    }

    public static void logBlue(String stringToLog) {
        pool.execute(new Thread(() -> {
            String COLOR = "\033[0;31m";
            String RESET = "\033[0m";
            System.out.println(COLOR + stringToLog + RESET);
            try {
                clientGuiInt.logBlue(stringToLog);
            } catch (RemoteException e) {
                //e.printStackTrace();
            }
        }));
    }

    public static void logGreen(String stringToLog) {
        pool.execute(new Thread(() -> {
            String COLOR = "\033[0;31m";
            String RESET = "\033[0m";
            System.out.println(COLOR + stringToLog + RESET);
            try {
                clientGuiInt.logGreen(stringToLog);
            } catch (RemoteException e) {
                //e.printStackTrace();
            }
        }));
    }

    public static void logRed(String stringToLog) {
        pool.execute(new Thread(() -> {
            String COLOR = "\033[0;31m";
            String RESET = "\033[0m";
            System.out.println(COLOR + stringToLog + RESET);
            try {
                clientGuiInt.logRed(stringToLog);
            } catch (RemoteException e) {
                //e.printStackTrace();
            }
        }));
    }
}
