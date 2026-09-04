package com.emailForemsic.emailForensic;

import com.emailForemsic.emailForensic.dto.AbuseIpDbResult;
import com.emailForemsic.emailForensic.service.AbuseIpDbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AbuseIpDbServiceTest {

    private RestTemplate restTemplate;
    private AbuseIpDbService abuseIpDbService;

    @BeforeEach
    void setUp() throws Exception {
        abuseIpDbService = new AbuseIpDbService(new RestTemplateBuilder());
        restTemplate = mock(RestTemplate.class);
        setField(abuseIpDbService, "restTemplate", restTemplate);
        setField(abuseIpDbService, "apiKey", "test-api-key");
    }

    // 1. Successful AbuseIPDB response with a high-confidence IP
    @Test
    void classifiesMaliciousForHighConfidenceScore() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ok("{\"data\":{\"abuseConfidenceScore\":90,\"totalReports\":42,\"lastReportedAt\":\"2024-01-15T12:00:00+00:00\"}}"));

        AbuseIpDbResult result = abuseIpDbService.checkIp("198.51.100.10");

        assertEquals("MALICIOUS", result.getStatus());
        assertEquals(90, result.getAbuseConfidenceScore());
        assertEquals(42, result.getTotalReports());
        assertEquals("2024-01-15T12:00:00+00:00", result.getLastReportedAt());
    }

    // 2. Zero abuse confidence / clean result
    @Test
    void classifiesCleanForZeroScore() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ok("{\"data\":{\"abuseConfidenceScore\":0,\"totalReports\":0,\"lastReportedAt\":null}}"));

        AbuseIpDbResult result = abuseIpDbService.checkIp("203.0.113.5");

        assertEquals("CLEAN", result.getStatus());
        assertEquals(0, result.getAbuseConfidenceScore());
        assertEquals(0, result.getTotalReports());
    }

    // 3. High abuse confidence (75–100) → MALICIOUS; moderate (25–74) → SUSPICIOUS
    @Test
    void classifiesSuspiciousForMidRangeScore() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ok("{\"data\":{\"abuseConfidenceScore\":50,\"totalReports\":10,\"lastReportedAt\":\"2024-06-01T00:00:00+00:00\"}}"));

        AbuseIpDbResult result = abuseIpDbService.checkIp("203.0.113.50");

        assertEquals("SUSPICIOUS", result.getStatus());
        assertEquals(50, result.getAbuseConfidenceScore());
    }

    // 4. Missing API key → UNKNOWN, no HTTP call made
    @Test
    void returnsUnknownWhenApiKeyIsMissing() throws Exception {
        setField(abuseIpDbService, "apiKey", "");

        AbuseIpDbResult result = abuseIpDbService.checkIp("203.0.113.1");

        assertEquals("UNKNOWN", result.getStatus());
        verifyNoInteractions(restTemplate);
    }

    // 4b. Null API key → UNKNOWN
    @Test
    void returnsUnknownWhenApiKeyIsNull() throws Exception {
        setField(abuseIpDbService, "apiKey", null);

        assertEquals("UNKNOWN", abuseIpDbService.checkIp("203.0.113.1").getStatus());
        verifyNoInteractions(restTemplate);
    }

    // 5. Private/local IP — service is called; callers (EmailCaseService) are responsible for
    //    not passing private IPs, but the service itself returns whatever the API gives
    //    (or UNKNOWN if the key is absent). Here we verify blank IP → UNKNOWN without HTTP call.
    @Test
    void returnsUnknownForBlankIp() {
        AbuseIpDbResult result = abuseIpDbService.checkIp("   ");
        assertEquals("UNKNOWN", result.getStatus());
        verifyNoInteractions(restTemplate);
    }

    // 6. Missing originating IP (null) → UNKNOWN, no HTTP call
    @Test
    void returnsUnknownForNullIp() {
        AbuseIpDbResult result = abuseIpDbService.checkIp(null);
        assertEquals("UNKNOWN", result.getStatus());
        verifyNoInteractions(restTemplate);
    }

    // 7. HTTP 401 → ERROR
    @Test
    void returnsErrorForHttp401() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertEquals("ERROR", abuseIpDbService.checkIp("203.0.113.1").getStatus());
    }

    // 8. HTTP 403 → ERROR
    @Test
    void returnsErrorForHttp403() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));

        assertEquals("ERROR", abuseIpDbService.checkIp("203.0.113.1").getStatus());
    }

    // 9. HTTP 429 → ERROR
    @Test
    void returnsErrorForHttp429() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

        assertEquals("ERROR", abuseIpDbService.checkIp("203.0.113.1").getStatus());
    }

    // 10. Connection failure → ERROR
    @Test
    void returnsErrorForConnectionFailure() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenThrow(new RestClientException("connection refused"));

        assertEquals("ERROR", abuseIpDbService.checkIp("203.0.113.1").getStatus());
    }

    // 11. Malformed API response → ERROR
    @Test
    void returnsErrorForMalformedJson() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ok("not-json-at-all"));

        assertEquals("ERROR", abuseIpDbService.checkIp("203.0.113.1").getStatus());
    }

    // 11b. Missing 'data' node → UNKNOWN
    @Test
    void returnsUnknownWhenDataNodeAbsent() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ok("{\"errors\":[]}"));

        assertEquals("UNKNOWN", abuseIpDbService.checkIp("203.0.113.1").getStatus());
    }

    // 11c. Empty body → UNKNOWN
    @Test
    void returnsUnknownForEmptyBody() {
        when(restTemplate.exchange(anyString(), any(), any(), eq(String.class)))
                .thenReturn(ok(""));

        assertEquals("UNKNOWN", abuseIpDbService.checkIp("203.0.113.1").getStatus());
    }

    private ResponseEntity<String> ok(String body) {
        return ResponseEntity.ok(body);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
