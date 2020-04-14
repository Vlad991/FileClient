package com.filesynch.client.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesynch.Main;
import com.filesynch.client.Client;
import com.filesynch.client.Logger;
import com.filesynch.dto.FilePartDTO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FilePartWebSocket extends TextWebSocketHandler {
    private ObjectMapper mapper = new ObjectMapper();
    private Client client;
    private ExecutorService handlerThreadPool;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        client = Main.client;
        Logger.log("/file-part: connected");
        handlerThreadPool =
                Executors.newFixedThreadPool(client.getAsyncService().getHandlerService().FILE_PART_HANDLER_COUNT);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (client == null) {
            throw new Exception("Client is null");
        }
        CompletableFuture.runAsync(() -> {
            try {
                Logger.log("sleep 5 sec");
                Thread.sleep(5000);
                String jsonString = message.getPayload();
                FilePartDTO filePartDTO = mapper.readValue(jsonString, FilePartDTO.class);
                client.sendFilePartToClient(filePartDTO);
            } catch (Exception e) {
                e.printStackTrace();
                Logger.log(e.getMessage());
            }
        }, handlerThreadPool);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Logger.log("/file-part: disconnected(" + status + ")");
        super.afterConnectionClosed(session, status);
        client.doReconnection(5);
    }
}
