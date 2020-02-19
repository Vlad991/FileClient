package com.filesynch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesynch.client.*;
import com.filesynch.client.websocket.*;
import com.filesynch.converter.ClientInfoConverter;
import com.filesynch.dto.FilePartDTO;
import com.filesynch.entity.ClientInfo;
import com.filesynch.gui.ConnectToServer;
import com.filesynch.gui.FileSynchronizationClient;
import com.filesynch.repository.ClientInfoRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
public class Main {
    public static JFrame connectToServerFrame;
    public static ConnectToServer connectToServer;
    public static JFrame clientFrame;
    public static FileSynchronizationClient fileSynchronizationClient;
    public static Client client;
    private static ConfigurableApplicationContext ctx;
    public static ObjectMapper mapper = new ObjectMapper();
    private static ClientInfoRepository clientInfoRepository;

    public static void main(String[] args) {
        ctx = SpringApplication.run(Main.class, args);
        System.setProperty("java.awt.headless", "false");
        connectToServerFrame = new JFrame("Connect To Server");
        connectToServer = new ConnectToServer();
        connectToServerFrame.setContentPane(connectToServer.getJPanelMain());
        connectToServerFrame.pack();
        connectToServerFrame.setLocationRelativeTo(null);
        connectToServerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        connectToServerFrame.setVisible(true);

        clientInfoRepository = ctx.getBean(ClientInfoRepository.class);
        ClientInfo clientInfo = clientInfoRepository.findFirstByIdGreaterThan(0L);
        if (clientInfo != null) {
            connectToServer.getJTextFieldName().setText(clientInfo.getName());
            connectToServer.getJTextFieldFilesFolder().setText(clientInfo.getFilesFolder());
            connectToServer.getJTextFieldSendFrequency().setText(String.valueOf(clientInfo.getSendFrequency()));
            connectToServer.getJTextFieldAliveFrequency().setText(String.valueOf(clientInfo.getAliveRequestFrequency()));
        }
    }

    public static void connectToServer(String ip, String port, ClientInfo clientInfo) {
        ClientInfoConverter clientInfoConverter = new ClientInfoConverter();
        ClientInfo clientInfoFromDB = clientInfoRepository.findFirstByIdGreaterThan(0L);
        if (clientInfoFromDB != null) {
            clientInfo.setId(clientInfoFromDB.getId());
            clientInfo.setLogin(clientInfoFromDB.getLogin());
        }
        clientInfoRepository.save(clientInfo);

        fileSynchronizationClient = new FileSynchronizationClient();
        Logger.logArea = fileSynchronizationClient.getJTextAreaLog();

        String wsURI = "ws://" + ip + ":" + port;
        String httpURI = "http://" + ip + ":" + port;
        StandardWebSocketClient standardWebSocketClient = new StandardWebSocketClient();
        WebSocketSession loginSession = null;
        WebSocketSession textMessageSession = null;
        WebSocketSession fileInfoSession = null;
        WebSocketSession filePartSession = null;
        WebSocketSession firstFilePartSession = null;


        try {
            RestClient restClient = new RestClient("http://" + ip + ":" + port);
            TextMessageWebSocket textMessageWebSocket = new TextMessageWebSocket();
            FileInfoWebSocket fileInfoWebSocket = new FileInfoWebSocket();
            FilePartWebSocket filePartWebSocket = new FilePartWebSocket();
            FirstFilePartWebSocket firstFilePartWebSocket = new FirstFilePartWebSocket();

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
            ListenableFuture<WebSocketSession> firstFilePartFut =
                    standardWebSocketClient
                            .doHandshake(firstFilePartWebSocket, webSocketHttpHeaders, new URI(wsURI + "/first-file-part"));
            textMessageSession = textMessageFut.get();
            fileInfoSession = fileInfoFut.get();
            filePartSession = filePartFut.get();
            firstFilePartSession = firstFilePartFut.get();

            client.setTextMessageSession(textMessageSession);
            client.setFileInfoSession(fileInfoSession);
            client.setFilePartSession(filePartSession);
            client.setFirstFilePartSession(firstFilePartSession);
            client.sendTextMessageToServer("Connected client: " + client.getClientInfo().getLogin());

            connectToServerFrame.setVisible(false);
            clientFrame = new JFrame("File Synchronization Client");
            clientFrame.setContentPane(fileSynchronizationClient.getJPanelClient());
            clientFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            clientFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    if (JOptionPane.showConfirmDialog(clientFrame,
                            "Are you sure you want to close this window?", "Close Window?",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
                        try {
                            client.logoutFromServer();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        System.exit(0);
                    }
                }
            });
            clientFrame.pack();
            clientFrame.setLocationRelativeTo(null);
            clientFrame.setVisible(true);
            client.setFileProgressBar(fileSynchronizationClient.getJProgressBarFile());
        } catch (Throwable t) {
            // todo: close all sessions!!!
            t.printStackTrace();
        }
    }

    public static void sendMessage(String message) {
        client.sendTextMessageToServer(message);
    }

    public static void sendFile(String file) {
        client.sendFileToServer(file);
    }

    public static void sendAllFiles() {
        client.sendAllFilesToServer();
    }

    public static void sendFileFast(String file) {
        if (!(file.charAt(0) == '.')) {
            client.sendFileToServerFast(file);
        }
    }

    public static void sendAllFilesFast() {
        try (Stream<Path> walk = Files.walk(Paths.get(client.FILE_OUTPUT_DIRECTORY.substring(0, client.FILE_OUTPUT_DIRECTORY.length() - 1)))) {
            List<String> result = walk.filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
            for (String filePath : result) {
                sendFileFast(filePath.replace(client.FILE_OUTPUT_DIRECTORY, ""));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
