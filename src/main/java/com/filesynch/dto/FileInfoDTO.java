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
public class FileInfoDTO implements Serializable {
    static final long serialVersionUID = 20L;
    private String hash;
    private String name;
    private long size;
    private int partsQuantity;
    private FileStatus fileStatus;
    private ClientInfoDTO client;
}
