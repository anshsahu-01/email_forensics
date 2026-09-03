package com.emailForemsic.emailForensic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailParsedResult {
    private String subject;
    private String senderFrom;
    private String replyTo;
    private String to;
    private String cc;
    private Instant date;
    private String messageId;
    private String returnPath;
    private String spfStatus;
    private String dkimStatus;
    private String dmarcStatus;
    private String rawBody;
    private List<ReceivedHeaderInfo> receivedHeaders;
    private List<String> extractedUrls;
    private String originatingIp;
}