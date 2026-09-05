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

    private String virusTotalStatus;
    private Integer virusTotalMalicious;
    private Integer virusTotalSuspicious;
    private Integer virusTotalHarmless;
    private Integer virusTotalUndetected;

    // AbuseIPDB enrichment — populated for IP indicators only
    private String abuseIpDbStatus;          // MALICIOUS, SUSPICIOUS, CLEAN, UNKNOWN, ERROR
    private Integer abuseConfidenceScore;    // 0–100 as returned by AbuseIPDB
    private Integer totalReports;            // total abuse reports on record
    private String lastReportedAt;           // ISO-8601 string from AbuseIPDB ("lastReportedAt")

    // ASN / Network Intelligence — populated from MaxMind GeoLite2-ASN lookup
    private String asnNumber;               // e.g. "AS15169" — prefixed string form of the AS number
    private String asnOrg;                  // e.g. "GOOGLE" — organization registered to the AS

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "case_id")
    private EmailCase emailCase;
}