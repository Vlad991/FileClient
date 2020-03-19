package com.filesynch;

import com.filesynch.client.Client;
import com.filesynch.converter.ClientInfoConverter;
import com.filesynch.repository.ClientInfoRepository;
import com.filesynch.rmi.ClientGuiInt;
import com.filesynch.rmi.ClientRmi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

@SpringBootApplication
public class Main {
    public static Client client;
    public static ClientRmi clientRmi;
    public static ClientGuiInt clientGui;
    private static ConfigurableApplicationContext ctx;
    private static ClientInfoRepository clientInfoRepository;
    public static String ip;
    public static String port;

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.createRegistry(8090);
            clientRmi = new ClientRmi();
            Naming.rebind("rmi://localhost:8090/gui", clientRmi);
        } catch (Exception e) {
            e.printStackTrace();
        }

        ctx = SpringApplication.run(Main.class, args);
        System.setProperty("java.awt.headless", "false");
        clientInfoRepository = ctx.getBean(ClientInfoRepository.class);
        clientRmi.setClientInfoRepository(clientInfoRepository);
        clientRmi.setClientInfoConverter(new ClientInfoConverter());
        clientRmi.setCtx(ctx);
    }
}
