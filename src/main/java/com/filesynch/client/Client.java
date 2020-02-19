package com.filesynch.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesynch.Main;
import com.filesynch.converter.*;
import com.filesynch.dto.*;
import com.filesynch.entity.*;
import com.filesynch.repository.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;
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

@Service
public class Client {
    private ClientInfoDTO clientInfoDTO;
    @Getter
    private ClientInfo clientInfo;
    @Getter
    @Setter
    private String login;
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
    private final ClientInfoRepository clientInfoRepository;
    private final FileInfoReceivedRepository fileInfoReceivedRepository;
    private final FileInfoSentRepository fileInfoSentRepository;
    private final FilePartReceivedRepository filePartReceivedRepository;
    private final FilePartSentRepository filePartSentRepository;
    private final TextMessageRepository textMessageRepository;
    private final int FILE_PART_SIZE = 1024; // in bytes (100 kB)
    public final String FILE_INPUT_DIRECTORY = "input_files/";
    public final String FILE_OUTPUT_DIRECTORY = "output_files/";
    public static final String CLIENT_LOGIN = "client_login";
    @Setter
    private JProgressBar fileProgressBar;
    private ObjectMapper mapper = new ObjectMapper();
    @Getter
    @Setter
    private RestClient restClient;

    public Client(ClientInfoRepository clientInfoRepository, FileInfoReceivedRepository fileInfoReceivedRepository, FileInfoSentRepository fileInfoSentRepository, FilePartReceivedRepository filePartReceivedRepository, FilePartSentRepository filePartSentRepository, TextMessageRepository textMessageRepository) {
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
    }

    public void sendTextMessageToClient(String message) {
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage(message);
        textMessageRepository.save(textMessage);
        Logger.log("server: " + message);
    }

    public boolean sendFileInfoToClient(FileInfoDTO fileInfoDTO) {
        FileInfoReceived existingFileInfo = fileInfoReceivedRepository.findByName(fileInfoDTO.getName());
        FileInfoReceived fileInfoReceived;
        if (existingFileInfo == null) {
            fileInfoReceived = fileInfoReceivedConverter.convertToEntity(fileInfoDTO);
            fileInfoReceived.setClient(clientInfo);
            fileInfoReceivedRepository.save(fileInfoReceived);
        } else {
            FileInfoReceived convertedFileInfo = fileInfoReceivedConverter.convertToEntity(fileInfoDTO);
            convertedFileInfo.setId(existingFileInfo.getId());
            fileInfoReceived = fileInfoReceivedRepository.save(convertedFileInfo);
            filePartReceivedRepository.removeAllByFileInfo(fileInfoReceived);
            File file = new File(FILE_INPUT_DIRECTORY + fileInfoReceived.getName());
            file.delete();
        }
        Logger.log(fileInfoDTO.toString());
        return true;
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

    public boolean registerToServer(ClientInfo clientInfo) throws Exception {
        String response = restClient.post("/register",
                mapper.writeValueAsString(clientInfoConverter.convertToDto(clientInfo)));
        clientInfoDTO = mapper.readValue(response, ClientInfoDTO.class);
        ClientInfo clientInfoFromDB = clientInfoRepository.findFirstByIdGreaterThan(0L);
        clientInfo = clientInfoConverter.convertToEntity(clientInfoDTO);
        clientInfo.setId(clientInfoFromDB.getId());
        this.clientInfo = clientInfoRepository.save(clientInfo);
        return clientInfoDTO != null;
    }

    public boolean loginToServer() throws Exception {
        String response = restClient.post("/login", mapper.writeValueAsString(clientInfo.getLogin()));
        login = clientInfo.getLogin();
        Logger.log(response);
        return true;
    }

    public void logoutFromServer() throws Exception {
        String response = restClient.post("/logout", mapper.writeValueAsString(clientInfo.getLogin()));
        login = clientInfo.getLogin();
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
                    filePartSession.sendMessage(
                            new org.springframework.web.socket.TextMessage(
                                    mapper.writeValueAsString(filePartDTO)));
                    result = true;
                    Logger.log(String.valueOf(result));
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
            filePartSession.sendMessage(
                    new org.springframework.web.socket.TextMessage(
                            mapper.writeValueAsString(filePartDTO)));
            result = true;
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
}
