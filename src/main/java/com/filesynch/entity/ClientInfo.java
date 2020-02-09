package com.filesynch.entity;

import com.filesynch.dto.ClientStatus;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

@Entity
@Table(name = "client_data")
public class ClientInfo { // only one row int table
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    private Long id;
    @Column(name = "login")
    private String login; // name to be logged in to server (login)
    @Column(name = "ip_address")
    private String ipAddress;
    @Column(name = "pc_name")
    private String pcName;
    @Column(name = "pc_model")
    private String pcModel;
    @Column(name = "status")
    private ClientStatus status;


    public ClientInfo() {
        try {
            URL url_name = new URL("http://bot.whatismyipaddress.com");
            BufferedReader sc = new BufferedReader(new InputStreamReader(url_name.openStream()));
            ipAddress = sc.readLine().trim();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            pcName = getInetAddress().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        String nameOS = System.getProperty("os.name");
        String osType = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        pcModel = "OS Name: " + nameOS + ", OS Type: " + osType + ", OS Version: " + osVersion;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPcName() {
        return pcName;
    }

    public String getPcModel() {
        return pcModel;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public void setStatus(ClientStatus status) {
        this.status = status;
    }

    private InetAddress getInetAddress() throws UnknownHostException {
        return InetAddress.getLocalHost();
    }

    @Override
    public String toString() {
        return "ClientInfo{" +
                "login='" + login + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", pcName='" + pcName + '\'' +
                ", pcModel='" + pcModel + '\'' +
                ", status=" + status +
                '}';
    }
}
