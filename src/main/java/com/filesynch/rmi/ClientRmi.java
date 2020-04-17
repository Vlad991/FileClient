package com.filesynch.rmi;

import com.filesynch.Main;
import com.filesynch.client.Client;
import com.filesynch.client.Logger;
import com.filesynch.client.RestClient;
import com.filesynch.client.websocket.*;
import com.filesynch.converter.ClientInfoConverter;
import com.filesynch.converter.SettingsConverter;
import com.filesynch.dto.ClientInfoDTO;
import com.filesynch.dto.ClientStatus;
import com.filesynch.dto.SettingsDTO;
import com.filesynch.entity.ClientInfo;
import com.filesynch.entity.Settings;
import com.filesynch.repository.ClientInfoRepository;
import com.filesynch.repository.SettingsRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Optional;

@Getter
@Setter
public class ClientRmi extends UnicastRemoteObject implements ClientRmiInt {
    private ClientGuiInt clientGui;
    private Client client;
    private ConfigurableApplicationContext ctx;
    private ClientInfoRepository clientInfoRepository;
    private SettingsRepository settingsRepository;
    private ClientInfoConverter clientInfoConverter;
    private SettingsConverter settingsConverter;
    public Environment environment;
    public String host;
    public int port;

    public ClientRmi() throws RemoteException {
        super();
    }

    @Override
    public ClientStatus getClientStatus() {
        if (client != null) {
            return client.getClientInfoDTO().getStatus();
        } else {
            return ClientStatus.NEW;
        }
    }

    @Override
    public ClientInfoDTO connectGuiToClient(ClientGuiInt clientGuiInt) {
        this.clientGui = clientGuiInt;
        Logger.clientGuiInt = clientGuiInt;
        Main.clientGui = clientGui;
        if (clientInfoRepository.findFirstByIdGreaterThan(0L) != null) {
            return clientInfoConverter.convertToDto(clientInfoRepository.findFirstByIdGreaterThan(0L));
        } else {
            ClientInfo clientInfo = new ClientInfo();
            return clientInfoConverter.convertToDto(clientInfoRepository.save(clientInfo));
        }
    }

    @Override
    public void connectToServer(String ip, String port, ClientInfoDTO clientInfoDTO) {
        Main.ip = ip;
        Main.port = port;
        ClientInfo clientInfoFromDB = clientInfoRepository.findFirstByIdGreaterThan(0L);
        ClientInfo clientInfo = clientInfoConverter.convertToEntity(clientInfoDTO);
        if (clientInfoFromDB != null) {
            clientInfo.setId(clientInfoFromDB.getId());
            clientInfo.setLogin(clientInfoFromDB.getLogin());
        }
        clientInfoRepository.save(clientInfo);

        String wsURI = "ws://" + ip + ":" + port;
        StandardWebSocketClient standardWebSocketClient = new StandardWebSocketClient();
        WebSocketSession textMessageSession = null;
        WebSocketSession fileInfoSession = null;
        WebSocketSession filePartSession = null;
        WebSocketSession filePartStatusSession = null;
        WebSocketSession fileStatusSession = null;
        WebSocketSession loadFileSession = null;

        try {
            RestClient restClient = new RestClient("http://" + ip + ":" + port);
            RegistrationWebSocket registrationWebSocket = new RegistrationWebSocket();
            TextMessageWebSocket textMessageWebSocket = new TextMessageWebSocket();
            FileInfoWebSocket fileInfoWebSocket = new FileInfoWebSocket();
            FilePartWebSocket filePartWebSocket = new FilePartWebSocket();
            FileStatusWebSocket fileStatusWebSocket = new FileStatusWebSocket();
            FilePartStatusWebSocket filePartStatusWebSocket = new FilePartStatusWebSocket();
            LoadFileWebSocket loadFileWebSocket = new LoadFileWebSocket();

            client = ctx.getBean(Client.class);
            client.setRestClient(restClient);
            if (clientInfo.getLogin() == null) {
                client.registerToServer(clientInfo, registrationWebSocket, wsURI);
                synchronized (client) {
                    client.wait();
                }
            }
            if (!client.loginToServer()) {
                Logger.log("Can't login");
                throw new Exception("Can't login");
            }

            // After Login
            WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
            webSocketHttpHeaders.add(Client.CLIENT_LOGIN, client.getLogin());
            ListenableFuture<WebSocketSession> textMessageFut =
                    standardWebSocketClient
                            .doHandshake(textMessageWebSocket, webSocketHttpHeaders, new URI(wsURI + "/text"));
            ListenableFuture<WebSocketSession> fileInfoFut =
                    standardWebSocketClient
                            .doHandshake(fileInfoWebSocket, webSocketHttpHeaders, new URI(wsURI + "/file-info"));
            ListenableFuture<WebSocketSession> filePartFut =
                    standardWebSocketClient
                            .doHandshake(filePartWebSocket, webSocketHttpHeaders, new URI(wsURI + "/file-part"));
            ListenableFuture<WebSocketSession> filePartStatusFut =
                    standardWebSocketClient
                            .doHandshake(filePartStatusWebSocket, webSocketHttpHeaders, new URI(wsURI + "/file-part-status"));
            ListenableFuture<WebSocketSession> fileStatusFut =
                    standardWebSocketClient
                            .doHandshake(fileStatusWebSocket, webSocketHttpHeaders, new URI(wsURI + "/file-status"));
            ListenableFuture<WebSocketSession> loadFileFut =
                    standardWebSocketClient
                            .doHandshake(loadFileWebSocket, webSocketHttpHeaders, new URI(wsURI + "/load-file"));
            textMessageSession = textMessageFut.get();
            fileInfoSession = fileInfoFut.get();
            filePartSession = filePartFut.get();
            filePartStatusSession = filePartStatusFut.get();
            fileStatusSession = fileStatusFut.get();
            loadFileSession = loadFileFut.get();

            client.setTextMessageSession(textMessageSession);
            client.setFileInfoSession(fileInfoSession);
            client.setFilePartSession(filePartSession);
            client.setFilePartStatusSession(filePartStatusSession);
            client.setFileStatusSession(fileStatusSession);
            client.setLoadFileSession(loadFileSession);
            client.sendTextMessageToServer("Connected client: " + client.getClientInfoDTO().getLogin());
        } catch (Throwable t) {
            // todo: close all sessions!!!
            t.printStackTrace();
        }
    }

    @Override
    public void sendMessage(String message) {
        client.sendTextMessageToServer(message);
    }

    @Override
    public void sendFile(String file) {
        while (!client.sendFileToServer(file)) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendAllFiles() {
        client.sendAllFilesToServer();
    }

    @Override
    public void setSettings(SettingsDTO settingsDTO) throws RemoteException {
        Optional<Settings> settingsOpt = settingsRepository.findById(1L);
        Settings settings;
        if (settingsOpt.isEmpty()) {
            settings = settingsConverter.convertToEntity(settingsDTO);
        } else {
            settings = settingsConverter.convertToEntity(settingsDTO);
            settings.setId(settingsOpt.get().getId());
        }
        client.setSettings(settingsRepository.save(settings));
    }

    @Override
    public SettingsDTO getSettings() throws RemoteException {
        Optional<Settings> settingsOpt;
        settingsOpt = settingsRepository.findById(1L);
        return settingsOpt.isPresent() ? settingsConverter.convertToDto(settingsOpt.get()) : new SettingsDTO();
    }

    @Override
    public void logout() throws RemoteException {
        try {
            client.logoutFromServer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
