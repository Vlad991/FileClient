package com.filesynch.repository;

import com.filesynch.entity.ClientInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientInfoRepository extends JpaRepository<ClientInfo, Long> {
    ClientInfo findByLogin(String login);
    Optional<ClientInfo> findById(Long id);
}
