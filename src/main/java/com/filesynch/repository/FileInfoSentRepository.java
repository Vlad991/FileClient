package com.filesynch.repository;

import com.filesynch.entity.FileInfoSent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileInfoSentRepository extends JpaRepository<FileInfoSent, Long> {
    FileInfoSent findByHash(String hash);
}
