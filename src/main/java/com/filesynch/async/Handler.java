package com.filesynch.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesynch.Main;
import com.filesynch.client.Logger;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Getter
@Setter
public class Handler {
    private WebSocketSession socketSession;
    private Object objectToSend;
    private boolean objectIsSent;
    private boolean isBusy;
    private ExecutorService threadPool;
    private ObjectMapper mapper;

    public Handler() {
    }

    public Handler(ExecutorService threadPool) {
        this.threadPool = threadPool;
        this.mapper = new ObjectMapper();
        this.isBusy = false;
    }

    public boolean sendMessage(Object objectToSend, String filePartName, long hanlderTimeout) throws Exception {
        this.objectToSend = objectToSend;
        objectIsSent = false;
        while (!objectIsSent) {
            CompletableFuture<Void> future = CompletableFuture
                    .runAsync(() -> {
                        try {
                            Logger.logBlue("thread-" + Thread.currentThread().getName().substring(Thread.currentThread().getName().length() - 2) + " "
                                    + filePartName + " -> " + "sending");
                            synchronized (socketSession) {
                                socketSession.sendMessage(new TextMessage(mapper.writeValueAsString(objectToSend)));
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }, threadPool);
            future.get();
            synchronized (objectToSend) {
                Logger.log("handler-" + Thread.currentThread().getName().substring(Thread.currentThread().getName().length() - 2) + " " + " begin wait...");
                long startTime = System.currentTimeMillis();
                objectToSend.wait(hanlderTimeout * 1000);
                Logger.log("handler-" + Thread.currentThread().getName().substring(Thread.currentThread().getName().length() - 2) + " " + " stop waiting since " + ((System.currentTimeMillis() - startTime) / 1000.0));
            }
            if (objectIsSent) Logger.logGreen(filePartName + " ----> " + objectIsSent);
            else Logger.logRed(filePartName + " ----> " + objectIsSent);
        }
        return true;
    }
}
