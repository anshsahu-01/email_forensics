package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.AbuseIpDbResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Queries AbuseIPDB /api/v2/check for IP reputation.
 * <p>
 * Mirrors the VirusTotalService pattern:
 *  - key loaded from property, never hardcoded
 *  - returns UNKNOWN when key is absent
 *  - returns ERROR on any HTTP or connection failure
 *  - non-fatal: callers must never let a failure here break email persistence
 */
@Service
public class AbuseIpDbService {

    private static final String CHECK_URL = "https://api.abuseipdb.com/api/v2/check";
    private static final int MAX_AGE_DAYS = 90;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    @Value("${abuseipdb.api-key:}")
    private String apiKey;

    public AbuseIpDbService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Looks up the abuse reputation for the given public IP address.
     *
     * @param ipAddress a non-null, non-blank public IP (IPv4 or IPv6)
     * @return enrichment result; never throws
     */
    public AbuseIpDbResult checkIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return unknownResult();
        }
        if (apiKey == null || apiKey.isBlank()) {
            return unknownResult();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Key", apiKey);
        headers.set("Accept", "application/json");

        String url = CHECK_URL + "?ipAddress=" + ipAddress + "&maxAgeInDays=" + MAX_AGE_DAYS;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            return parseResponse(response.getBody());
        } catch (HttpClientErrorException e) {
            // 401, 403, 404, 429, etc. — all non-fatal, return ERROR so callers know enrichment failed
            return AbuseIpDbResult.builder().status("ERROR").build();
        } catch (RestClientException e) {
            // connection failure, timeout, etc.
            return AbuseIpDbResult.builder().status("ERROR").build();
        }
    }

    private AbuseIpDbResult parseResponse(String body) {
        if (body == null || body.isBlank()) {
            return AbuseIpDbResult.builder().status("UNKNOWN").build();
        }
        try {
            JsonNode data = objectMapper.readTree(body).path("data");
            if (!data.isObject()) {
                return AbuseIpDbResult.builder().status("UNKNOWN").build();
            }

            int score = readInt(data, "abuseConfidenceScore");
            int totalReports = readInt(data, "totalReports");
            String lastReportedAt = readString(data, "lastReportedAt");

            // Classification: mirrors VirusTotal pattern — clear thresholds, distinguishable states
            String status;
            if (score >= 75) {
                status = "MALICIOUS";
            } else if (score >= 25) {
                status = "SUSPICIOUS";
            } else if (data.has("abuseConfidenceScore")) {
                // Field is present but score is 0–24 — genuinely clean/low-risk
                status = "CLEAN";
            } else {
                status = "UNKNOWN";
            }

            return AbuseIpDbResult.builder()
                    .status(status)
                    .abuseConfidenceScore(score)
                    .totalReports(totalReports)
                    .lastReportedAt(lastReportedAt)
                    .build();
        } catch (Exception e) {
            return AbuseIpDbResult.builder().status("ERROR").build();
        }
    }

    private int readInt(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && field.isIntegralNumber()) ? field.intValue() : 0;
    }

    private String readString(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : null;
    }

    private AbuseIpDbResult unknownResult() {
        return AbuseIpDbResult.builder().status("UNKNOWN").build();
    }
}
