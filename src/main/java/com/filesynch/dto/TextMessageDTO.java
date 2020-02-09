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
public class TextMessageDTO implements Serializable {
    static final long serialVersionUID = 40L;
    private String message;
    private ClientInfoDTO client;
}
