package com.filesynch.converter;

import com.filesynch.dto.TextMessageDTO;
import com.filesynch.entity.TextMessage;

public class TextMessageConverter {
    private ClientInfoConverter clientInfoConverter;

    public TextMessageConverter(ClientInfoConverter clientInfoConverter) {
        this.clientInfoConverter = clientInfoConverter;
    }

    public TextMessageDTO convertToDto(TextMessage textMessage) {
        TextMessageDTO textMessageDTO = new TextMessageDTO();
        textMessageDTO.setClient(clientInfoConverter.convertToDto(textMessage.getClient()));
        textMessageDTO.setMessage(textMessage.getMessage());
        return textMessageDTO;
    }

    public TextMessage convertToEntity(TextMessageDTO textMessageDTO) {
        TextMessage textMessage = new TextMessage();
        textMessage.setClient(clientInfoConverter.convertToEntity(textMessageDTO.getClient()));
        textMessage.setMessage(textMessageDTO.getMessage());
        return textMessage;
    }
}
