package com.emailForemsic.emailForensic.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

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
    private String subject;

    private String spfStatus;   // PASS, FAIL, NONE
    private String dkimStatus;  // PASS, FAIL, NONE
    private String dmarcStatus; // PASS, FAIL, NONE

    @JsonBackReference
    @OneToOne
    @JoinColumn(name = "case_id", nullable = false)
    private EmailCase emailCase;
}