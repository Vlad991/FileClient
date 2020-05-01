package com.filesynch.repository;

import com.filesynch.dto.ClientStatus;
import com.filesynch.entity.ClientInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientInfoRepository extends JpaRepository<ClientInfo, Long> {
    ClientInfo findByLogin(String login);
    ClientInfo findFirstByIdGreaterThan(Long id);
    @Modifying
    @Query("UPDATE ClientInfo c SET c.status = :status WHERE c.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") ClientStatus status);
}
