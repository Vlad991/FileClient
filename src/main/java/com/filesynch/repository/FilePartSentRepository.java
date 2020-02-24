package com.filesynch.repository;

import com.filesynch.dto.FilePartStatus;
import com.filesynch.entity.FileInfoSent;
import com.filesynch.entity.FilePartSent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilePartSentRepository extends JpaRepository<FilePartSent, Long> {
    FilePartSent findByHashKey(String hashKey);
    List<FilePartSent> findAllByFileInfo(FileInfoSent fileInfoSent);
    List<FilePartSent> findAllByFileInfoAndStatus(FileInfoSent fileInfoSent, FilePartStatus filePartStatus);
    void removeAllByFileInfo(FileInfoSent fileInfoSent);
}
