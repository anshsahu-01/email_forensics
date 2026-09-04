package com.emailForemsic.emailForensic;

import com.emailForemsic.emailForensic.dto.AbuseIpDbResult;
import com.emailForemsic.emailForensic.dto.EmailParsedResult;
import com.emailForemsic.emailForensic.dto.ReceivedHeaderInfo;
import com.emailForemsic.emailForensic.dto.VirusTotalReputationResult;
import com.emailForemsic.emailForensic.entity.EmailCase;
import com.emailForemsic.emailForensic.entity.EmailIndicator;
import com.emailForemsic.emailForensic.repository.EmailCaseRepository;
import com.emailForemsic.emailForensic.service.AbuseIpDbService;
import com.emailForemsic.emailForensic.service.EmailCaseService;
import com.emailForemsic.emailForensic.service.EmailParserService;
import com.emailForemsic.emailForensic.service.GeoLocationService;
import com.emailForemsic.emailForensic.service.VirusTotalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EmailCaseServiceTest {

    private EmailParserService parserService;
    private GeoLocationService geoLocationService;
    private VirusTotalService virusTotalService;
    private AbuseIpDbService abuseIpDbService;
    private EmailCaseRepository caseRepository;
    private EmailCaseService emailCaseService;

    @BeforeEach
    void setUp() throws Exception {
        parserService = mock(EmailParserService.class);
        geoLocationService = mock(GeoLocationService.class);
        virusTotalService = mock(VirusTotalService.class);
        abuseIpDbService = mock(AbuseIpDbService.class);
        caseRepository = mock(EmailCaseRepository.class);
        emailCaseService = new EmailCaseService();

        setField(emailCaseService, "parserService", parserService);
        setField(emailCaseService, "geoLocationService", geoLocationService);
        setField(emailCaseService, "virusTotalService", virusTotalService);
        setField(emailCaseService, "abuseIpDbService", abuseIpDbService);
        setField(emailCaseService, "caseRepository", caseRepository);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    // -----------------------------------------------------------------------
    // Helper: build a fully populated parsed result with one originating IP
    // -----------------------------------------------------------------------
    private EmailParsedResult buildParsedResult(String originatingIp, List<String> urls) {
        return EmailParsedResult.builder()
                .subject("Basic email test")
                .senderFrom("John Doe <john@example.com>")
                .replyTo("Support <support@example.com>")
                .to("Jane Smith <jane@example.com>")
                .cc("Bob <bob@example.com>, Carol <carol@example.com>")
                .date(Instant.parse("2025-04-01T12:34:56Z"))
                .messageId("<basic-123@example.com>")
                .returnPath("<bounce@example.com>")
                .spfStatus("pass")
                .dkimStatus("fail")
                .dmarcStatus("none")
                .receivedHeaders(List.of(ReceivedHeaderInfo.builder()
                        .rawValue("from sender.example.org [203.0.113.25] by mx.example.net")
                        .fromHost("sender.example.org")
                        .fromIp("203.0.113.25")
                        .byHost("mx.example.net")
                        .build()))
                .originatingIp(originatingIp)
                .extractedUrls(urls)
                .build();
    }

    private MultipartFile dummyFile() {
        return new MockMultipartFile("file", "test.eml", "message/rfc822", "content".getBytes());
    }

    // -----------------------------------------------------------------------
    // Existing test — kept intact (test #13: VirusTotal still works)
    // -----------------------------------------------------------------------
    @Test
    void copiesBasicParsedHeadersIntoEmailHeaderBeforeSaving() throws Exception {
        EmailParsedResult parsedResult = buildParsedResult(
                "203.0.113.25",
                List.of("https://example.com/path", "http://192.168.1.10/test"));

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(virusTotalService.checkUrl(any(String.class))).thenReturn(VirusTotalReputationResult.builder()
                .status("CLEAN").malicious(0).suspicious(0).harmless(5).undetected(2).build());
        when(abuseIpDbService.checkIp(anyString())).thenReturn(
                AbuseIpDbResult.builder().status("CLEAN").abuseConfidenceScore(0).totalReports(0).build());
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());

        assertNotNull(savedCase.getHeader());
        assertEquals(parsedResult.getSubject(), savedCase.getHeader().getSubject());
        assertEquals(parsedResult.getSenderFrom(), savedCase.getHeader().getSenderFrom());
        assertEquals(parsedResult.getReplyTo(), savedCase.getHeader().getReplyTo());
        assertEquals(parsedResult.getTo(), savedCase.getHeader().getTo());
        assertEquals(parsedResult.getCc(), savedCase.getHeader().getCc());
        assertEquals(parsedResult.getDate(), savedCase.getHeader().getDate());
        assertEquals(parsedResult.getMessageId(), savedCase.getHeader().getMessageId());
        assertEquals(parsedResult.getReturnPath(), savedCase.getHeader().getReturnPath());
        assertEquals(parsedResult.getSpfStatus(), savedCase.getHeader().getSpfStatus());
        assertEquals(parsedResult.getDkimStatus(), savedCase.getHeader().getDkimStatus());
        assertEquals(parsedResult.getDmarcStatus(), savedCase.getHeader().getDmarcStatus());
        assertEquals(parsedResult.getOriginatingIp(), savedCase.getOriginatingIp());
        assertNotNull(savedCase.getReceivedHeaders());
        assertTrue(savedCase.getReceivedHeaders().contains("203.0.113.25"));

        // Three indicators: 1 IP + 2 URLs
        assertEquals(3, savedCase.getIndicators().size());
        assertTrue(savedCase.getIndicators().stream().anyMatch(i ->
                "URL".equals(i.getType()) && "https://example.com/path".equals(i.getValue())));
        assertTrue(savedCase.getIndicators().stream().anyMatch(i ->
                "URL".equals(i.getType()) && "http://192.168.1.10/test".equals(i.getValue())));
        assertTrue(savedCase.getIndicators().stream().filter(i -> "URL".equals(i.getType()))
                .allMatch(i -> "CLEAN".equals(i.getVirusTotalStatus())));
        verify(virusTotalService, times(2)).checkUrl(any(String.class));

        ArgumentCaptor<EmailCase> captor = ArgumentCaptor.forClass(EmailCase.class);
        verify(caseRepository).save(captor.capture());
        assertEquals(parsedResult.getTo(), captor.getValue().getHeader().getTo());
    }

    // -----------------------------------------------------------------------
    // Test #12: AbuseIPDB fields are persisted into the existing IP indicator
    // -----------------------------------------------------------------------
    @Test
    void persistsAbuseIpDbFieldsIntoIpIndicator() throws Exception {
        EmailParsedResult parsedResult = buildParsedResult("203.0.113.25", List.of());

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(abuseIpDbService.checkIp("203.0.113.25")).thenReturn(AbuseIpDbResult.builder()
                .status("MALICIOUS")
                .abuseConfidenceScore(90)
                .totalReports(42)
                .lastReportedAt("2024-01-15T12:00:00+00:00")
                .build());
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());

        EmailIndicator ipIndicator = savedCase.getIndicators().stream()
                .filter(i -> "IP".equals(i.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected an IP indicator"));

        assertEquals("203.0.113.25", ipIndicator.getValue());
        assertEquals("MALICIOUS", ipIndicator.getAbuseIpDbStatus());
        assertEquals(90, ipIndicator.getAbuseConfidenceScore());
        assertEquals(42, ipIndicator.getTotalReports());
        assertEquals("2024-01-15T12:00:00+00:00", ipIndicator.getLastReportedAt());
        verify(abuseIpDbService, times(1)).checkIp("203.0.113.25");
    }

    // -----------------------------------------------------------------------
    // AbuseIPDB failure is NON-FATAL — email case is still saved
    // -----------------------------------------------------------------------
    @Test
    void emailCaseIsSavedWhenAbuseIpDbThrows() throws Exception {
        EmailParsedResult parsedResult = buildParsedResult("203.0.113.25", List.of());

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(abuseIpDbService.checkIp(anyString())).thenThrow(new RuntimeException("timeout"));
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());

        // Case must be saved despite the exception
        verify(caseRepository, times(1)).save(any(EmailCase.class));
        assertNotNull(savedCase);
        // IP indicator gets ERROR status
        EmailIndicator ipIndicator = savedCase.getIndicators().stream()
                .filter(i -> "IP".equals(i.getType()))
                .findFirst()
                .orElseThrow();
        assertEquals("ERROR", ipIndicator.getAbuseIpDbStatus());
    }

    // -----------------------------------------------------------------------
    // No IP indicator (and no AbuseIPDB call) when originatingIp is null
    // -----------------------------------------------------------------------
    @Test
    void noAbuseIpDbCallWhenOriginatingIpIsNull() throws Exception {
        EmailParsedResult parsedResult = buildParsedResult(null, List.of());

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());

        verifyNoInteractions(abuseIpDbService);
        assertTrue(savedCase.getIndicators().stream().noneMatch(i -> "IP".equals(i.getType())));
    }

    // -----------------------------------------------------------------------
    // AbuseIPDB UNKNOWN result (e.g., missing key) still persists gracefully
    // -----------------------------------------------------------------------
    @Test
    void persistsUnknownStatusWhenAbuseIpDbReturnsUnknown() throws Exception {
        EmailParsedResult parsedResult = buildParsedResult("203.0.113.25", List.of());

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(abuseIpDbService.checkIp(anyString())).thenReturn(
                AbuseIpDbResult.builder().status("UNKNOWN").build());
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());

        EmailIndicator ipIndicator = savedCase.getIndicators().stream()
                .filter(i -> "IP".equals(i.getType()))
                .findFirst()
                .orElseThrow();
        assertEquals("UNKNOWN", ipIndicator.getAbuseIpDbStatus());
        assertNull(ipIndicator.getAbuseConfidenceScore());
    }
}
