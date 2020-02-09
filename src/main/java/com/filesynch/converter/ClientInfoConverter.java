package com.filesynch.converter;

import com.filesynch.dto.ClientInfoDTO;
import com.filesynch.entity.ClientInfo;

public class ClientInfoConverter {
    public ClientInfoDTO convertToDto(ClientInfo clientInfo) {
        ClientInfoDTO clientInfoDTO = new ClientInfoDTO();
        clientInfoDTO.setLogin(clientInfo.getLogin());
        clientInfoDTO.setIpAddress(clientInfo.getIpAddress());
        clientInfoDTO.setPcName(clientInfo.getPcName());
        clientInfoDTO.setPcModel(clientInfo.getPcModel());
        clientInfoDTO.setStatus(clientInfo.getStatus());
        return clientInfoDTO;
    }

    public ClientInfo convertToEntity(ClientInfoDTO clientInfoDTO) {
        ClientInfo clientInfo = new ClientInfo();
        clientInfo.setLogin(clientInfoDTO.getLogin());
        clientInfo.setStatus(clientInfoDTO.getStatus());
        return clientInfo;
    }
}
