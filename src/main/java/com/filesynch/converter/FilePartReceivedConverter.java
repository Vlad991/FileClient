package com.filesynch.converter;

import com.filesynch.dto.FilePartDTO;
import com.filesynch.entity.FilePartReceived;

public class FilePartReceivedConverter {
    private ClientInfoConverter clientInfoConverter;
    private FileInfoReceivedConverter fileInfoConverter;

    public FilePartReceivedConverter(ClientInfoConverter clientInfoConverter,
                                     FileInfoReceivedConverter fileInfoConverter) {
        this.clientInfoConverter = clientInfoConverter;
        this.fileInfoConverter = fileInfoConverter;
    }

    public FilePartDTO convertToDto(FilePartReceived filePartReceived) {
        FilePartDTO filePartDTO = new FilePartDTO();
        filePartDTO.setHashKey(filePartReceived.getHashKey());
        filePartDTO.setClient(clientInfoConverter.convertToDto(filePartReceived.getClient()));
        filePartDTO.setFileInfoDTO(fileInfoConverter.convertToDto(filePartReceived.getFileInfo()));
        filePartDTO.setOrder(filePartReceived.getOrder());
        filePartDTO.setStatus(filePartReceived.getStatus());
        return filePartDTO;
    }

    public FilePartReceived convertToEntity(FilePartDTO filePartDTO) {
        FilePartReceived filePartReceived = new FilePartReceived();
        filePartReceived.setHashKey(filePartDTO.getHashKey());
        filePartReceived.setClient(clientInfoConverter.convertToEntity(filePartDTO.getClient()));
        filePartReceived.setFileInfo(fileInfoConverter.convertToEntity(filePartDTO.getFileInfoDTO()));
        filePartReceived.setOrder(filePartDTO.getOrder());
        filePartReceived.setStatus(filePartDTO.getStatus());
        return filePartReceived;
    }
}
