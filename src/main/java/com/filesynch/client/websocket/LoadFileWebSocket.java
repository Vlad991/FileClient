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

public class LoadFileWebSocket extends TextWebSocketHandler {
    private ObjectMapper mapper = new ObjectMapper();
    private Client client;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        client = Main.client;
        Logger.log("/load-file: connected");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (client == null) {
            throw new Exception("Client is null");
        }
        String jsonString = message.getPayload();
        FileInfoDTO fileInfoDTO = mapper.readValue(jsonString, FileInfoDTO.class);
        client.loadFile(fileInfoDTO);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Logger.log("/load-file: disconnected(" + status + ")");
        super.afterConnectionClosed(session, status);
        client.doReconnection(5);
    }
}
