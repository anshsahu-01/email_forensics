package com.emailForemsic.emailForensic.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "email_headers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String senderFrom;
    private String replyTo;

    @Column(name = "email_to")
    private String to;

    @Column(name = "cc")
    private String cc;

    @Column(name = "sent_date")
    private Instant date;

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "return_path")
    private String returnPath;

    private String subject;

    private String spfStatus;   // PASS, FAIL, NONE
    private String dkimStatus;  // PASS, FAIL, NONE
    private String dmarcStatus; // PASS, FAIL, NONE

    @JsonBackReference
    @OneToOne
    @JoinColumn(name = "case_id", nullable = false)
    private EmailCase emailCase;
}