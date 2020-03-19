package com.filesynch.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientGuiInt extends Remote {
    public void log(String stringToLog) throws RemoteException;
}
