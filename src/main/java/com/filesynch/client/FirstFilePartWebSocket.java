package com.filesynch.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesynch.Main;
import com.filesynch.dto.FileInfoDTO;
import com.filesynch.dto.FilePartDTO;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

//@Component
public class FirstFilePartWebSocket extends TextWebSocketHandler {
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
            FileInfoDTO fileInfoDTO = mapper.readValue(jsonString, FileInfoDTO.class);
            FilePartDTO firstNotSentFilePartDTO = client.getFirstNotSentFilePartFromClient(fileInfoDTO);
            TextMessage textMessage = new TextMessage(mapper.writeValueAsString(firstNotSentFilePartDTO));
            client.getFilePartSession().sendMessage(textMessage);
        } catch (IOException e) {
            Logger.log(e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
    }
}
