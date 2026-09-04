package com.emailForemsic.emailForensic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirusTotalReputationResult {
    private String status;
    private Integer malicious;
    private Integer suspicious;
    private Integer harmless;
    private Integer undetected;
}
