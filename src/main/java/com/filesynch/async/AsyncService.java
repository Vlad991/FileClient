package com.filesynch.async;

import com.filesynch.client.Client;
import com.filesynch.client.Logger;
import com.filesynch.dto.FilePartDTO;
import lombok.Getter;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.*;

@Service
public class AsyncService {
    private ExecutorService handlerThreadPool;
    private ExecutorService threadPool;
    @Getter
    private HandlerService handlerService;
    private BlockingQueue<FilePartDTO> filePartDTOQueue;
    private BlockingQueue<WebSocketSession> sessionQueue;
    private Client client;
    private boolean isRunning;

    public AsyncService(HandlerService handlerService, Client client) {
        this.handlerThreadPool = Executors.newFixedThreadPool(client.getSettings().getHandlersCount());
        this.threadPool = Executors.newFixedThreadPool(client.getSettings().getThreadsCount());
        this.handlerService = handlerService;
        this.filePartDTOQueue = new LinkedBlockingQueue<>();
        this.sessionQueue = new LinkedBlockingQueue<>();
        this.client = client;
        client.setAsyncService(this);
    }

    public synchronized void addFilePartToHandling(FilePartDTO filePartDTO, WebSocketSession session) {
        filePartDTOQueue.add(filePartDTO);
        sessionQueue.add(session);
        if (!isRunning && filePartDTO.getOrder() == filePartDTO.getFileInfoDTO().getPartsQuantity()) {
            Logger.log("FP:" + filePartDTOQueue.size() + " S:" + sessionQueue.size());
            isRunning = true;
            startHandlingFileParts();
        }
    }

    public void startHandlingFileParts() {
        Logger.log("Async service STARTED handling FileParts");
        FilePartDTO filePartDTO = filePartDTOQueue.poll();
        WebSocketSession session = sessionQueue.poll();
        while (filePartDTO != null) {
            Handler handler = null;
            try {
                handler = handlerService.getFilePartHandler(threadPool, session, filePartDTO);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            handleFilePartAsync(filePartDTO, handler);
            filePartDTO = filePartDTOQueue.poll();
        }
        isRunning = false;
        Logger.log("Async service STOPPED handling FileParts");
    }

    public void handleFilePartAsync(FilePartDTO filePartDTO, Handler handler) {
        CompletableFuture<Boolean> future = CompletableFuture
                .supplyAsync(() -> {
                    Logger.logYellow("handler-" + Thread.currentThread().getName().substring(Thread.currentThread().getName().length() - 2) + " "
                            + filePartDTO.getFileInfoDTO().getName().split("\\.")[0] + "__" + filePartDTO.getOrder()
                            + " -----------> " + "started");
                    boolean result = false;
                    try {
                        result = handler.sendMessage(filePartDTO, filePartDTO.getFileInfoDTO().getName().split("\\.")[0] + "__" + filePartDTO.getOrder());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    handlerService.freeFilePartHandler(handler);
                    return result;
                }, handlerThreadPool)
                .thenApply((result) -> {
                    Logger.logYellow("handler-" + Thread.currentThread().getName().substring(Thread.currentThread().getName().length() - 2) + " "
                            + filePartDTO.getFileInfoDTO().getName().split("\\.")[0] + "__" + filePartDTO.getOrder()
                            + " -> " + result);
                    client.sayServerToLoadFile(filePartDTO.getFileInfoDTO());
                    return result;
                })
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return false;
                });
    }

    public void notifyHandler(FilePartDTO filePartDTO, boolean isSent) {
        Handler handler = handlerService.getHandlerByFilePart(filePartDTO);
        if (handler != null) {
            handler.setObjectIsSent(isSent);
            synchronized (handler.getObjectToSend()) {
                handler.getObjectToSend().notify();
            }
        }
    }
}
