package com.filesynch.repository;

import com.filesynch.entity.FileInfoSent;
import com.filesynch.entity.FilePartSent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilePartSentRepository extends JpaRepository<FilePartSent, Long> {
    FilePartSent findByHashKey(Long hashKey);
    List<FilePartSent> findAllByFileInfo(FileInfoSent fileInfoSent);
    void deleteAllByFileInfo_Id(Long fileInfoId);
}
