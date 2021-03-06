package com.filesynch.client.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesynch.Main;
import com.filesynch.async.AsyncService;
import com.filesynch.client.Client;
import com.filesynch.client.Logger;
import com.filesynch.dto.FilePartDTO;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class FilePartStatusWebSocket extends TextWebSocketHandler {
    private ObjectMapper mapper = new ObjectMapper();
    private Client client;
    private AsyncService asyncService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        client = Main.client;
        Logger.log("/file-part-status: connected");
        asyncService = client.getAsyncService();
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (client == null) {
            throw new Exception("Client is null");
        }
        String jsonString = message.getPayload();
        FilePartDTO filePartDTO = mapper.readValue(jsonString, FilePartDTO.class);
        client.saveFilePartStatus(filePartDTO);
        boolean result = client.saveFilePartStatus(filePartDTO);
        if (result) {
            asyncService.notifyHandler(filePartDTO, true);
        } else {
            asyncService.notifyHandler(filePartDTO, false);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Logger.log("/file-part-status: disconnected(" + status + ")");
        super.afterConnectionClosed(session, status);
        client.doReconnection(5);
    }
}
