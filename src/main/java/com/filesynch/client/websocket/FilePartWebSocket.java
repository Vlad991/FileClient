package com.filesynch.client.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesynch.Main;
import com.filesynch.client.Client;
import com.filesynch.client.Logger;
import com.filesynch.dto.FilePartDTO;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

//@Component
public class FilePartWebSocket extends TextWebSocketHandler {
    private ObjectMapper mapper = new ObjectMapper();
    private Client client;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        client = Main.client;
        if (client == null) {
            return;
        }
        try {
            String login = (String) session.getAttributes().get(Client.CLIENT_LOGIN);
            String jsonString = message.getPayload();
            FilePartDTO filePartDTO = mapper.readValue(jsonString, FilePartDTO.class);
            if (filePartDTO.getOrder() == 1 && filePartDTO.getHashKey() == null) {
                WebSocketSession clientFirstFilePartSession = client.getFirstFilePartSession();
                synchronized (clientFirstFilePartSession) {
                    clientFirstFilePartSession.getAttributes().put("first_file_part", filePartDTO);
                    clientFirstFilePartSession.notify();
                }
                return;
            }
            boolean result = client.sendFilePartToClient(filePartDTO);
            TextMessage textMessage = new TextMessage(mapper.writeValueAsString(result));
            client.getTextMessageSession().sendMessage(textMessage);
        } catch (IOException e) {
            Logger.log(e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }
}
