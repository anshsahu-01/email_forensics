package com.emailForemsic.emailForensic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReceivedHeaderInfo {
    private String rawValue;
    private String fromHost;
    private String fromIp;
    private String byHost;
    private String byIp;
    private Instant timestamp;
}
