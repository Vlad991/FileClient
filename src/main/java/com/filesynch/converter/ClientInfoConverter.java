package com.filesynch.converter;

import com.filesynch.dto.ClientInfoDTO;
import com.filesynch.entity.ClientInfo;

import java.util.List;
import java.util.stream.Collectors;

public class ClientInfoConverter {
    public ClientInfoConverter() {
    }

    public ClientInfoDTO convertToDto(ClientInfo clientInfo) {
        ClientInfoDTO clientInfoDTO = new ClientInfoDTO();
        clientInfoDTO.setLogin(clientInfo.getLogin());
        clientInfoDTO.setName(clientInfo.getName());
        clientInfoDTO.setExternalIp(clientInfo.getExternalIp());
        clientInfoDTO.setLocalIp(clientInfo.getLocalIp());
        clientInfoDTO.setPcName(clientInfo.getPcName());
        clientInfoDTO.setPcModel(clientInfo.getPcModel());
        clientInfoDTO.setStatus(clientInfo.getStatus());
        clientInfoDTO.setFilePartSize(clientInfo.getFilePartSize());
        clientInfoDTO.setOutputFilesFolder(clientInfo.getOutputFilesFolder());
        clientInfoDTO.setInputFilesFolder(clientInfo.getInputFilesFolder());
        clientInfoDTO.setHandlersCount(clientInfo.getHandlersCount());
        clientInfoDTO.setHandlerTimeout(clientInfo.getHandlerTimeout());
        clientInfoDTO.setThreadsCount(clientInfo.getThreadsCount());
        clientInfoDTO.setSendFrequency(clientInfo.getSendFrequency());
        clientInfoDTO.setAliveRequestFrequency(clientInfo.getAliveRequestFrequency());
        return clientInfoDTO;
    }

    public ClientInfo convertToEntity(ClientInfoDTO clientInfoDTO) {
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setLogin(clientInfoDTO.getLogin());
        clientInfo.setName(clientInfoDTO.getName());
        clientInfo.setExternalIp(clientInfoDTO.getExternalIp());
        clientInfo.setLocalIp(clientInfoDTO.getLocalIp());
        clientInfo.setPcName(clientInfoDTO.getPcName());
        clientInfo.setPcModel(clientInfoDTO.getPcModel());
        clientInfo.setStatus(clientInfoDTO.getStatus());
        clientInfo.setFilePartSize(clientInfoDTO.getFilePartSize());
        clientInfo.setOutputFilesFolder(clientInfoDTO.getOutputFilesFolder());
        clientInfo.setInputFilesFolder(clientInfoDTO.getInputFilesFolder());
        clientInfo.setHandlersCount(clientInfoDTO.getHandlersCount());
        clientInfo.setHandlerTimeout(clientInfoDTO.getHandlerTimeout());
        clientInfo.setThreadsCount(clientInfoDTO.getThreadsCount());
        clientInfo.setSendFrequency(clientInfoDTO.getSendFrequency());
        clientInfo.setAliveRequestFrequency(clientInfoDTO.getAliveRequestFrequency());
        return clientInfo;
    }

    public List<ClientInfoDTO> convertToListDto(List<ClientInfo> clientInfoList) {
        return clientInfoList.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}
