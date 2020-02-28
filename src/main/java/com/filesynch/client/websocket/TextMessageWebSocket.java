package com.filesynch.client.websocket;

import com.filesynch.Main;
import com.filesynch.client.Client;
import com.filesynch.client.Logger;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class TextMessageWebSocket extends TextWebSocketHandler {
    private Client client = Main.client;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        client = Main.client;
        Logger.log("/text: connected");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (client == null) {
            throw new Exception("Client is null");
        }
        String messageString = message.getPayload();
        client.sendTextMessageToClient(messageString);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Logger.log("/text: disconnected");
        super.afterConnectionClosed(session, status);
        client.doReconnection(5);
    }
}
