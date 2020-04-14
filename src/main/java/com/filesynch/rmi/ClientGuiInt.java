package com.filesynch.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientGuiInt extends Remote {
    public void log(String stringToLog) throws RemoteException;

    public void logYellow(String stringToLog) throws RemoteException;

    public void logBlue(String stringToLog) throws RemoteException;

    public void logGreen(String stringToLog) throws RemoteException;

    public void logRed(String stringToLog) throws RemoteException;
}
