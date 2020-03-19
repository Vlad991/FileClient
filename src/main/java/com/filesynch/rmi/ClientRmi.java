package com.filesynch.rmi;

import com.filesynch.Main;
import com.filesynch.client.Client;
import com.filesynch.client.Logger;
import com.filesynch.client.RestClient;
import com.filesynch.client.websocket.*;
import com.filesynch.converter.ClientInfoConverter;
import com.filesynch.dto.ClientInfoDTO;
import com.filesynch.entity.ClientInfo;
import com.filesynch.repository.ClientInfoRepository;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.net.URI;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

@Getter
@Setter
public class ClientRmi extends UnicastRemoteObject implements ClientRmiInt {
    private ClientGuiInt clientGui;
    private Client client;
    private ConfigurableApplicationContext ctx;
    private ClientInfoRepository clientInfoRepository;
    private ClientInfoConverter clientInfoConverter;
    public Environment environment;
    public String host;
    public int port;

    public ClientRmi() throws RemoteException {
        super();
    }

    @Override
    public ClientInfoDTO connectGuiToClient(ClientGuiInt clientGuiInt) throws RemoteException {
        this.clientGui = clientGuiInt;
        Logger.clientGuiInt = clientGuiInt;
        Main.clientGui = clientGui;
        return clientInfoConverter.convertToDto(clientInfoRepository.findFirstByIdGreaterThan(0L));
    }

    @Override
    public void connectToServer(String ip, String port, ClientInfoDTO clientInfoDTO) throws RemoteException {
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
            TextMessageWebSocket textMessageWebSocket = new TextMessageWebSocket();
            FileInfoWebSocket fileInfoWebSocket = new FileInfoWebSocket();
            FilePartWebSocket filePartWebSocket = new FilePartWebSocket();
            FileStatusWebSocket fileStatusWebSocket = new FileStatusWebSocket();
            FilePartStatusWebSocket filePartStatusWebSocket = new FilePartStatusWebSocket();
            LoadFileWebSocket loadFileWebSocket = new LoadFileWebSocket();

            client = ctx.getBean(Client.class);
            client.setRestClient(restClient);
            if (!client.registerToServer(clientInfo)) {
                Logger.log("Can't register");
                throw new Exception("Can't register");
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
            client.sendTextMessageToServer("Connected client: " + client.getClientInfo().getLogin());
        } catch (Throwable t) {
            // todo: close all sessions!!!
            t.printStackTrace();
        }
    }

    @Override
    public void sendMessage(String message) throws RemoteException {
        client.sendTextMessageToServer(message);
    }

    @Override
    public void sendFile(String file) throws RemoteException {
        while (!client.sendFileToServer(file)) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void sendAllFiles() throws RemoteException {
        client.sendAllFilesToServer();
    }
}
