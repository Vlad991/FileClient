package com.filesynch.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesynch.Main;
import com.filesynch.configuration.DataConfig;
import com.filesynch.converter.*;
import com.filesynch.dto.*;
import com.filesynch.entity.*;
import com.filesynch.repository.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.DigestUtils;
import org.springframework.web.socket.WebSocketSession;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Client {
    private ClientInfoDTO clientInfoDTO;
    @Getter
    private ClientInfo clientInfo;
    @Getter
    @Setter
    private String login;
    @Getter
    private WebSocketSession loginSession;
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
    private WebSocketSession firstFilePartSession;
    private ClientInfoConverter clientInfoConverter;
    private FileInfoReceivedConverter fileInfoReceivedConverter;
    private FileInfoSentConverter fileInfoSentConverter;
    private FilePartReceivedConverter filePartReceivedConverter;
    private FilePartSentConverter filePartSentConverter;
    private TextMessageConverter textMessageConverter;
    private ClientInfoRepository clientInfoRepository;
    private FileInfoReceivedRepository fileInfoReceivedRepository;
    private FileInfoSentRepository fileInfoSentRepository;
    private FilePartReceivedRepository filePartReceivedRepository;
    private FilePartSentRepository filePartSentRepository;
    private TextMessageRepository textMessageRepository;
    private final int FILE_PART_SIZE = 1024 * 100; // in bytes (100 kB)
    public final String FILE_INPUT_DIRECTORY = "input_files/";
    public final String FILE_OUTPUT_DIRECTORY = "output_files/";
    public static final String CLIENT_LOGIN = "client_login";
    @Setter
    private JProgressBar fileProgressBar;
    private ObjectMapper mapper = new ObjectMapper();

    public Client(WebSocketSession loginSession) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(DataConfig.class);
        ctx.refresh();
        clientInfoRepository = ctx.getBean(ClientInfoRepository.class);
        fileInfoReceivedRepository = ctx.getBean(FileInfoReceivedRepository.class);
        fileInfoSentRepository = ctx.getBean(FileInfoSentRepository.class);
        filePartReceivedRepository = ctx.getBean(FilePartReceivedRepository.class);
        filePartSentRepository = ctx.getBean(FilePartSentRepository.class);
        textMessageRepository = ctx.getBean(TextMessageRepository.class);
        clientInfoConverter = new ClientInfoConverter();
        fileInfoReceivedConverter = new FileInfoReceivedConverter(clientInfoConverter);
        fileInfoSentConverter = new FileInfoSentConverter(clientInfoConverter);
        filePartReceivedConverter = new FilePartReceivedConverter(clientInfoConverter, fileInfoReceivedConverter);
        filePartSentConverter = new FilePartSentConverter(clientInfoConverter, fileInfoSentConverter);
        textMessageConverter = new TextMessageConverter(clientInfoConverter);

        Optional<ClientInfo> clientInfoOptional = clientInfoRepository.findById(1L);
        if (!clientInfoOptional.isPresent()) {
            clientInfo = new ClientInfo();
            clientInfo.setStatus(ClientStatus.CLIENT_STANDBY);
            clientInfoDTO = clientInfoConverter.convertToDto(clientInfo);
        } else {
            clientInfo = clientInfoOptional.get();
            clientInfo.setStatus(ClientStatus.CLIENT_STANDBY);
            clientInfoDTO = clientInfoConverter.convertToDto(clientInfo);
            login = clientInfo.getLogin();
        }
        this.loginSession = loginSession;
        Main.client = this;
    }

    public boolean sendLoginToClient(String login) {
        this.login = login;
        clientInfoDTO.setLogin(login);
        clientInfo = clientInfoConverter.convertToEntity(clientInfoDTO);
        clientInfoRepository.save(clientInfo);
        clientInfo = clientInfoRepository.findByLogin(clientInfo.getLogin());
        return true;
    }

    public void sendTextMessageToClient(String message) {
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage(message);
        textMessageRepository.save(textMessage);
        Logger.log(message);
    }

    public boolean sendCommandToClient(String command) {
        Logger.log(command);
        return true;
    }

    public boolean sendFileInfoToClient(FileInfoDTO fileInfoDTO) {
        FileInfoReceived fileInfoReceived = fileInfoReceivedConverter.convertToEntity(fileInfoDTO);
        fileInfoReceived.setClient(clientInfo);
        fileInfoReceivedRepository.save(fileInfoReceived);
        Logger.log(fileInfoDTO.toString());
        return true;
    }

    public FilePartDTO getFirstNotSentFilePartFromClient(FileInfoDTO fileInfoDTO) {
        FileInfoReceived fileInfoReceived = fileInfoReceivedRepository
                .findByHash(fileInfoDTO.getHash());
        if (fileInfoReceived == null) {
            FilePartDTO filePartDTO = new FilePartDTO();
            filePartDTO.setOrder(1);
            filePartDTO.setStatus(FilePartStatus.NOT_SENT);
            return filePartDTO;
        }
        List<FilePartReceived> filePartReceivedList = filePartReceivedRepository.findAllByFileInfo(fileInfoReceived);
        if (filePartReceivedList.size() == 0) {
            FilePartDTO filePartDTO = new FilePartDTO();
            filePartDTO.setOrder(1);
            filePartDTO.setStatus(FilePartStatus.NOT_SENT);
            return filePartDTO;
        }
        Collections.sort(filePartReceivedList, new Comparator<FilePartReceived>() {
            public int compare(FilePartReceived o1, FilePartReceived o2) {
                return Integer.compare(o1.getOrder(), o2.getOrder());
            }
        });
        FilePartReceived firstNotSentFilePartReceived = filePartReceivedList.stream()
                .filter(fp -> (fp.getStatus() == FilePartStatus.NOT_SENT))
                .findFirst()
                .get();
        return filePartReceivedConverter.convertToDto(firstNotSentFilePartReceived);
    }

    public boolean sendFilePartToClient(FilePartDTO filePartDTO) {
        try {
            File file = new File(FILE_INPUT_DIRECTORY + filePartDTO.getFileInfoDTO().getName());
            if (filePartDTO.getOrder() == 1) {
                file.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(file, true);
            out.write(filePartDTO.getData(), 0, filePartDTO.getLength());
            out.flush();
            out.close();
            filePartDTO.setStatus(FilePartStatus.SENT);
            FilePartReceived filePart = filePartReceivedConverter.convertToEntity(filePartDTO);
            filePart.setClient(clientInfo);
            FileInfoReceived fileInfo = fileInfoReceivedRepository
                    .findByHash(filePart.getFileInfo().getHash());
            if (fileInfo != null) {
                filePart.setFileInfo(fileInfo);
            }
            filePartReceivedRepository.save(filePart);
            Logger.log(filePart.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean loginToServer() {
        setClientStatus(ClientStatus.CLIENT_FIRST);
        if (!isLoggedIn()) {
            try {
                synchronized (this) {
                    loginSession
                            .sendMessage(
                                    new org.springframework.web.socket.TextMessage(
                                            mapper.writeValueAsString(clientInfoDTO)));
                    this.wait();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (login == null) {
                Logger.log("Log in failed!");
                return false;
            }
            clientInfo.setLogin(login);
            clientInfo.setStatus(ClientStatus.CLIENT_SECOND);
            clientInfoDTO.setLogin(login);
            clientInfoDTO.setStatus(ClientStatus.CLIENT_SECOND);
            clientInfoRepository.save(clientInfo);
            Logger.log("Log in success!");
        } else {
            try {
                loginSession
                        .sendMessage(
                                new org.springframework.web.socket.TextMessage(
                                        mapper.writeValueAsString(clientInfoDTO)));
                synchronized (this) {
                    this.wait();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        loginSession.getAttributes().put(CLIENT_LOGIN, login);
        return true;
    }

    public boolean sendTextMessageToServer(String message) {
        setClientStatus(ClientStatus.CLIENT_WORK);
        if (isLoggedIn()) {
            try {
                textMessageSession.getAttributes().put(CLIENT_LOGIN, login);
                synchronized (textMessageSession) {
                    textMessageSession.sendMessage(
                            new org.springframework.web.socket.TextMessage(
                                    mapper.writeValueAsString(message)));
                    textMessageSession.wait();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else {
            Logger.log("You are not logged in!");
            return false;
        }
    }

    // this is cycle for sending file parts from client to server
    public boolean sendFileToServerFast(String filename) {
        if (filename.charAt(0) == '.') {
            Logger.log("Can't send hidden file");
            return true;
        }
        if (isLoggedIn()) {
            setClientStatus(ClientStatus.CLIENT_WORK);
            try {
                String filePathname = FILE_OUTPUT_DIRECTORY + filename;
                File file = new File(filePathname);
                FileInputStream in = new FileInputStream(file);

                FileInfoDTO fileInfoDTO = new FileInfoDTO();
                fileInfoDTO.setName(filename);
                fileInfoDTO.setSize(file.length());
                fileInfoDTO.setClient(clientInfoDTO);
                fileInfoDTO.setHash(DigestUtils.md5DigestAsHex(in));
                fileInfoSession
                        .sendMessage(
                                new org.springframework.web.socket.TextMessage(
                                        mapper.writeValueAsString(fileInfoDTO)));
                FileInfoSent fileInfo = fileInfoSentConverter.convertToEntity(fileInfoDTO);
                fileInfo.setClient(clientInfo);
                fileInfo.setFileStatus(FileStatus.TRANSFER_PROCESS);
                fileInfo = fileInfoSentRepository.save(fileInfo);

                byte[] fileData = new byte[FILE_PART_SIZE];
                int bytesCount = in.read(fileData);
                int step = 1;
                fileProgressBar.setMinimum(0);
                fileProgressBar.setMaximum((int) fileInfoDTO.getSize());
                int progressValue = 0;
                while (bytesCount > 0) {
                    Logger.log(String.valueOf(bytesCount));
                    FilePartDTO filePartDTO = new FilePartDTO();
                    filePartDTO.setOrder(step);
                    step++;
                    filePartDTO.setHashKey((long) filePartDTO.hashCode());
                    filePartDTO.setFileInfoDTO(fileInfoDTO);
                    filePartDTO.setData(fileData);
                    filePartDTO.setLength(bytesCount);
                    filePartDTO.setStatus(FilePartStatus.NOT_SENT);
                    filePartDTO.setClient(clientInfoDTO);
                    boolean result;
                    synchronized (textMessageSession) {
                        filePartSession.sendMessage(
                                new org.springframework.web.socket.TextMessage(
                                        mapper.writeValueAsString(filePartDTO)));
                        textMessageSession.wait();
                        result = true; // todo: check if result false (can see only in logger)
                    }
                    Logger.log(String.valueOf(result));
                    // todo check for "true" from method sendFilePart()!!!!!!!!!!!!
                    FilePartSent filePartSent = filePartSentConverter.convertToEntity(filePartDTO);
                    filePartSent.setClient(clientInfo);
                    filePartSent.setFileInfo(fileInfo);
                    filePartSent.setStatus(FilePartStatus.SENT);
                    filePartSentRepository.save(filePartSent);

                    bytesCount = in.read(fileData);
                    progressValue += FILE_PART_SIZE;
                    fileProgressBar.setValue(progressValue);
                    //Thread.sleep(2000);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        } else {
            Logger.log("You are not logged in!");
            return false;
        }

        return true;
    }

    public boolean sendAllFilesToServer() {
        setClientStatus(ClientStatus.CLIENT_WORK);
        if (!isLoggedIn()) {
            Logger.log("You are not logged in");
            return false;
        }
        try (Stream<Path> walk = Files.walk(Paths.get(FILE_OUTPUT_DIRECTORY
                .substring(0, FILE_OUTPUT_DIRECTORY.length() - 1)))) {
            List<String> filePathNames = walk.filter(Files::isRegularFile)
                    .map(x -> x.toString()).collect(Collectors.toList());
            for (String filePath : filePathNames) {
                boolean result = sendFileToServer(filePath.replace(FILE_OUTPUT_DIRECTORY, ""));
                if (!result) {
                    return false;
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
        if (!isLoggedIn()) {
            Logger.log("You are not logged in");
            return false;
        }
        File file = new File(FILE_OUTPUT_DIRECTORY + filename);
        String fileHash = getFileHash(FILE_OUTPUT_DIRECTORY + filename);
        if (!file.exists()) {
            Logger.log("File " + filename + " not exists");
            return false;
        }
        LinkedHashMap<Integer, FilePartDTO> filePartHashMap = new LinkedHashMap<>();
        FileInfoSent fileInfo = null;
        FileInfoDTO fileInfoDTO = null;
        try {
            FileInputStream in = new FileInputStream(file);
            fileInfo = fileInfoSentRepository.findByHash(fileHash);
            if (fileInfo == null) {
                fileInfoDTO = new FileInfoDTO();
                fileInfoDTO.setHash(fileHash);
                fileInfoDTO.setName(filename);
                fileInfoDTO.setSize(file.length());
                ClientInfoDTO clientInfoDTO = clientInfoConverter.convertToDto(clientInfo);
                fileInfoDTO.setClient(clientInfoDTO);
                fileInfo = fileInfoSentConverter.convertToEntity(fileInfoDTO);
                fileInfo.setClient(clientInfo);
                fileInfo.setFileStatus(FileStatus.NOT_TRANSFERRED);
                fileInfoSession
                        .sendMessage(
                                new org.springframework.web.socket.TextMessage(
                                        mapper.writeValueAsString(fileInfoDTO)));
                fileInfoSentRepository.save(fileInfo);
            }
            fileInfoDTO = fileInfoSentConverter.convertToDto(fileInfo);

            byte[] fileData = new byte[FILE_PART_SIZE];
            int bytesCount = in.read(fileData);
            int step = 1;
            while (bytesCount > 0) {
                FilePartDTO filePartDTO = new FilePartDTO();
                filePartDTO.setOrder(step);
                filePartDTO.setFileInfoDTO(fileInfoDTO);
                filePartDTO.setData(fileData);
                filePartDTO.setLength(bytesCount);
                filePartDTO.setStatus(FilePartStatus.NOT_SENT);
                filePartDTO.setClient(clientInfoDTO);
                filePartDTO.setHashKey((long) filePartDTO.hashCode());
                filePartHashMap.put(step, filePartDTO);
                step++;
                fileData = new byte[FILE_PART_SIZE];
                bytesCount = in.read(fileData);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        switch (fileInfo.getFileStatus()) {
            case NOT_TRANSFERRED:
                filePartSentRepository.deleteAllByFileInfo_Id(fileInfo.getId());
                for (Map.Entry<Integer, FilePartDTO> entry : filePartHashMap.entrySet()) {
                    FilePartSent filePart = filePartSentConverter.convertToEntity(entry.getValue());
                    filePart.setClient(clientInfo);
                    filePart.setFileInfo(fileInfo);
                    filePartSentRepository.save(filePart);
                }
                boolean result = sendAllFilePartsToServer(filePartHashMap, fileInfoDTO);
                if (result) {
                    Logger.log("File with hash: " + fileInfoDTO.getHash() + " sent");
                }
                return result;
            case TRANSFER_PROCESS:
                result = sendAllFilePartsToServer(filePartHashMap, fileInfoDTO);
                if (result) {
                    Logger.log("File with hash: " + fileInfoDTO.getHash() + " sent");
                }
                return result;
            case TRANSFERRED:
                Logger.log("File with hash: " + fileInfoDTO.getHash() + " sent");
                return true;
        }
        return true;
    }

    private boolean sendAllFilePartsToServer(LinkedHashMap<Integer, FilePartDTO> filePartHashMap,
                                             FileInfoDTO fileInfoDTO) {
        FileInfoSent fileInfo = fileInfoSentRepository.findByHash(fileInfoDTO.getHash());
        fileInfo.setFileStatus(FileStatus.TRANSFER_PROCESS);
        fileInfo = fileInfoSentRepository.save(fileInfo);
        List<FilePartSent> filePartList = filePartSentRepository.findAllByFileInfo(fileInfo);
        Collections.sort(filePartList, new Comparator<FilePartSent>() {
            public int compare(FilePartSent o1, FilePartSent o2) {
                return Integer.compare(o1.getOrder(), o2.getOrder());
            }
        });
        FilePartSent firstNotSentFilePart = filePartList.stream()
                .filter(fp -> (fp.getStatus() == FilePartStatus.NOT_SENT))
                .findFirst()
                .get();
        FilePartDTO firstNotSentFilePartDTOFromClient = null;
        try {
            synchronized (firstFilePartSession) {
                //firstFilePartSession.getAttributes().put("first_f_p", "true");
                firstFilePartSession
                        .sendMessage(
                                new org.springframework.web.socket.TextMessage(
                                        mapper.writeValueAsString(fileInfoDTO)));
                firstFilePartSession.wait();
                firstNotSentFilePartDTOFromClient =
                        (FilePartDTO) firstFilePartSession.getAttributes().get("first_file_part");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        FilePartSent firstNotSentFilePartFromClient = null;
        if (firstNotSentFilePartDTOFromClient.getOrder() == 1) {
            firstNotSentFilePartFromClient = firstNotSentFilePart;
        } else {
            firstNotSentFilePartFromClient =
                    filePartSentConverter.convertToEntity(firstNotSentFilePartDTOFromClient);
        }
        if (firstNotSentFilePart.getOrder() != firstNotSentFilePartFromClient.getOrder()) {
            if (firstNotSentFilePart.getOrder() > firstNotSentFilePartFromClient.getOrder()) {
                for (FilePartSent fp : filePartList) {
                    if (fp.getOrder() == (firstNotSentFilePart.getOrder() - 1)) {
                        fp.setStatus(FilePartStatus.NOT_SENT);
                        break;
                    }
                }
            } else {
                firstNotSentFilePart.setStatus(FilePartStatus.SENT);
            }
            sendAllFilePartsToServer(filePartHashMap, fileInfoDTO);
        } else {
            fileProgressBar.setMinimum(0);
            fileProgressBar.setMaximum((int) fileInfoDTO.getSize());
            int progressValue = 0;
            for (int i = firstNotSentFilePart.getOrder(); i <= filePartHashMap.size(); i++) {
                try {
                    FilePartDTO filePartDTOToSend = filePartHashMap.get(i);
                    FilePartSent filePartToSend = filePartList.get(i - 1);

                    sendFilePartToServer(filePartDTOToSend);

                    filePartToSend.setStatus(FilePartStatus.SENT);
                    filePartSentRepository.save(filePartToSend);
                    progressValue += FILE_PART_SIZE;
                    fileProgressBar.setValue(progressValue);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false; //todo? (break)
                }
            }
        }
        fileInfo.setFileStatus(FileStatus.TRANSFERRED);
        fileInfoSentRepository.save(fileInfo);
        return true;
    }

    public boolean sendFilePartToServer(FilePartDTO filePartDTO) {
        boolean result = false;
        try {
            synchronized (textMessageSession) {
                filePartSession.sendMessage(
                        new org.springframework.web.socket.TextMessage(
                                mapper.writeValueAsString(filePartDTO)));
                textMessageSession.wait();
                result = true; // todo: check if result false (can see only in logger)
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result) {
            FilePartSent filePartSent = filePartSentRepository.findByHashKey(filePartDTO.getHashKey());
            filePartSent.setStatus(FilePartStatus.SENT);
            filePartSentRepository.save(filePartSent);
            return true;
        } else {
            return false;
        }
    }

    private void setClientStatus(ClientStatus clientStatus) {
        clientInfo.setStatus(clientStatus);
        clientInfoDTO.setStatus(clientStatus);
    }

    public boolean isLoggedIn() {
        return login != null;
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
}
