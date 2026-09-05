package com.emailForemsic.emailForensic;

import com.emailForemsic.emailForensic.dto.AbuseIpDbResult;
import com.emailForemsic.emailForensic.dto.AsnResult;
import com.emailForemsic.emailForensic.dto.EmailParsedResult;
import com.emailForemsic.emailForensic.dto.GeoLocationResult;
import com.emailForemsic.emailForensic.dto.ReceivedHeaderInfo;
import com.emailForemsic.emailForensic.dto.VirusTotalReputationResult;
import com.emailForemsic.emailForensic.entity.EmailCase;
import com.emailForemsic.emailForensic.entity.EmailIndicator;
import com.emailForemsic.emailForensic.repository.EmailCaseRepository;
import com.emailForemsic.emailForensic.service.AbuseIpDbService;
import com.emailForemsic.emailForensic.service.AsnService;
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
    private AsnService asnService;
    private VirusTotalService virusTotalService;
    private AbuseIpDbService abuseIpDbService;
    private EmailCaseRepository caseRepository;
    private EmailCaseService emailCaseService;

    @BeforeEach
    void setUp() throws Exception {
        parserService = mock(EmailParserService.class);
        geoLocationService = mock(GeoLocationService.class);
        asnService = mock(AsnService.class);
        virusTotalService = mock(VirusTotalService.class);
        abuseIpDbService = mock(AbuseIpDbService.class);
        caseRepository = mock(EmailCaseRepository.class);
        emailCaseService = new EmailCaseService();

        setField(emailCaseService, "parserService", parserService);
        setField(emailCaseService, "geoLocationService", geoLocationService);
        setField(emailCaseService, "asnService", asnService);
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
        when(geoLocationService.lookup(anyString())).thenReturn(GeoLocationResult.builder().build());
        when(asnService.lookup(anyString())).thenReturn(AsnResult.builder().build());
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
        when(geoLocationService.lookup(anyString())).thenReturn(GeoLocationResult.builder().build());
        when(asnService.lookup(anyString())).thenReturn(AsnResult.builder().build());
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
        when(geoLocationService.lookup(anyString())).thenReturn(GeoLocationResult.builder().build());
        when(asnService.lookup(anyString())).thenReturn(AsnResult.builder().build());
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
        when(geoLocationService.lookup(anyString())).thenReturn(GeoLocationResult.builder().build());
        when(asnService.lookup(anyString())).thenReturn(AsnResult.builder().build());
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

    // -----------------------------------------------------------------------
    // Sender Spoofing Risk Tests
    // -----------------------------------------------------------------------

    private EmailParsedResult buildSpoofingParsedResult(String from, String replyTo, String returnPath, String dmarc, String spf, String dkim) {
        return EmailParsedResult.builder()
                .senderFrom(from)
                .replyTo(replyTo)
                .returnPath(returnPath)
                .dmarcStatus(dmarc)
                .spfStatus(spf)
                .dkimStatus(dkim)
                .originatingIp(null)
                .build();
    }

    @Test
    void spoofing_matchingFromAndReplyTo_noMismatch() throws Exception {
        EmailParsedResult parsed = buildSpoofingParsedResult("sender@example.com", "sender@example.com", "sender@example.com", "pass", "pass", "pass");
        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsed);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());
        assertEquals("LOW", savedCase.getSpoofingRisk());
        assertEquals("[]", savedCase.getSpoofingFindings());
    }

    @Test
    void spoofing_fromReplyToMismatch_dmarcFail() throws Exception {
        EmailParsedResult parsed = buildSpoofingParsedResult("sender@example.com", "attacker@other.com", "sender@example.com", "fail", "pass", "pass");
        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsed);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());
        assertEquals("HIGH", savedCase.getSpoofingRisk());
        assertTrue(savedCase.getSpoofingFindings().contains("FROM_REPLY_TO_MISMATCH"));
    }

    @Test
    void spoofing_fromReturnPathMismatch_dmarcFail() throws Exception {
        EmailParsedResult parsed = buildSpoofingParsedResult("sender@example.com", "sender@example.com", "attacker@other.com", "fail", "pass", "pass");
        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsed);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());
        assertEquals("HIGH", savedCase.getSpoofingRisk());
        assertTrue(savedCase.getSpoofingFindings().contains("FROM_RETURN_PATH_MISMATCH"));
    }

    @Test
    void spoofing_bothMismatches() throws Exception {
        EmailParsedResult parsed = buildSpoofingParsedResult("sender@example.com", "attacker1@other.com", "attacker2@other.com", "pass", "pass", "pass");
        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsed);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());
        assertEquals("HIGH", savedCase.getSpoofingRisk()); // both mismatches = HIGH regardless of DMARC
        assertTrue(savedCase.getSpoofingFindings().contains("FROM_REPLY_TO_MISMATCH"));
        assertTrue(savedCase.getSpoofingFindings().contains("FROM_RETURN_PATH_MISMATCH"));
    }

    @Test
    void spoofing_missingReplyTo_noMismatch() throws Exception {
        EmailParsedResult parsed = buildSpoofingParsedResult("sender@example.com", null, "sender@example.com", "pass", "pass", "pass");
        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsed);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());
        assertEquals("LOW", savedCase.getSpoofingRisk());
        assertEquals("[]", savedCase.getSpoofingFindings());
    }

    @Test
    void spoofing_missingReturnPath_noMismatch() throws Exception {
        EmailParsedResult parsed = buildSpoofingParsedResult("sender@example.com", "sender@example.com", null, "pass", "pass", "pass");
        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsed);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());
        assertEquals("LOW", savedCase.getSpoofingRisk());
        assertEquals("[]", savedCase.getSpoofingFindings());
    }

    @Test
    void spoofing_malformedSenderAddress() throws Exception {
        EmailParsedResult parsed = buildSpoofingParsedResult("invalid-email", null, null, "pass", "pass", "pass");
        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsed);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());
        assertEquals("UNKNOWN", savedCase.getSpoofingRisk());
        assertEquals("[]", savedCase.getSpoofingFindings());
    }

    @Test
    void spoofing_dmarcPassOneMismatch_isMedium() throws Exception {
        EmailParsedResult parsed = buildSpoofingParsedResult("sender@example.com", "attacker@other.com", "sender@example.com", "pass", "pass", "pass");
        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsed);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());
        assertEquals("MEDIUM", savedCase.getSpoofingRisk());
        assertTrue(savedCase.getSpoofingFindings().contains("FROM_REPLY_TO_MISMATCH"));
    }

    @Test
    void spoofing_dmarcUnknownOneMismatch_isHigh() throws Exception {
        EmailParsedResult parsed = buildSpoofingParsedResult("sender@example.com", "attacker@other.com", "sender@example.com", "unknown", "pass", "pass");
        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsed);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());
        assertEquals("HIGH", savedCase.getSpoofingRisk());
    }

    @Test
    void spoofing_noMismatchNormalAuth_isLow() throws Exception {
        EmailParsedResult parsed = buildSpoofingParsedResult("sender@example.com", "sender@example.com", "sender@example.com", "pass", "pass", "pass");
        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsed);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());
        assertEquals("LOW", savedCase.getSpoofingRisk());
    }

    @Test
    void spoofing_insufficientData_isUnknown() throws Exception {
        EmailParsedResult parsed = buildSpoofingParsedResult(null, null, null, "unknown", "unknown", "unknown");
        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsed);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());
        assertEquals("UNKNOWN", savedCase.getSpoofingRisk());
    }

    @Test
    void spoofing_existingEmailAnalysisPersistsSuccessfully() throws Exception {
        EmailParsedResult parsed = buildSpoofingParsedResult("PayPal Support <support@paypal.com>", "support@paypal.com, feedback@paypal.com", "bounces@paypal.com", "pass", "pass", "pass");
        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsed);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());
        assertEquals("LOW", savedCase.getSpoofingRisk());
        assertEquals("[]", savedCase.getSpoofingFindings());
    }

    // -----------------------------------------------------------------------
    // Geolocation persistence and failure tests
    // -----------------------------------------------------------------------

    @Test
    void persistsGeoFieldsIntoEmailCaseWhenLookupSucceeds() throws Exception {
        EmailParsedResult parsedResult = buildParsedResult("8.8.8.8", List.of());

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(geoLocationService.lookup("8.8.8.8")).thenReturn(GeoLocationResult.builder()
                .country("United States")
                .city("Mountain View")
                .latitude(37.386)
                .longitude(-122.0838)
                .timezone("America/Los_Angeles")
                .build());
        when(abuseIpDbService.checkIp(anyString())).thenReturn(
                AbuseIpDbResult.builder().status("CLEAN").build());
        when(asnService.lookup(anyString())).thenReturn(AsnResult.builder().build());
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());

        assertEquals("United States", savedCase.getGeoCountry());
        assertEquals("Mountain View", savedCase.getGeoCity());
        assertEquals(37.386, savedCase.getGeoLatitude());
        assertEquals(-122.0838, savedCase.getGeoLongitude());
        assertEquals("America/Los_Angeles", savedCase.getGeoTimezone());
        verify(geoLocationService, times(1)).lookup("8.8.8.8");

        EmailIndicator ipIndicator = savedCase.getIndicators().stream()
                .filter(i -> "IP".equals(i.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected an IP indicator"));
        assertEquals("Mountain View, United States (America/Los_Angeles)", ipIndicator.getDetails());
    }

    @Test
    void geoFieldsAreNullWhenLookupReturnsEmptyResult() throws Exception {
        EmailParsedResult parsedResult = buildParsedResult("203.0.113.1", List.of());

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(geoLocationService.lookup(anyString())).thenReturn(GeoLocationResult.builder().build());
        when(asnService.lookup(anyString())).thenReturn(AsnResult.builder().build());
        when(abuseIpDbService.checkIp(anyString())).thenReturn(
                AbuseIpDbResult.builder().status("CLEAN").build());
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());

        assertNull(savedCase.getGeoCountry());
        assertNull(savedCase.getGeoCity());
        assertNull(savedCase.getGeoLatitude());
        assertNull(savedCase.getGeoLongitude());
        assertNull(savedCase.getGeoTimezone());
        // Case must still be saved
        verify(caseRepository, times(1)).save(any(EmailCase.class));
    }

    @Test
    void emailCaseIsSavedWhenGeoLocationLookupThrows() throws Exception {
        EmailParsedResult parsedResult = buildParsedResult("203.0.113.25", List.of());

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(geoLocationService.lookup(anyString())).thenThrow(new RuntimeException("unexpected geo failure"));
        when(asnService.lookup(anyString())).thenReturn(AsnResult.builder().build());
        when(abuseIpDbService.checkIp(anyString())).thenReturn(
                AbuseIpDbResult.builder().status("CLEAN").build());
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        // Must not throw — geo failure is non-fatal
        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());

        verify(caseRepository, times(1)).save(any(EmailCase.class));
        assertNotNull(savedCase);
        // Geo fields remain null
        assertNull(savedCase.getGeoCountry());
        assertNull(savedCase.getGeoCity());
    }

    @Test
    void noGeoLookupWhenOriginatingIpIsNull() throws Exception {
        EmailParsedResult parsedResult = buildParsedResult(null, List.of());

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        emailCaseService.processAndSaveEml(dummyFile());

        // GeoLocationService must never be called when originatingIp is null
        verify(geoLocationService, never()).lookup(any());
        verifyNoInteractions(geoLocationService);
    }

    // -----------------------------------------------------------------------
    // ASN / Network Intelligence persistence and failure tests
    // -----------------------------------------------------------------------

    @Test
    void persistsAsnFieldsIntoIpIndicatorWhenLookupSucceeds() throws Exception {
        EmailParsedResult parsedResult = buildParsedResult("8.8.8.8", List.of());

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(geoLocationService.lookup(anyString())).thenReturn(GeoLocationResult.builder().build());
        when(abuseIpDbService.checkIp(anyString())).thenReturn(
                AbuseIpDbResult.builder().status("CLEAN").build());
        when(asnService.lookup("8.8.8.8")).thenReturn(AsnResult.builder()
                .asnNumber("AS15169")
                .asnOrg("GOOGLE")
                .build());
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());

        EmailIndicator ipIndicator = savedCase.getIndicators().stream()
                .filter(i -> "IP".equals(i.getType()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected an IP indicator"));

        assertEquals("AS15169", ipIndicator.getAsnNumber());
        assertEquals("GOOGLE", ipIndicator.getAsnOrg());
        verify(asnService, times(1)).lookup("8.8.8.8");
    }

    @Test
    void asnFieldsAreNullWhenLookupReturnsEmptyResult() throws Exception {
        EmailParsedResult parsedResult = buildParsedResult("203.0.113.1", List.of());

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(geoLocationService.lookup(anyString())).thenReturn(GeoLocationResult.builder().build());
        when(abuseIpDbService.checkIp(anyString())).thenReturn(
                AbuseIpDbResult.builder().status("CLEAN").build());
        when(asnService.lookup(anyString())).thenReturn(AsnResult.builder().build());
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());

        EmailIndicator ipIndicator = savedCase.getIndicators().stream()
                .filter(i -> "IP".equals(i.getType()))
                .findFirst()
                .orElseThrow();

        assertNull(ipIndicator.getAsnNumber());
        assertNull(ipIndicator.getAsnOrg());
        // Case must still be saved
        verify(caseRepository, times(1)).save(any(EmailCase.class));
    }

    @Test
    void emailCaseIsSavedWhenAsnLookupThrows() throws Exception {
        EmailParsedResult parsedResult = buildParsedResult("203.0.113.25", List.of());

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(geoLocationService.lookup(anyString())).thenReturn(GeoLocationResult.builder().build());
        when(abuseIpDbService.checkIp(anyString())).thenReturn(
                AbuseIpDbResult.builder().status("CLEAN").build());
        when(asnService.lookup(anyString())).thenThrow(new RuntimeException("unexpected ASN failure"));
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        // Must not throw — ASN failure is non-fatal
        EmailCase savedCase = emailCaseService.processAndSaveEml(dummyFile());

        verify(caseRepository, times(1)).save(any(EmailCase.class));
        assertNotNull(savedCase);
        EmailIndicator ipIndicator = savedCase.getIndicators().stream()
                .filter(i -> "IP".equals(i.getType()))
                .findFirst()
                .orElseThrow();
        // ASN fields remain null after failure
        assertNull(ipIndicator.getAsnNumber());
        assertNull(ipIndicator.getAsnOrg());
    }

    @Test
    void noAsnLookupWhenOriginatingIpIsNull() throws Exception {
        EmailParsedResult parsedResult = buildParsedResult(null, List.of());

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(i -> i.getArgument(0));

        emailCaseService.processAndSaveEml(dummyFile());

        // AsnService must never be called when originatingIp is null
        verify(asnService, never()).lookup(any());
        verifyNoInteractions(asnService);
    }
}
