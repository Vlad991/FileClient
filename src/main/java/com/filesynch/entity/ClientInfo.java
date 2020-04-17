package com.filesynch.entity;

import com.filesynch.dto.ClientStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

@Getter
@Setter
@ToString
@Entity
@Table(name = "client_data")
public class ClientInfo { // only one row int table
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    private Long id;
    @Column(name = "login", unique = true)
    private String login;
    @Column(name = "name")
    private String name;
    @Column(name = "external_ip")
    private String externalIp;
    @Column(name = "local_ip")
    private String localIp;
    @Column(name = "pc_name")
    private String pcName;
    @Column(name = "pc_model")
    private String pcModel;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ClientStatus status;
    @Column(name = "output_folder")
    private String outputFilesFolder;
    @Column(name = "input_folder")
    private String inputFilesFolder;
    @Column(name = "file_part_size")
    private int filePartSize;
    @Column(name = "handlers_count")
    private int handlersCount;
    @Column(name = "hanlder_timeout")
    private int handlerTimeout;
    @Column(name = "threads_count")
    private int threadsCount;
    @Column(name = "send_frequency")
    private int sendFrequency; // per Hour
    @Column(name = "work_request_frequency")
    private int aliveRequestFrequency; // per Hour


    public ClientInfo() {
        try {
            URL whatIsMyIP = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(whatIsMyIP.openStream()));
            externalIp = in.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            pcName = getInetAddress().getHostName();
            localIp = getInetAddress().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String nameOS = System.getProperty("os.name");
        String osType = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        pcModel = "OS Name: " + nameOS + ", OS Type: " + osType + ", OS Version: " + osVersion;
        status = ClientStatus.NEW;
    }

    private InetAddress getInetAddress() throws UnknownHostException {
        return InetAddress.getLocalHost();
    }
}
