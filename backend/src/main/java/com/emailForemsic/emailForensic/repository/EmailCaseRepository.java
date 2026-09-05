package com.emailForemsic.emailForensic.repository;

import com.emailForemsic.emailForensic.entity.EmailCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailCaseRepository extends JpaRepository<EmailCase, Long> {
}