package com.filesynch.repository;

import com.filesynch.entity.FileInfoReceived;
import com.filesynch.entity.FilePartReceived;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilePartReceivedRepository extends JpaRepository<FilePartReceived, Long> {
    List<FilePartReceived> findAllByFileInfo(FileInfoReceived fileInfoReceived);
}
