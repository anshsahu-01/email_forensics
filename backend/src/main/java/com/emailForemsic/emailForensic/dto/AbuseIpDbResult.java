package com.emailForemsic.emailForensic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Carries the relevant fields from AbuseIPDB's /api/v2/check response.
 * status values: MALICIOUS, SUSPICIOUS, CLEAN, UNKNOWN, ERROR
 * (kept separate from VirusTotal status to not conflate the two sources).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbuseIpDbResult {
    private String status;
    private Integer abuseConfidenceScore;
    private Integer totalReports;
    private String lastReportedAt;
}
