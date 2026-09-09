package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.AiAnalysisRequest;
import com.emailForemsic.emailForensic.dto.AiAnalysisResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiServiceClient {

    private final RestClient restClient;

    public AiServiceClient(
            @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl) {

        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
                .build();
    }

    public AiAnalysisResponse analyze(AiAnalysisRequest request) {

        return restClient.post()
                .uri("/api/v1/analyze")
                .body(request)
                .retrieve()
                .body(AiAnalysisResponse.class);
    }
}