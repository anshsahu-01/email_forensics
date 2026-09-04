package com.emailForemsic.emailForensic;

import com.emailForemsic.emailForensic.dto.VirusTotalReputationResult;
import com.emailForemsic.emailForensic.service.VirusTotalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VirusTotalServiceTest {

    private RestTemplate restTemplate;
    private VirusTotalService virusTotalService;

    @BeforeEach
    void setUp() throws Exception {
        virusTotalService = new VirusTotalService(new RestTemplateBuilder());
        restTemplate = mock(RestTemplate.class);
        setField(virusTotalService, "restTemplate", restTemplate);
        setField(virusTotalService, "apiKey", "test-key");
    }

    @Test
    void classifiesMaliciousResponse() {
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(response("{\"data\":{\"attributes\":{\"last_analysis_stats\":{\"malicious\":2,\"suspicious\":0,\"harmless\":4,\"undetected\":10}}}}"));

        VirusTotalReputationResult result = virusTotalService.checkUrl("https://example.com");

        assertEquals("MALICIOUS", result.getStatus());
        assertEquals(2, result.getMalicious());
    }

    @Test
    void classifiesSuspiciousResponse() {
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(response("{\"data\":{\"attributes\":{\"last_analysis_stats\":{\"malicious\":0,\"suspicious\":1,\"harmless\":4,\"undetected\":10}}}}"));

        assertEquals("SUSPICIOUS", virusTotalService.checkUrl("https://example.com").getStatus());
    }

    @Test
    void classifiesCleanResponse() {
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(response("{\"data\":{\"attributes\":{\"last_analysis_stats\":{\"malicious\":0,\"suspicious\":0,\"harmless\":4,\"undetected\":10}}}}"));

        assertEquals("CLEAN", virusTotalService.checkUrl("https://example.com").getStatus());
    }

    @Test
    void returnsUnknownForMissingKeyAndInconclusiveResponse() throws Exception {
        setField(virusTotalService, "apiKey", "");
        assertEquals("UNKNOWN", virusTotalService.checkUrl("https://example.com").getStatus());

        setField(virusTotalService, "apiKey", "test-key");
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(response("{\"data\":{\"attributes\":{\"last_analysis_stats\":{}}}}"));
        assertEquals("UNKNOWN", virusTotalService.checkUrl("https://example.com").getStatus());

        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), eq(String.class)))
            .thenReturn(response("{\"data\":{\"attributes\":{\"last_analysis_stats\":{\"undetected\":10}}}}"));
        assertEquals("UNKNOWN", virusTotalService.checkUrl("https://example.com").getStatus());
    }

    @Test
    void returnsErrorForHttpAndConnectionFailures() {
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND))
                .thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS))
                .thenThrow(new RestClientException("connection failure"));

        assertEquals("ERROR", virusTotalService.checkUrl("https://example.com").getStatus());
        assertEquals("ERROR", virusTotalService.checkUrl("https://example.com").getStatus());
        assertEquals("ERROR", virusTotalService.checkUrl("https://example.com").getStatus());
        assertEquals("ERROR", virusTotalService.checkUrl("https://example.com").getStatus());
    }

    @Test
    void returnsErrorForMalformedResponse() {
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(response("not-json"));

        assertEquals("ERROR", virusTotalService.checkUrl("https://example.com").getStatus());
    }

    @Test
    void usesUrlSafeBase64Identifier() {
        when(restTemplate.exchange(anyString(), eq(org.springframework.http.HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(response("{}"));

        virusTotalService.checkUrl("https://example.com/path?x=1");

        verify(restTemplate).exchange(contains("aHR0cHM6Ly9leGFtcGxlLmNvbS9wYXRoP3g9MQ"), any(), any(), eq(String.class));
    }

    private ResponseEntity<String> response(String body) {
        return ResponseEntity.ok(body);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
