package com.filesynch.converter;

import com.filesynch.dto.FilePartDTO;
import com.filesynch.entity.FilePartSent;

public class FilePartSentConverter {
    private ClientInfoConverter clientInfoConverter;
    private FileInfoSentConverter fileInfoConverter;

    public FilePartSentConverter(ClientInfoConverter clientInfoConverter,
                                 FileInfoSentConverter fileInfoConverter) {
        this.clientInfoConverter = clientInfoConverter;
        this.fileInfoConverter = fileInfoConverter;
    }

    public FilePartDTO convertToDto(FilePartSent filePartSent) {
        FilePartDTO filePartDTO = new FilePartDTO();
        filePartDTO.setHashKey(filePartSent.getHashKey());
        filePartDTO.setClient(clientInfoConverter.convertToDto(filePartSent.getClient()));
        filePartDTO.setFileInfoDTO(fileInfoConverter.convertToDto(filePartSent.getFileInfo()));
        filePartDTO.setOrder(filePartSent.getOrder());
        filePartDTO.setStatus(filePartSent.getStatus());
        return filePartDTO;
    }

    public FilePartSent convertToEntity(FilePartDTO filePartDTO) {
        FilePartSent filePartSent = new FilePartSent();
        filePartSent.setHashKey(filePartDTO.getHashKey());
        filePartSent.setClient(clientInfoConverter.convertToEntity(filePartDTO.getClient()));
        filePartSent.setFileInfo(fileInfoConverter.convertToEntity(filePartDTO.getFileInfoDTO()));
        filePartSent.setOrder(filePartDTO.getOrder());
        filePartSent.setStatus(filePartDTO.getStatus());
        return filePartSent;
    }
}
