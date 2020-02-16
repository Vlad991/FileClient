package com.filesynch.client.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesynch.Main;
import com.filesynch.client.Client;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

//@Component
public class TextMessageWebSocket extends TextWebSocketHandler {
    private ObjectMapper mapper = new ObjectMapper();
    private Client client = Main.client;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        client = Main.client;
        if (client == null) {
            return;
        }
        String messageString = message.getPayload();
        synchronized (client.getTextMessageSession()) {
            client.sendTextMessageToClient(messageString);
            client.getTextMessageSession().notify();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }
}
