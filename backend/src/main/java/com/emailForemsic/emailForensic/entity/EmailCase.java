package com.emailForemsic.emailForensic.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "email_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileHash;
    private String analysisStatus;
    private Integer threatScore;

    @Column(columnDefinition = "TEXT")
    private String rawBody;

    private LocalDateTime createdAt;

    @JsonManagedReference
    @OneToOne(mappedBy = "emailCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private EmailHeader header;

    @JsonManagedReference
    @Builder.Default
    @OneToMany(mappedBy = "emailCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmailIndicator> indicators = new ArrayList<>();

    public void setHeader(EmailHeader header) {
        this.header = header;
        if (header != null) {
            header.setEmailCase(this);
        }
    }

    public void addIndicator(EmailIndicator indicator) {
        if (this.indicators == null) {
            this.indicators = new ArrayList<>();
        }
        this.indicators.add(indicator);
        indicator.setEmailCase(this);
    }
}