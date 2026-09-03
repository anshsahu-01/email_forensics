package com.emailForemsic.emailForensic;

import com.emailForemsic.emailForensic.dto.EmailParsedResult;
import com.emailForemsic.emailForensic.dto.ReceivedHeaderInfo;
import com.emailForemsic.emailForensic.service.EmailParserService;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EmailParserServiceTest {

    private final EmailParserService parserService = new EmailParserService();

    @Test
    void parsesBasicEmailHeaders() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("basic-email.eml")) {
            assertNotNull(inputStream);

            EmailParsedResult result = parserService.parseEml(inputStream);

            assertEquals("John Doe <john@example.com>", result.getSenderFrom());
            assertEquals("Jane Smith <jane@example.com>", result.getTo());
            assertEquals("Basic email test", result.getSubject());
            assertEquals("<basic-123@example.com>", result.getMessageId());
            assertEquals(Instant.parse("2025-04-01T12:34:56Z"), result.getDate());
        }
    }

    @Test
    void parsesCcReplyToAndReturnPathHeaders() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("cc-reply-returnpath-email.eml")) {
            assertNotNull(inputStream);

            EmailParsedResult result = parserService.parseEml(inputStream);

            assertEquals("Bob <bob@example.com>, Carol <carol@example.com>", result.getCc());
            assertEquals("Support <support@example.com>", result.getReplyTo());
            assertEquals("<bounce@example.com>", result.getReturnPath());
            assertEquals("Sender Name <sender@example.com>", result.getSenderFrom());
        }
    }

    @Test
    void decodesMimeEncodedSubject() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("mime-subject-email.eml")) {
            assertNotNull(inputStream);

            EmailParsedResult result = parserService.parseEml(inputStream);

            assertEquals("Case 1 - Translated subject", result.getSubject());
        }
    }

    @Test
    void handlesMissingOptionalHeadersGracefully() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("missing-headers-email.eml")) {
            assertNotNull(inputStream);

            EmailParsedResult result = parserService.parseEml(inputStream);

            assertEquals("Only Sender <sender@example.com>", result.getSenderFrom());
            assertNull(result.getTo());
            assertNull(result.getSubject());
            assertNull(result.getDate());
            assertNull(result.getMessageId());
            assertNull(result.getReturnPath());
        }
    }

    @Test
    void preservesMultipleRecipients() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("multi-recipient-email.eml")) {
            assertNotNull(inputStream);

            EmailParsedResult result = parserService.parseEml(inputStream);

            assertEquals(
                "First Person <first@example.com>, Second Person <second@example.com>, Third Person <third@example.com>",
                result.getTo()
            );
        }
    }

    @Test
    void parseEmlRejectsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> parserService.parseEml(null));
    }

    @Test
    void parsesAllAuthenticationResultsAsLowercase() throws Exception {
        EmailParsedResult result = parseFixture("auth-all-pass-email.eml");

        assertEquals("pass", result.getSpfStatus());
        assertEquals("pass", result.getDkimStatus());
        assertEquals("pass", result.getDmarcStatus());
    }

    @Test
    void parsesMixedAuthenticationResultsIndependently() throws Exception {
        EmailParsedResult result = parseFixture("auth-mixed-email.eml");

        assertEquals("pass", result.getSpfStatus());
        assertEquals("fail", result.getDkimStatus());
        assertEquals("fail", result.getDmarcStatus());
    }

    @Test
    void usesReceivedSpfWhenAuthenticationResultsHasNoSpf() throws Exception {
        EmailParsedResult result = parseFixture("received-spf-fallback-email.eml");

        assertEquals("pass", result.getSpfStatus());
        assertEquals("none", result.getDkimStatus());
        assertEquals("none", result.getDmarcStatus());
    }

    @Test
    void prefersAuthenticationResultsSpfOverReceivedSpf() throws Exception {
        EmailParsedResult result = parseFixture("auth-spf-priority-email.eml");

        assertEquals("pass", result.getSpfStatus());
    }

    @Test
    void doesNotTreatDkimSignatureAsVerification() throws Exception {
        EmailParsedResult result = parseFixture("dkim-signature-only-email.eml");

        assertNotEquals("pass", result.getDkimStatus());
        assertEquals("none", result.getDkimStatus());
    }

    @Test
    void defaultsMissingAuthenticationHeadersToNone() throws Exception {
        EmailParsedResult result = parseFixture("no-authentication-email.eml");

        assertEquals("none", result.getSpfStatus());
        assertEquals("none", result.getDkimStatus());
        assertEquals("none", result.getDmarcStatus());
    }

    @Test
    void selectsFirstValidAuthenticationResultPerMechanism() throws Exception {
        EmailParsedResult result = parseFixture("multiple-authentication-results-email.eml");

        assertEquals("pass", result.getSpfStatus());
        assertEquals("none", result.getDkimStatus());
        assertEquals("pass", result.getDmarcStatus());
    }

    @Test
    void acceptsMixedCaseAuthenticationResults() throws Exception {
        EmailParsedResult result = parseFixture("mixed-case-authentication-email.eml");

        assertEquals("pass", result.getSpfStatus());
        assertEquals("fail", result.getDkimStatus());
        assertEquals("pass", result.getDmarcStatus());
    }

    @Test
    void ignoresMalformedAuthenticationEntriesWithoutBreakingParsing() throws Exception {
        EmailParsedResult result = parseFixture("malformed-authentication-email.eml");

        assertEquals("none", result.getSpfStatus());
        assertEquals("none", result.getDkimStatus());
        assertEquals("pass", result.getDmarcStatus());
    }

    private EmailParsedResult parseFixture(String fixture) throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fixture)) {
            assertNotNull(inputStream);
            return parserService.parseEml(inputStream);
        }
    }

    @Test
    void parsesMultipleReceivedHeadersInOriginalOrderAndFindsOriginatingIp() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("multiple-received-email.eml")) {
            assertNotNull(inputStream);

            EmailParsedResult result = parserService.parseEml(inputStream);

            assertEquals(3, result.getReceivedHeaders().size());
            assertEquals("internal.local", result.getReceivedHeaders().get(0).getFromHost());
            assertEquals("192.168.1.20", result.getReceivedHeaders().get(0).getFromIp());
            assertEquals("relay.example.net", result.getReceivedHeaders().get(0).getByHost());
            assertEquals("198.51.100.10", result.getReceivedHeaders().get(0).getByIp());
            assertEquals("sender.example.org", result.getReceivedHeaders().get(1).getFromHost());
            assertEquals("203.0.113.25", result.getReceivedHeaders().get(1).getFromIp());
            assertEquals(Instant.parse("2026-09-03T10:19:30Z"), result.getReceivedHeaders().get(1).getTimestamp());
            assertEquals("203.0.113.25", result.getOriginatingIp());
        }
    }

    @Test
    void parsesPublicIpv6ReceivedAddress() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("ipv6-received-email.eml")) {
            assertNotNull(inputStream);

            EmailParsedResult result = parserService.parseEml(inputStream);

            assertEquals(1, result.getReceivedHeaders().size());
            ReceivedHeaderInfo received = result.getReceivedHeaders().get(0);
            assertEquals("2001:db8::25", received.getFromIp());
            assertEquals("2001:db8::25", result.getOriginatingIp());
        }
    }

    @Test
    void handlesEmailWithoutReceivedHeader() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("no-received-email.eml")) {
            assertNotNull(inputStream);

            EmailParsedResult result = parserService.parseEml(inputStream);

            assertNotNull(result.getReceivedHeaders());
            assertTrue(result.getReceivedHeaders().isEmpty());
            assertNull(result.getOriginatingIp());
        }
    }

    @Test
    void toleratesMalformedReceivedHeader() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("malformed-received-email.eml")) {
            assertNotNull(inputStream);

            EmailParsedResult result = parserService.parseEml(inputStream);

            assertEquals(1, result.getReceivedHeaders().size());
            assertNull(result.getReceivedHeaders().get(0).getFromHost());
            assertNull(result.getReceivedHeaders().get(0).getFromIp());
            assertNull(result.getReceivedHeaders().get(0).getByHost());
            assertNull(result.getReceivedHeaders().get(0).getTimestamp());
            assertNull(result.getOriginatingIp());
        }
    }

    @Test
    void ignoresPrivateOnlyReceivedChain() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("private-only-received-email.eml")) {
            assertNotNull(inputStream);

            EmailParsedResult result = parserService.parseEml(inputStream);

            assertEquals(3, result.getReceivedHeaders().size());
            assertEquals("127.0.0.1", result.getReceivedHeaders().get(0).getFromIp());
            assertEquals("10.0.0.8", result.getReceivedHeaders().get(1).getFromIp());
            assertEquals("::1", result.getReceivedHeaders().get(2).getFromIp());
            assertNull(result.getOriginatingIp());
        }
    }

}
