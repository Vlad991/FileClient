package com.filesynch.rmi;

import com.filesynch.dto.ClientInfoDTO;
import com.filesynch.dto.ClientStatus;
import com.filesynch.dto.SettingsDTO;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientRmiInt extends Remote {
    public ClientStatus getClientStatus() throws RemoteException;

    public ClientInfoDTO connectGuiToClient(ClientGuiInt clientGuiInt) throws RemoteException;

    public void connectToServer(String ip, String port, ClientInfoDTO clientInfoDTO) throws RemoteException;

    public void sendMessage(String message) throws RemoteException;

    public void sendFile(String file) throws RemoteException;

    public void sendAllFiles() throws RemoteException;

    public void setSettings(SettingsDTO settings) throws RemoteException;

    public SettingsDTO getSettings() throws RemoteException;

    public void logout() throws RemoteException;
}
