package com.emailForemsic.emailForensic.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "email_indicators")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailIndicator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type;
    private String value;
    private String details;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "case_id")
    private EmailCase emailCase;
}