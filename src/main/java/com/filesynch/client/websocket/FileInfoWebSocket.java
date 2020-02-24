package com.filesynch.client.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesynch.Main;
import com.filesynch.client.Client;
import com.filesynch.client.Logger;
import com.filesynch.dto.FileInfoDTO;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

public class FileInfoWebSocket extends TextWebSocketHandler {
    private ObjectMapper mapper = new ObjectMapper();
    private Client client;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        client = Main.client;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (client == null) {
            throw new Exception("Client is null");
        }
        try {
            String jsonString = message.getPayload();
            FileInfoDTO fileInfoDTO = mapper.readValue(jsonString, FileInfoDTO.class);
            client.sendFileInfoToClient(fileInfoDTO);
        } catch (IOException e) {
            e.printStackTrace();
            Logger.log(e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }
}
