package com.filesynch.entity;

import com.filesynch.dto.FilePartStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "file_parts_sent")
public class FilePartSent {
    @Id
    @GeneratedValue(generator="increment")
    @GenericGenerator(name="increment", strategy = "increment")
    private Long id;
    @Column(name = "hash_key")
    private String hashKey;
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
