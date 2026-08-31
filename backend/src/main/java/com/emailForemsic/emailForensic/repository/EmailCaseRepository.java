package com.emailForemsic.emailForensic.repository;

import com.emailForemsic.emailForensic.entity.EmailCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface EmailCaseRepository extends JpaRepository<EmailCase, Long> {
    Optional<EmailCase> findByFileHash(String fileHash);
}