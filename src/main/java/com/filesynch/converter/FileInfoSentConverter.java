package com.filesynch.converter;

import com.filesynch.dto.FileInfoDTO;
import com.filesynch.entity.FileInfoSent;

public class FileInfoSentConverter {
    private ClientInfoConverter clientInfoConverter;

    public FileInfoSentConverter(ClientInfoConverter clientInfoConverter) {
        this.clientInfoConverter = clientInfoConverter;
    }

    public FileInfoDTO convertToDto(FileInfoSent fileInfoSent) {
        FileInfoDTO fileInfoDTO = new FileInfoDTO();
        fileInfoDTO.setHash(fileInfoSent.getHash());
        fileInfoDTO.setName(fileInfoSent.getName());
        fileInfoDTO.setSize(fileInfoSent.getSize());
        fileInfoDTO.setClient(clientInfoConverter.convertToDto(fileInfoSent.getClient()));
        return fileInfoDTO;
    }

    public FileInfoSent convertToEntity(FileInfoDTO fileInfoDTO) {
        FileInfoSent fileInfoSent = new FileInfoSent();
        fileInfoSent.setHash(fileInfoDTO.getHash());
        fileInfoSent.setName(fileInfoDTO.getName());
        fileInfoSent.setSize(fileInfoDTO.getSize());
        fileInfoSent.setClient(clientInfoConverter.convertToEntity(fileInfoDTO.getClient()));
        return fileInfoSent;
    }
}
