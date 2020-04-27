package com.filesynch.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesynch.Main;
import com.filesynch.async.AsyncService;
import com.filesynch.client.websocket.*;
import com.filesynch.converter.*;
import com.filesynch.dto.*;
import com.filesynch.entity.*;
import com.filesynch.repository.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class Client {
    @Getter
    private ClientInfoDTO clientInfoDTO;
    @Getter
    @Setter
    private String login;
    @Getter
    @Setter
    private WebSocketSession registrationSession;
    @Getter
    @Setter
    private WebSocketSession textMessageSession;
    @Getter
    @Setter
    private WebSocketSession fileInfoSession;
    @Getter
    @Setter
    private WebSocketSession filePartSession;
    @Getter
    @Setter
    private WebSocketSession filePartStatusSession;
    @Getter
    @Setter
    private WebSocketSession fileStatusSession;
    @Getter
    @Setter
    private WebSocketSession loadFileSession;
    private ClientInfoConverter clientInfoConverter;
    private FileInfoReceivedConverter fileInfoReceivedConverter;
    private FileInfoSentConverter fileInfoSentConverter;
    private FilePartReceivedConverter filePartReceivedConverter;
    private FilePartSentConverter filePartSentConverter;
    private TextMessageConverter textMessageConverter;
    private final ClientInfoRepository clientInfoRepository;
    private final FileInfoReceivedRepository fileInfoReceivedRepository;
    @Getter
    private final FileInfoSentRepository fileInfoSentRepository;
    private final FilePartReceivedRepository filePartReceivedRepository;
    private final FilePartSentRepository filePartSentRepository;
    private final TextMessageRepository textMessageRepository;
    private final SettingsRepository settingsRepository;
    @Setter
    @Getter
    private AsyncService asyncService;
    @Getter
    @Setter
    private Settings settings;
    public static final String slash = File.separator;
    public static final String CLIENT_LOGIN = "client_login";
    public static final String CLIENT_NAME = "client_name";
    private ObjectMapper mapper = new ObjectMapper();
    @Getter
    @Setter
    private RestClient restClient;
    public HashMap<String, ArrayList<FilePartDTO>> filePartHashMap;

    public Client(ClientInfoRepository clientInfoRepository,
                  FileInfoReceivedRepository fileInfoReceivedRepository,
                  FileInfoSentRepository fileInfoSentRepository,
                  FilePartReceivedRepository filePartReceivedRepository,
                  FilePartSentRepository filePartSentRepository,
                  TextMessageRepository textMessageRepository,
                  SettingsRepository settingsRepository) {
        clientInfoConverter = new ClientInfoConverter();
        fileInfoReceivedConverter = new FileInfoReceivedConverter(clientInfoConverter);
        fileInfoSentConverter = new FileInfoSentConverter(clientInfoConverter);
        filePartReceivedConverter = new FilePartReceivedConverter(clientInfoConverter, fileInfoReceivedConverter);
        filePartSentConverter = new FilePartSentConverter(clientInfoConverter, fileInfoSentConverter);
        textMessageConverter = new TextMessageConverter(clientInfoConverter);

        ClientInfo clientInfo = clientInfoRepository.findFirstByIdGreaterThan(0L);
        if (clientInfo == null) {
            clientInfo = new ClientInfo();
            clientInfo.setStatus(ClientStatus.NEW);
            clientInfoDTO = clientInfoConverter.convertToDto(clientInfo);
        } else {
            clientInfoDTO = clientInfoConverter.convertToDto(clientInfo);
            login = clientInfo.getLogin();
        }
        Main.client = this;
        this.clientInfoRepository = clientInfoRepository;
        this.fileInfoReceivedRepository = fileInfoReceivedRepository;
        this.fileInfoSentRepository = fileInfoSentRepository;
        this.filePartReceivedRepository = filePartReceivedRepository;
        this.filePartSentRepository = filePartSentRepository;
        this.textMessageRepository = textMessageRepository;
        this.settingsRepository = settingsRepository;
        filePartHashMap = new HashMap<>();

        this.settings = settingsRepository.findById(1L).isPresent() ?
                settingsRepository.findById(1L).get() : new Settings();
    }

    public void saveTextMessage(String message) {
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage(message);
        textMessageRepository.save(textMessage);
        Logger.log("server: " + message);
    }

    public boolean saveFileInfo(FileInfoDTO fileInfoDTO) {
        if (isLoggedIn()) {
            filePartHashMap.put(fileInfoDTO.getName(), new ArrayList<>());
            FileInfoReceived existingFileInfo =
                    fileInfoReceivedRepository.findByHashAndName(fileInfoDTO.getHash(), fileInfoDTO.getName());
            FileInfoReceived fileInfoReceived;
            if (existingFileInfo == null) {
                fileInfoReceived = fileInfoReceivedConverter.convertToEntity(fileInfoDTO);
                fileInfoReceived.setClient(clientInfoRepository.findByLogin(login));
                fileInfoReceivedRepository.save(fileInfoReceived);
            } else {
                FileInfoReceived convertedFileInfo = fileInfoReceivedConverter.convertToEntity(fileInfoDTO);
                convertedFileInfo.setId(existingFileInfo.getId());
                fileInfoReceived = fileInfoReceivedRepository.save(convertedFileInfo);
                filePartReceivedRepository.removeAllByFileInfo(fileInfoReceived);
                File file = new File(fileInfoDTO.getClient().getInputFilesFolder() + fileInfoReceived.getName());
                file.delete();
            }
            File file =
                    new File(settings.getInputFilesDirectory() + fileInfoDTO.getName());
            File fileDir = new File(settings.getOutputFilesDirectory());
            File partsFileDir = new File(settings.getInputFilesDirectory() + "parts" + slash);
            fileDir.mkdir();
            partsFileDir.mkdir();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Logger.log(fileInfoDTO.toString());
            //Main.updateFileQueue();
            return true;
        } else {
            return false;
        }
    }

    public boolean saveFilePart(FilePartDTO filePartDTO) {
        if (isLoggedIn()) {
            try {
                String partHash = loadFilePart(filePartDTO);
                if (!partHash.equals(filePartDTO.getHashKey())) {
                    filePartDTO.setStatus(FilePartStatus.NOT_SENT);
                    sendFilePartStatusToServer(filePartDTO);
                    return false;
                }
                filePartDTO.setStatus(FilePartStatus.SENT);
                FilePartReceived filePart = filePartReceivedConverter.convertToEntity(filePartDTO);
                filePart.setClient(clientInfoRepository.findByLogin(login));
                FileInfoReceived fileInfo = fileInfoReceivedRepository
                        .findByHashAndName(filePart.getFileInfo().getHash(), filePart.getFileInfo().getName());
                if (fileInfo != null) {
                    filePart.setFileInfo(fileInfo);
                }
                filePartReceivedRepository.save(filePart);
                sendFilePartStatusToServer(filePartDTO);
                //queueFileParts.put(filePartDTO.getHashKey().toString(), filePartDTO);
                //Main.updateFileQueue();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean saveFilePartStatus(FilePartDTO filePartDTO) {
        if (isLoggedIn()) {
            FilePartSent filePartSent = filePartSentRepository
                    .findByHashKeyAndFileInfo_Name(
                            filePartDTO.getHashKey(),
                            filePartDTO.getFileInfoDTO().getName());
            filePartSent.setStatus(filePartDTO.getStatus());
            filePartSentRepository.save(filePartSent);
            return true;
        } else {
            return false;
        }
    }

    private String loadFilePart(FilePartDTO filePartDTO) throws IOException {
        File file =
                new File(
                        settings.getInputFilesDirectory() + "parts" + slash
                                + filePartDTO.getFileInfoDTO().getName().split("\\.")[0]
                                + "__" + filePartDTO.getOrder() + "."
                                + filePartDTO.getFileInfoDTO().getName().split("\\.")[1]);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        FileOutputStream out = new FileOutputStream(file, true);
        out.write(filePartDTO.getData(), 0, filePartDTO.getLength());
        out.flush();
        out.close();
        return getFileHash(settings.getInputFilesDirectory() + "parts" + slash
                + filePartDTO.getFileInfoDTO().getName().split("\\.")[0]
                + "__" + filePartDTO.getOrder() + "."
                + filePartDTO.getFileInfoDTO().getName().split("\\.")[1]);
    }

    public void loadFile(FileInfoDTO fileInfoDTO) throws IOException {
        File file =
                new File(settings.getInputFilesDirectory() + fileInfoDTO.getName());
        if (!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream out = new FileOutputStream(file, true);
        for (int i = 1; i <= fileInfoDTO.getPartsQuantity(); i++) {
            FileInputStream in = new FileInputStream(
                    settings.getInputFilesDirectory() + "parts" + slash
                            + fileInfoDTO.getName().split("\\.")[0]
                            + "__" + i + "."
                            + fileInfoDTO.getName().split("\\.")[1]);
            byte[] filePartData = new byte[(settings.getFilePartSize() * 1024)];
            int bytesCount = in.read(filePartData);
            out.write(filePartData, 0, bytesCount);
            out.flush();
        }
        out.close();
        String realFileHash = getFileHash(settings.getInputFilesDirectory() + fileInfoDTO.getName());
        if (realFileHash.equals(fileInfoDTO.getHash())) {
            fileInfoDTO.setFileStatus(FileStatus.TRANSFERRED);
        } else {
            fileInfoDTO.setFileStatus(FileStatus.NOT_TRANSFERRED);
        }
        sendFileStatusToServer(fileInfoDTO);
    }

    private void sendFileStatusToServer(FileInfoDTO fileInfoDTO) {
        try {
            fileStatusSession.sendMessage(new org.springframework.web.socket.TextMessage(
                    mapper.writeValueAsString(fileInfoDTO)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFilePartStatusToServer(FilePartDTO filePartDTO) {
        filePartDTO.setData(null);
        try {
            synchronized (filePartStatusSession) {
                filePartStatusSession
                        .sendMessage(new org.springframework.web.socket.TextMessage(
                                mapper.writeValueAsString(filePartDTO)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean registerToServer(ClientInfo clientInfo, RegistrationWebSocket registrationWebSocket, String wsURI) {
        clientInfoDTO = clientInfoConverter.convertToDto(clientInfo);
        try {
            StandardWebSocketClient standardWebSocketClient = new StandardWebSocketClient();

            WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
            webSocketHttpHeaders.add(Client.CLIENT_NAME, clientInfoDTO.getName());
            ListenableFuture<WebSocketSession> registrationFut =
                    standardWebSocketClient
                            .doHandshake(registrationWebSocket, webSocketHttpHeaders, new URI(wsURI + "/register"));

            WebSocketSession registrationSession = registrationFut.get();
            this.registrationSession = registrationSession;
            registrationSession
                    .sendMessage(
                            new org.springframework.web.socket.TextMessage(
                                    mapper.writeValueAsString(clientInfoConverter
                                            .convertToDto(clientInfo))));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void isRegistered(ClientInfoDTO clientInfoDTO) {
        ClientInfo clientInfoFromDB = clientInfoRepository.findFirstByIdGreaterThan(0L);
        ClientInfo clientInfo = clientInfoConverter.convertToEntity(clientInfoDTO);
        clientInfo.setId(clientInfoFromDB.getId());
        this.clientInfoDTO = clientInfoConverter.convertToDto(clientInfoRepository.save(clientInfo));
    }

    public boolean loginToServer() throws Exception {
        String response = restClient.post("/login", mapper.writeValueAsString(clientInfoDTO.getLogin()));
        login = clientInfoDTO.getLogin();
        Logger.log(response);
        return true;
    }

    public void logoutFromServer() throws Exception {
        String response = restClient.post("/logout", mapper.writeValueAsString(clientInfoDTO.getLogin()));
        login = clientInfoDTO.getLogin();
        Logger.log(response);
    }

    public boolean sendTextMessageToServer(String message) {
        setClientStatus(ClientStatus.CLIENT_WORK);
        if (isLoggedIn()) {
            try {
                textMessageSession.sendMessage(
                        new org.springframework.web.socket.TextMessage(
                                mapper.writeValueAsString(message)));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else {
            Logger.log("You are not logged in!");
            return false;
        }
    }

    public boolean sendAllFilesToServer() {
        setClientStatus(ClientStatus.CLIENT_WORK);
        if (!isLoggedIn()) {
            Logger.log("You are not logged in");
            return false;
        }
        try (Stream<Path> walk = Files.walk(Paths.get(settings.getOutputFilesDirectory()
                .substring(0, settings.getOutputFilesDirectory().length() - 1)))) {
            List<String> filePathNames = walk.filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
            for (String filePath : filePathNames) {
                while (!sendFileToServer(filePath.replace(settings.getOutputFilesDirectory(), ""))) {
                    try {
                        Thread.sleep(25000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean sendFileToServer(String filename) {
        if (filename.charAt(0) == '.') {
            Logger.log("Can't send hidden file");
            return true;
        }
        setClientStatus(ClientStatus.CLIENT_WORK);
        File file = new File(settings.getOutputFilesDirectory() + filename);
        String fileHash = getFileHash(settings.getOutputFilesDirectory() + filename);
        if (!file.exists()) {
            Logger.log("File " + filename + " not exists");
            return false;
        }
        FileInfoSent fileInfo = null;
        FileInfoDTO fileInfoDTO = null;
        ClientInfo clientInfo = null;
        try {
            FileInputStream in = new FileInputStream(file);
            fileInfo = fileInfoSentRepository.findByHashAndName(fileHash, filename);
            if (fileInfo == null) {
                fileInfoDTO = new FileInfoDTO();
                fileInfoDTO.setHash(fileHash);
                fileInfoDTO.setName(filename);
                fileInfoDTO.setSize(file.length());
                fileInfoDTO.setPartsQuantity((int) (file.length() / (settings.getFilePartSize() * 1024)
                        + (file.length() % (settings.getFilePartSize() * 1024) == 0 ? 0 : 1)));
                clientInfo = clientInfoRepository.findByLogin(login);
                ClientInfoDTO clientInfoDTO = clientInfoConverter.convertToDto(clientInfo);
                fileInfoDTO.setClient(clientInfoDTO);
                fileInfo = fileInfoSentConverter.convertToEntity(fileInfoDTO);
                fileInfo.setClient(clientInfo);
                fileInfo.setFileStatus(FileStatus.NOT_TRANSFERRED);
                fileInfoSession
                        .sendMessage(
                                new org.springframework.web.socket.TextMessage(mapper
                                        .writeValueAsString(fileInfoDTO)));
                fileInfoSentRepository.save(fileInfo);
                //queueFileInfo.put(fileInfoDTO.getHash(), fileInfoDTO);
                //Main.updateFileQueue();
            }
            fileInfoDTO = fileInfoSentConverter.convertToDto(fileInfo);
            clientInfo = clientInfoRepository.findByLogin(login);
            ClientInfoDTO clientInfoDTO = clientInfoConverter.convertToDto(clientInfo);

            byte[] fileData = new byte[(settings.getFilePartSize() * 1024)];
            int bytesCount = in.read(fileData);
            int step = 1;
            while (bytesCount > 0) {
                String hashKey = getFilePartHash(bytesCount, fileData);
                FilePartSent existsFilePartSent =
                        filePartSentRepository.findByHashKeyAndFileInfo_Name(hashKey, filename);
                if (existsFilePartSent == null) {
                    FilePartDTO filePartDTO = new FilePartDTO();
                    filePartDTO.setOrder(step);
                    filePartDTO.setFileInfoDTO(fileInfoDTO);
                    filePartDTO.setData(fileData);
                    filePartDTO.setLength(bytesCount);
                    filePartDTO.setStatus(FilePartStatus.WAIT);
                    filePartDTO.setClient(clientInfoDTO);
                    filePartDTO.setHashKey(getFilePartHash(bytesCount, fileData));
                    FilePartSent filePart = filePartSentConverter.convertToEntity(filePartDTO);
                    filePart.setFileInfo(fileInfo);
                    filePart.setClient(clientInfo);
                    filePartSentRepository.save(filePart);
                    sendFilePartToServer(filePartDTO);
                } else if (!(existsFilePartSent.getStatus() == FilePartStatus.SENT)) {
                    FilePartDTO filePartDTO = filePartSentConverter.convertToDto(existsFilePartSent);
                    filePartDTO.setOrder(step);
                    filePartDTO.setFileInfoDTO(fileInfoDTO);
                    filePartDTO.setData(fileData);
                    filePartDTO.setLength(bytesCount);
                    filePartDTO.setStatus(FilePartStatus.WAIT);
                    filePartDTO.setClient(clientInfoDTO);
                    filePartDTO.setHashKey(getFilePartHash(bytesCount, fileData));
                    FilePartSent filePart = filePartSentConverter.convertToEntity(filePartDTO);
                    filePart.setFileInfo(fileInfo);
                    filePart.setClient(clientInfo);
                    sendFilePartToServer(filePartDTO);
                }
                step++;
                fileData = new byte[(settings.getFilePartSize() * 1024)];
                bytesCount = in.read(fileData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //queueFiles.put(fileInfoDTO.getHash(), fileInfoDTO);
        //Main.updateFileQueue();
        //Logger.log("File with hash: " + fileInfoDTO.getHash() + " sent");
        return true;
    }

    public boolean sendFilePartToServer(FilePartDTO filePartDTO) {
        asyncService.addFilePartToHandling(filePartDTO, filePartSession);
        return true;
    }

    public boolean sayServerToLoadFile(FileInfoDTO fileInfoDTO) {
        FileInfoSent fileInfo = fileInfoSentRepository.findByHashAndName(fileInfoDTO.getHash(), fileInfoDTO.getName());
        List<FilePartSent> filePartsWait = filePartSentRepository
                .findAllByFileInfoAndStatus(fileInfo, FilePartStatus.WAIT);
        List<FilePartSent> filePartsNotSent = filePartSentRepository
                .findAllByFileInfoAndStatus(fileInfo, FilePartStatus.NOT_SENT);
        if (filePartsWait.size() == 0 && filePartsNotSent.size() == 0) {
            fileInfoDTO.setFileStatus(FileStatus.TRANSFERRED);
            try {
                loadFileSession.sendMessage(
                        new org.springframework.web.socket.TextMessage(mapper.writeValueAsString(fileInfoDTO))
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private void setClientStatus(ClientStatus clientStatus) {
        clientInfoDTO.setStatus(clientStatus);
        ClientInfo clientInfo = clientInfoRepository.findByLogin(clientInfoDTO.getLogin());
        clientInfo.setStatus(clientStatus);
        clientInfoRepository.save(clientInfo);
    }

    public boolean isLoggedIn() {
        return login != null;
    }

    public boolean clientIsRegistered() {
        ClientInfo client = clientInfoRepository.findFirstByIdGreaterThan(0L);
        if (client.getLogin() == null) {
            Logger.log("Not registered");
            return false;
        }
        return true;
    }

    private String getFileHash(String filePathname) {
        String hash = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            File file = new File(filePathname);
            FileInputStream fis = new FileInputStream(file);

            byte[] byteArray = new byte[1024];
            int bytesCount = 0;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
            ;
            fis.close();
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            hash = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hash;
    }

    private String getFilePartHash(int bytesCount, byte[] byteArray) {
        String hash = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(byteArray, 0, bytesCount);
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            hash = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hash;
    }

    public synchronized void doReconnection(int repeat) {
        for (int i = 1; i <= repeat; i++) {
            if (textMessageSession == null || !textMessageSession.isOpen()) {
                TextMessageWebSocket textMessageWebSocket = new TextMessageWebSocket();
                textMessageSession = reconnectWebSocket(textMessageWebSocket, "/text");
            }
            if (fileInfoSession == null || !fileInfoSession.isOpen()) {
                FileInfoWebSocket fileInfoWebSocket = new FileInfoWebSocket();
                fileInfoSession = reconnectWebSocket(fileInfoWebSocket, "/file-info");
            }
            if (fileStatusSession == null || !fileStatusSession.isOpen()) {
                FileStatusWebSocket fileStatusWebSocket = new FileStatusWebSocket();
                fileStatusSession = reconnectWebSocket(fileStatusWebSocket, "/file-status");
            }
            if (filePartSession == null || !filePartSession.isOpen()) {
                FilePartWebSocket filePartWebSocket = new FilePartWebSocket();
                filePartSession = reconnectWebSocket(filePartWebSocket, "/file-part");
            }
            if (filePartStatusSession == null || !filePartStatusSession.isOpen()) {
                FilePartStatusWebSocket filePartStatusWebSocket = new FilePartStatusWebSocket();
                filePartStatusSession = reconnectWebSocket(filePartStatusWebSocket, "/file-part-status");
            }
            if (loadFileSession == null || !loadFileSession.isOpen()) {
                LoadFileWebSocket loadFileWebSocket = new LoadFileWebSocket();
                loadFileSession = reconnectWebSocket(loadFileWebSocket, "/load-file");
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private WebSocketSession reconnectWebSocket(TextWebSocketHandler webSocket, String uri) {
        String wsURI = "ws://" + Main.ip + ":" + Main.port;
        StandardWebSocketClient standardWebSocketClient = new StandardWebSocketClient();
        WebSocketSession webSocketSession = null;
        try {
            WebSocketHttpHeaders webSocketHttpHeaders = new WebSocketHttpHeaders();
            webSocketHttpHeaders.add(CLIENT_LOGIN, getLogin());
            ListenableFuture<WebSocketSession> listenableFuture =
                    standardWebSocketClient
                            .doHandshake(webSocket, webSocketHttpHeaders, new URI(wsURI + uri));
            webSocketSession = listenableFuture.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return webSocketSession;
    }
}
