package com.filesynch;

import com.filesynch.client.*;
import com.filesynch.client.websocket.*;
import com.filesynch.dto.FilePartDTO;
import com.filesynch.gui.ConnectToServer;
import com.filesynch.gui.FileSynchronizationClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import javax.swing.*;
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
    public static JFrame clientFrame;
    public static FileSynchronizationClient fileSynchronizationClient;
    public static Client client;
    private static ConfigurableApplicationContext ctx;

    public static void main(String[] args) {
        ctx = SpringApplication.run(Main.class, args);
        System.setProperty("java.awt.headless", "false");
        connectToServerFrame = new JFrame("Connect To Server");
        connectToServerFrame.setContentPane(new ConnectToServer().getJPanelMain());
        connectToServerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        connectToServerFrame.pack();
        connectToServerFrame.setLocationRelativeTo(null);
        connectToServerFrame.setVisible(true);
    }

    public static void connectToServer(String ip, String port) {
        fileSynchronizationClient = new FileSynchronizationClient();

        String uri = "ws://" + ip + ":" + port;
        StandardWebSocketClient standardWebSocketClient = new StandardWebSocketClient();
        WebSocketSession loginSession = null;
        WebSocketSession textMessageSession = null;
        WebSocketSession fileInfoSession = null;
        WebSocketSession filePartSession = null;
        WebSocketSession firstFilePartSession = null;

        try {
            LoginWebSocket loginWebSocket = new LoginWebSocket();
            TextMessageWebSocket textMessageWebSocket = new TextMessageWebSocket();
            FileInfoWebSocket fileInfoWebSocket = new FileInfoWebSocket();
            FilePartWebSocket filePartWebSocket = new FilePartWebSocket();
            FirstFilePartWebSocket firstFilePartWebSocket = new FirstFilePartWebSocket();

            ListenableFuture<WebSocketSession> loginFut =
                    standardWebSocketClient.doHandshake(loginWebSocket, uri + "/login");
            loginSession = loginFut.get();

            Logger.log = fileSynchronizationClient.getJTextAreaLog();
            client = ctx.getBean(Client.class);
            client.setLoginSession(loginSession);
            client.loginToServer();

            WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
            webSocketHttpHeaders.add(Client.CLIENT_LOGIN, client.getLogin());
            ListenableFuture<WebSocketSession> textMessageFut =
                    standardWebSocketClient
                            .doHandshake(textMessageWebSocket, webSocketHttpHeaders, new URI(uri + "/text"));
            ListenableFuture<WebSocketSession> fileInfoFut =
                    standardWebSocketClient
                            .doHandshake(fileInfoWebSocket, webSocketHttpHeaders, new URI(uri + "/file-info"));
            ListenableFuture<WebSocketSession> filePartFut =
                    standardWebSocketClient
                            .doHandshake(filePartWebSocket, webSocketHttpHeaders, new URI(uri + "/file-part"));
            ListenableFuture<WebSocketSession> firstFilePartFut =
                    standardWebSocketClient
                            .doHandshake(firstFilePartWebSocket, webSocketHttpHeaders, new URI(uri + "/first-file-part"));
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
            clientFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            clientFrame.pack();
            clientFrame.setLocationRelativeTo(null);
            clientFrame.setVisible(true);
            client.setFileProgressBar(fileSynchronizationClient.getJProgressBarFile());

            FilePartDTO filePartDTO = new FilePartDTO();
            filePartDTO.setOrder(0);
            client.sendFilePartToServer(filePartDTO);
            client.sendFileToServer("info.txt");
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
        try (Stream<Path> walk = Files.walk(Paths.get(client.FILE_OUTPUT_DIRECTORY.substring(0,client.FILE_OUTPUT_DIRECTORY.length() - 1)))) {
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
