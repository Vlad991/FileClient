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
    private String login; // name to be logged in to server (login)
    private String ipAddress;
    private String pcName;
    private String pcModel;
    private ClientStatus status;
}
