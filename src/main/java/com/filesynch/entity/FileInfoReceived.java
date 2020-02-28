package com.filesynch.entity;

import com.filesynch.dto.FileStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "file_info_reveived")
public class FileInfoReceived {
    @Id
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    private Long id;
    @Column(name = "md5_hash")
    private String hash;
    @Column(name = "name", unique = true)
    private String name;
    @Column(name = "size")
    private long size; // in bytes
    @Column(name = "parts_quantity")
    private int partsQuantity;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private FileStatus fileStatus;
    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientInfo client;
}
