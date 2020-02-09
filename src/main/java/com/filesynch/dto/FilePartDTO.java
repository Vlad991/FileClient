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
public class FilePartDTO implements Serializable {
    static final long serialVersionUID = 30L;
    private Long hashKey;
    private FileInfoDTO fileInfoDTO;
    private FilePartStatus status;
    private int order;
    private byte[] data;
    private int length;
    private ClientInfoDTO client;
}
