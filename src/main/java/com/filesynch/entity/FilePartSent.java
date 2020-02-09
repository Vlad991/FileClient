package com.filesynch.entity;

import com.filesynch.dto.FilePartStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "file_parts_sent")
public class FilePartSent {
    @Id
    private Long hashKey;
    @ManyToOne
    @JoinColumn(name = "file_info_id")
    private FileInfoSent fileInfo;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private FilePartStatus status;
    @Column(name = "index_number")
    private int order;
    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientInfo client;
}
