package com.filesynch.repository;

import com.filesynch.entity.FileInfoReceived;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileInfoReceivedRepository extends JpaRepository<FileInfoReceived, Long> {
    FileInfoReceived findByHashAndName(String hash, String name);
}
