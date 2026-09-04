package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.VirusTotalReputationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
public class VirusTotalService {

    private static final String URL_LOOKUP = "https://www.virustotal.com/api/v3/urls/";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    @Value("${virustotal.api-key:}")
    private String apiKey;

    public VirusTotalService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    public VirusTotalReputationResult checkUrl(String normalizedUrl) {
        if (normalizedUrl == null || normalizedUrl.isBlank() || apiKey == null || apiKey.isBlank()) {
            return unknownResult();
        }

        String identifier = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(normalizedUrl.getBytes(StandardCharsets.UTF_8));
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apikey", apiKey);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    URL_LOOKUP + identifier,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            return parseResponse(response.getBody());
        } catch (RestClientException e) {
            return VirusTotalReputationResult.builder().status("ERROR").build();
        }
    }

    private VirusTotalReputationResult parseResponse(String body) {
        if (body == null || body.isBlank()) {
            return VirusTotalReputationResult.builder().status("UNKNOWN").build();
        }
        try {
            JsonNode stats = objectMapper.readTree(body)
                    .path("data")
                    .path("attributes")
                    .path("last_analysis_stats");
            if (!stats.isObject()) {
                return VirusTotalReputationResult.builder().status("UNKNOWN").build();
            }

            int malicious = readCount(stats, "malicious");
            int suspicious = readCount(stats, "suspicious");
            int harmless = readCount(stats, "harmless");
            int undetected = readCount(stats, "undetected");
            String status = malicious > 0 ? "MALICIOUS"
                    : suspicious > 0 ? "SUSPICIOUS"
                    : harmless > 0 ? "CLEAN"
                    : "UNKNOWN";
            return VirusTotalReputationResult.builder()
                    .status(status)
                    .malicious(malicious)
                    .suspicious(suspicious)
                    .harmless(harmless)
                    .undetected(undetected)
                    .build();
        } catch (Exception e) {
            return VirusTotalReputationResult.builder().status("ERROR").build();
        }
    }

    private int readCount(JsonNode stats, String name) {
        JsonNode count = stats.get(name);
        return count != null && count.isIntegralNumber() ? count.intValue() : 0;
    }

    private VirusTotalReputationResult unknownResult() {
        return VirusTotalReputationResult.builder().status("UNKNOWN").build();
    }
}
