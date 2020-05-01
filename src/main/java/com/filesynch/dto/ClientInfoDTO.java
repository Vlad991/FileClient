package com.filesynch.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class ClientInfoDTO implements Serializable {
    static final long serialVersionUID = 10L;
    private Long id;
    private String login;
    private String name;
    private String externalIp;
    private String localIp;
    private String pcName;
    private String pcModel;
    private ClientStatus status;
    private String outputFilesFolder;
    private String inputFilesFolder;
    private int filePartSize;
    private int handlersCount;
    private int handlerTimeout;
    private int threadsCount;
    private int sendFrequency;
    private int aliveRequestFrequency;
}
