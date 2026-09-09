package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.AiAnalysisRequest;
import com.emailForemsic.emailForensic.dto.AiAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class AiServiceClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String aiServiceUrl;

    public AiServiceClient(
            @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl,
            ObjectMapper objectMapper) {

        this.aiServiceUrl = aiServiceUrl;
        this.objectMapper = objectMapper;

        // Force HTTP/1.1 for communication with FastAPI/Uvicorn
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public AiAnalysisResponse analyze(AiAnalysisRequest request) {

        try {

            // Convert Java DTO to JSON
            String json = objectMapper.writeValueAsString(request);

            System.out.println("========================================");
            System.out.println("AI REQUEST JSON");
            System.out.println(json);
            System.out.println("========================================");

            // Build direct HTTP POST request
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(aiServiceUrl + "/api/v1/analyze"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            System.out.println("AI REQUEST URL:");
            System.out.println(aiServiceUrl + "/api/v1/analyze");

            System.out.println("AI REQUEST BODY LENGTH: " + json.length());
            System.out.println("AI REQUEST BODY BYTES: " +
                    json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);

            // Send request
            HttpResponse<String> httpResponse =
                    httpClient.send(
                            httpRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            System.out.println("========================================");
            System.out.println("AI SERVICE HTTP STATUS");
            System.out.println(httpResponse.statusCode());
            System.out.println("========================================");

            System.out.println("========================================");
            System.out.println("AI SERVICE RAW RESPONSE");
            System.out.println(httpResponse.body());
            System.out.println("========================================");

            // Handle HTTP errors
            if (httpResponse.statusCode() < 200 ||
                    httpResponse.statusCode() >= 300) {

                throw new RuntimeException(
                        "AI service returned HTTP " +
                                httpResponse.statusCode() +
                                ": " +
                                httpResponse.body()
                );
            }

            // Convert Python JSON response into Java DTO
            AiAnalysisResponse response =
                    objectMapper.readValue(
                            httpResponse.body(),
                            AiAnalysisResponse.class
                    );

            System.out.println("========================================");
            System.out.println("AI ANALYSIS RESPONSE");
            System.out.println("Threat Score: " + response.getThreatScore());
            System.out.println("Risk Level: " + response.getRiskLevel());
            System.out.println("Verdict: " + response.getVerdict());
            System.out.println("Confidence: " + response.getConfidence());
            System.out.println("Summary: " + response.getSummary());
            System.out.println("Reasoning: " + response.getReasoning());
            System.out.println("========================================");

            return response;

        } catch (Exception e) {

            System.out.println("========================================");
            System.out.println("AI SERVICE ERROR");
            System.out.println(e.getMessage());
            System.out.println("========================================");

            e.printStackTrace();

            throw new RuntimeException(
                    "Failed to communicate with AI service: "
                            + e.getMessage(),
                    e
            );
        }
    }
}