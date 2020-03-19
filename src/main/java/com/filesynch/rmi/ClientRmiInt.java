package com.filesynch.rmi;

import com.filesynch.dto.ClientInfoDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientRmiInt extends Remote {
    public ClientInfoDTO connectGuiToClient(ClientGuiInt clientGuiInt) throws RemoteException;

    public void connectToServer(String ip, String port, ClientInfoDTO clientInfoDTO) throws RemoteException;

    public void sendMessage(String message) throws RemoteException;

    public void sendFile(String file) throws RemoteException;

    public void sendAllFiles() throws RemoteException;
}
