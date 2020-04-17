package com.filesynch.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "client_settings")
public class Settings {
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    private Long id;
    @Column(name = "output_directory")
    private String outputFilesDirectory;
    @Column(name = "input_directory")
    private String inputFilesDirectory;
    @Column(name = "file_part_size")
    private int filePartSize = 5;
    @Column(name = "handlers_count")
    private int handlersCount = 7;
    @Column(name = "handler_timeout")
    private int handlerTimeout = 20;
    @Column(name = "threads_count")
    private int threadsCount = 3;
}
