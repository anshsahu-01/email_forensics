package com.emailForemsic.emailForensic.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiAnalysisRequest {

    private String subject;

    private String bodyText;

    private List<String> urls;

    private String senderDomain;
}