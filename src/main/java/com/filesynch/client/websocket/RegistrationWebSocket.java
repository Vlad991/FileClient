package com.filesynch.client.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesynch.Main;
import com.filesynch.client.Client;
import com.filesynch.client.Logger;
import com.filesynch.dto.ClientInfoDTO;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class RegistrationWebSocket extends TextWebSocketHandler {
    private ObjectMapper mapper = new ObjectMapper();
    private Client client = Main.client;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        client = Main.client;
        Logger.log("/register: connected");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (client == null) {
            throw new Exception("Client is null");
        }
        String jsonString = message.getPayload();
        ClientInfoDTO clientInfoDTO = mapper.readValue(jsonString, ClientInfoDTO.class);
        client.isRegistered(clientInfoDTO);
        synchronized (client) {
            client.notify();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Logger.log("/register: disconnected");
        super.afterConnectionClosed(session, status);
        client.doReconnection(5);
    }
}
