package com.filesynch.client.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesynch.Main;
import com.filesynch.client.Client;
import com.filesynch.client.Logger;
import com.filesynch.dto.FileInfoDTO;
import com.filesynch.dto.FileStatus;
import com.filesynch.entity.FileInfoSent;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class FileStatusWebSocket extends TextWebSocketHandler {
    private ObjectMapper mapper = new ObjectMapper();
    private Client client;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        client = Main.client;
        session.setTextMessageSizeLimit(102400000);
        Logger.log("/file-status: connected");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (client == null) {
            throw new Exception("Client is null");
        }
        String jsonString = message.getPayload();
        FileInfoDTO fileInfoDTO = mapper.readValue(jsonString, FileInfoDTO.class);
        if (fileInfoDTO.getFileStatus() == FileStatus.TRANSFERRED) {
            Logger.logGreen("File with hash: " + fileInfoDTO.getHash() + " SENT");
        } else {
            Logger.logRed("File with hash: " + fileInfoDTO.getHash() + " NOT SENT");
        }
        FileInfoSent fileInfo = client.getFileInfoSentRepository().findByHashAndName(fileInfoDTO.getHash(),
                fileInfoDTO.getName());
        fileInfo.setFileStatus(fileInfoDTO.getFileStatus());
        client.getFileInfoSentRepository().save(fileInfo);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Logger.log("/file-status: disconnected(" + status + ")");
        super.afterConnectionClosed(session, status);
        client.doReconnection(5);
    }
}
