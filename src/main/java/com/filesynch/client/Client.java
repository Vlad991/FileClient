package com.filesynch.client;

import com.filesynch.configuration.DataConfig;
import com.filesynch.converter.*;
import com.filesynch.dto.*;
import com.filesynch.entity.*;
import com.filesynch.repository.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.DigestUtils;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Client extends UnicastRemoteObject implements ClientInt {
    private ClientInfoDTO clientInfoDTO;
    @Getter
    private transient ClientInfo clientInfo;
    private transient ServerInt server;
    private transient ClientInfoConverter clientInfoConverter;
    private transient FileInfoReceivedConverter fileInfoReceivedConverter;
    private transient FileInfoSentConverter fileInfoSentConverter;
    private transient FilePartReceivedConverter filePartReceivedConverter;
    private transient FilePartSentConverter filePartSentConverter;
    private transient TextMessageConverter textMessageConverter;
    private transient ClientInfoRepository clientInfoRepository;
    private transient FileInfoReceivedRepository fileInfoReceivedRepository;
    private transient FileInfoSentRepository fileInfoSentRepository;
    private transient FilePartReceivedRepository filePartReceivedRepository;
    private transient FilePartSentRepository filePartSentRepository;
    private transient TextMessageRepository textMessageRepository;
    private final int FILE_PART_SIZE = 1024*100; // in bytes (100 kB)
    public final String FILE_INPUT_DIRECTORY = "src/main/resources/in/";
    public final String FILE_OUTPUT_DIRECTORY = "src/main/resources/out/";
    @Setter
    @Getter
    private transient Logger logger;
    @Setter
    private transient JProgressBar fileProgressBar;

    public Client(ServerInt serverInt) throws RemoteException {
        super(143);
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
        }
        this.server = serverInt;
    }

    @Override
    public boolean sendLoginToClient(String login) {
        clientInfoDTO.setLogin(login);
        clientInfo = clientInfoConverter.convertToEntity(clientInfoDTO);
        clientInfoRepository.save(clientInfo);
        clientInfo = clientInfoRepository.findByLogin(clientInfo.getLogin());
        return true;
    }

    @Override
    public ClientInfoDTO getClientInfoFromClient() {
        return clientInfoDTO;
    }

    @Override
    public void sendTextMessageToClient(String message) {
        TextMessage textMessage = new TextMessage();
        textMessage.setMessage(message);
        textMessageRepository.save(textMessage);
        logger.log(message);
    }

    @Override
    public boolean sendCommandToClient(String command) {
        logger.log(command);
        return true;
    }

    @Override
    public boolean sendFileInfoToClient(FileInfoDTO fileInfoDTO) {
        FileInfoReceived fileInfoReceived = fileInfoReceivedConverter.convertToEntity(fileInfoDTO);
        fileInfoReceived.setClient(clientInfo);
        fileInfoReceivedRepository.save(fileInfoReceived);
        logger.log(fileInfoDTO.toString());
        return true;
    }

    @Override
    public FilePartDTO getFirstNotSentFilePartFromClient(FileInfoDTO fileInfoDTO) throws RemoteException {
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

    @Override
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
            logger.log(filePart.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // calls here
    public boolean loginToServer() {
        setClientStatus(ClientStatus.CLIENT_FIRST);
        if (!isLoggedIn()) {
            String login = null;
            try {
                login = server.loginToServer(this);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (login == null) {
                logger.log("Log in failed!");
                return false;
            }
            clientInfo.setLogin(login);
            clientInfo.setStatus(ClientStatus.CLIENT_SECOND);
            clientInfoDTO.setLogin(login);
            clientInfoDTO.setStatus(ClientStatus.CLIENT_SECOND);
            clientInfoRepository.save(clientInfo);
            logger.log("Log in success!");
        } else {
            try {
                server.loginToServer(this);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    // calls here
    public boolean sendTextMessageToServer(String message) {
        setClientStatus(ClientStatus.CLIENT_WORK);
        if (isLoggedIn()) {
            String answer = null;
            try {
                answer = server.sendAndReceiveTextMessageFromServer(clientInfo.getLogin(), message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            logger.log(answer);
            return true;
        } else {
            logger.log("You are not logged in!");
            return false;
        }
    }

    // this is cycle for sending file parts from client to server, it calls here
    public boolean sendFileToServerFast(String filename) {
        if (filename.charAt(0) == '.') {
            logger.log("Can't send hidden file");
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
                server.sendFileInfoToServer(clientInfoDTO.getLogin(), fileInfoDTO);
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
                    logger.log(String.valueOf(bytesCount));
                    FilePartDTO filePartDTO = new FilePartDTO();
                    filePartDTO.setOrder(step);
                    step++;
                    filePartDTO.setHashKey((long) filePartDTO.hashCode());
                    filePartDTO.setFileInfoDTO(fileInfoDTO);
                    filePartDTO.setData(fileData);
                    filePartDTO.setLength(bytesCount);
                    filePartDTO.setStatus(FilePartStatus.NOT_SENT);
                    filePartDTO.setClient(clientInfoDTO);
                    boolean result = server.sendFilePartToServer(clientInfoDTO.getLogin(), filePartDTO);
                    logger.log(String.valueOf(result));
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
            logger.log("You are not logged in!");
            return false;
        }

        return true;
    }

    // calls here
    public boolean sendAllFilesToServer() {
        setClientStatus(ClientStatus.CLIENT_WORK);
        if (!isLoggedIn()) {
            logger.log("You are not logged in");
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

    // calls here
    public boolean sendFileToServer(String filename) {
        if (filename.charAt(0) == '.') {
            logger.log("Can't send hidden file");
            return true;
        }
        setClientStatus(ClientStatus.CLIENT_WORK);
        if (!isLoggedIn()) {
            logger.log("You are not logged in");
            return false;
        }
        File file = new File(FILE_OUTPUT_DIRECTORY + filename);
        String fileHash = getFileHash(FILE_OUTPUT_DIRECTORY + filename);
        if (!file.exists()) {
            logger.log("File " + filename + " not exists");
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
                server.sendFileInfoToServer(clientInfo.getLogin(), fileInfoDTO);
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
        } catch (IOException e) {
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
                return sendAllFilePartsToServer(filePartHashMap, fileInfoDTO);
            case TRANSFER_PROCESS:
                return sendAllFilePartsToServer(filePartHashMap, fileInfoDTO);
            case TRANSFERRED:
                return true;
        }
        logger.log("File with hash: " + fileInfoDTO.getHash() + " sent");
        return true;
    }

    // calls here
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
            firstNotSentFilePartDTOFromClient =
                    server.getFirstNotSentFilePartFromServer(clientInfo.getLogin(), fileInfoDTO);
        } catch (RemoteException e) {
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

    // calls here
    private boolean sendFilePartToServer(FilePartDTO filePartDTO) {
        boolean result = false;
        try {
            result = server.sendFilePartToServer(clientInfo.getLogin(), filePartDTO);
            //Thread.sleep(2000);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        logger.log(String.valueOf(result));
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

    private boolean isLoggedIn() {
        return clientInfo.getLogin() != null;
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
