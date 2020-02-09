package com.filesynch.converter;

import com.filesynch.dto.FileInfoDTO;
import com.filesynch.entity.FileInfoReceived;

public class FileInfoReceivedConverter {
    private ClientInfoConverter clientInfoConverter;

    public FileInfoReceivedConverter(ClientInfoConverter clientInfoConverter) {
        this.clientInfoConverter = clientInfoConverter;
    }

    public FileInfoDTO convertToDto(FileInfoReceived fileInfoReceived) {
        FileInfoDTO fileInfoDTO = new FileInfoDTO();
        fileInfoDTO.setHash(fileInfoReceived.getHash());
        fileInfoDTO.setName(fileInfoReceived.getName());
        fileInfoDTO.setSize(fileInfoReceived.getSize());
        fileInfoDTO.setClient(clientInfoConverter.convertToDto(fileInfoReceived.getClient()));
        return fileInfoDTO;
    }

    public FileInfoReceived convertToEntity(FileInfoDTO fileInfoDTO) {
        FileInfoReceived fileInfoReceived = new FileInfoReceived();
        fileInfoReceived.setHash(fileInfoDTO.getHash());
        fileInfoReceived.setName(fileInfoDTO.getName());
        fileInfoReceived.setSize(fileInfoDTO.getSize());
        fileInfoReceived.setClient(clientInfoConverter.convertToEntity(fileInfoDTO.getClient()));
        return fileInfoReceived;
    }
}
