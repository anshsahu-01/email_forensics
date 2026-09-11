package com.emailForemsic.emailForensic;

import com.emailForemsic.emailForensic.dto.EmailParsedResult;
import com.emailForemsic.emailForensic.dto.ReceivedHeaderInfo;
import com.emailForemsic.emailForensic.service.EmailParserService;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

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

            assertEquals(
                    "Bob <bob@example.com>, Carol <carol@example.com>",
                    result.getCc()
            );
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
        assertThrows(
                IllegalArgumentException.class,
                () -> parserService.parseEml(null)
        );
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

            assertEquals(
                    "internal.local",
                    result.getReceivedHeaders().get(0).getFromHost()
            );

            assertEquals(
                    "192.168.1.20",
                    result.getReceivedHeaders().get(0).getFromIp()
            );

            assertEquals(
                    "relay.example.net",
                    result.getReceivedHeaders().get(0).getByHost()
            );

            // Updated fixture uses a real public IP.
            assertEquals(
                    "8.8.8.8",
                    result.getReceivedHeaders().get(0).getByIp()
            );

            assertEquals(
                    "sender.example.org",
                    result.getReceivedHeaders().get(1).getFromHost()
            );

            // Updated fixture uses a real public IP.
            assertEquals(
                    "1.1.1.1",
                    result.getReceivedHeaders().get(1).getFromIp()
            );

            assertEquals(
                    Instant.parse("2026-09-03T10:19:30Z"),
                    result.getReceivedHeaders().get(1).getTimestamp()
            );

            // Originating IP is the oldest public IP in the Received chain.
            assertEquals(
                    "1.1.1.1",
                    result.getOriginatingIp()
            );
        }
    }

    @Test
    void parsesPublicIpv6ReceivedAddress() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("ipv6-received-email.eml")) {
            assertNotNull(inputStream);

            EmailParsedResult result = parserService.parseEml(inputStream);

            assertEquals(1, result.getReceivedHeaders().size());

            ReceivedHeaderInfo received = result.getReceivedHeaders().get(0);

            assertEquals(
                    "2001:4860:4860::8888",
                    received.getFromIp()
            );

            assertEquals(
                    "2001:4860:4860::8888",
                    result.getOriginatingIp()
            );
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

            assertEquals(
                    "127.0.0.1",
                    result.getReceivedHeaders().get(0).getFromIp()
            );

            assertEquals(
                    "10.0.0.8",
                    result.getReceivedHeaders().get(1).getFromIp()
            );

            assertEquals(
                    "::1",
                    result.getReceivedHeaders().get(2).getFromIp()
            );

            assertNull(result.getOriginatingIp());
        }
    }

    @Test
void extractsPlainTextUrlsAndPreservesUsefulComponents() throws Exception {
    EmailParsedResult result = parseFixture("plain-url-email.eml");

    assertEquals(
            List.of(
                    "https://example.com/path?query=value#fragment",
                    "http://sub.example.com:8080/login",
                    "http://192.168.1.10/test"
            ),
            result.getExtractedUrls()
    );
}

@Test
void extractsHtmlAnchorAndVisibleTextUrls() throws Exception {
    EmailParsedResult result = parseFixture("html-url-email.eml");

    assertEquals(
            List.of(
                    "https://example.com/welcome",
                    "https://example.com/a%20b"
            ),
            result.getExtractedUrls()
    );
}

@Test
void deduplicatesUrlsInFirstSeenOrder() throws Exception {
    EmailParsedResult result = parseFixture("duplicate-url-email.eml");

    assertEquals(
            List.of(
                    "https://example.com/path",
                    "https://other.example/path"
            ),
            result.getExtractedUrls()
    );
}

@Test
void deduplicatesTheSameUrlAcrossMultipartTextAndHtml() throws Exception {
    EmailParsedResult result = parseFixture("multipart-url-email.eml");

    assertEquals(
            List.of(
                    "https://example.com/shared",
                    "https://html.example/path"
            ),
            result.getExtractedUrls()
    );
}

@Test
void ignoresMalformedUrlCandidates() throws Exception {
    EmailParsedResult result = parseFixture("malformed-url-email.eml");

    assertEquals(
            List.of(
                    "https://valid.example/path"
            ),
            result.getExtractedUrls()
    );
}

    @Test
    void returnsNoUrlsWhenEmailHasNoLinks() throws Exception {
        EmailParsedResult result = parseFixture("no-url-email.eml");

        assertNotNull(result.getExtractedUrls());
        assertTrue(result.getExtractedUrls().isEmpty());
    }

    @Test
    void testIsPublicIpFilters() throws Exception {

        java.lang.reflect.Method method =
                EmailParserService.class.getDeclaredMethod(
                        "isPublicIp",
                        String.class
                );

        method.setAccessible(true);

        // Loopback
        assertFalse((Boolean) method.invoke(parserService, "::1"));
        assertFalse((Boolean) method.invoke(parserService, "127.0.0.1"));

        // Private IPv4
        assertFalse((Boolean) method.invoke(parserService, "10.0.0.1"));
        assertFalse((Boolean) method.invoke(parserService, "172.16.0.1"));
        assertFalse((Boolean) method.invoke(parserService, "192.168.1.1"));

        // Unique Local IPv6 (fc00::/7)
        assertFalse((Boolean) method.invoke(parserService, "fc00::1"));
        assertFalse((Boolean) method.invoke(parserService, "fdff:ffff::1"));

        // Link Local IPv6 (fe80::/10)
        assertFalse((Boolean) method.invoke(parserService, "fe80::1"));

        // Documentation IPv6 (2001:db8::/32)
        assertFalse((Boolean) method.invoke(parserService, "2001:db8::1"));

        // IPv4-mapped IPv6 (::ffff:0:0/96)
        assertFalse((Boolean) method.invoke(
                parserService,
                "::ffff:192.168.1.1"
        ));

        // Public IPv6
        assertTrue((Boolean) method.invoke(
                parserService,
                "2001:4860:4860::8888"
        ));

        assertTrue((Boolean) method.invoke(
                parserService,
                "2606:4700:4700::1111"
        ));

        // Public IPv4
        assertTrue((Boolean) method.invoke(
                parserService,
                "8.8.8.8"
        ));
    }

    // -----------------------------------------------------------------------
    // Sender IP Intelligence tests
    // -----------------------------------------------------------------------

    @Test
    void senderIp_xOriginatingIp_publicIp_returnsConfirmed() throws Exception {
        EmailParsedResult result =
                parseFixture("x-originating-ip-email.eml");

        assertEquals("8.8.8.8", result.getSenderIp());
        assertEquals("X-Originating-IP", result.getSenderIpSource());
        assertEquals("CONFIRMED", result.getSenderIpConfidence());
    }

    @Test
    void senderIp_xClientIp_publicIp_returnsConfirmed() throws Exception {
        EmailParsedResult result =
                parseFixture("x-client-ip-email.eml");

        assertEquals("1.1.1.1", result.getSenderIp());
        assertEquals("X-Client-IP", result.getSenderIpSource());
        assertEquals("CONFIRMED", result.getSenderIpConfidence());
    }

    @Test
    void senderIp_xSenderIp_publicIp_returnsConfirmed() throws Exception {
        EmailParsedResult result =
                parseFixture("x-sender-ip-email.eml");

        assertEquals("8.8.4.4", result.getSenderIp());
        assertEquals("X-Sender-IP", result.getSenderIpSource());
        assertEquals("CONFIRMED", result.getSenderIpConfidence());
    }

    @Test
    void senderIp_xRealIp_publicIp_returnsConfirmed() throws Exception {
        EmailParsedResult result =
                parseFixture("x-real-ip-email.eml");

        assertEquals("8.8.4.4", result.getSenderIp());
        assertEquals("X-Real-IP", result.getSenderIpSource());
        assertEquals("CONFIRMED", result.getSenderIpConfidence());
    }

    @Test
    void senderIp_gmailRelayOnly_noExplicitHeaders_returnsNotExposed() throws Exception {
        EmailParsedResult result =
                parseFixture("gmail-relay-only-email.eml");

        // No explicit client-origin headers present —
        // sender device IP is not exposed.
        assertNull(result.getSenderIp());
        assertEquals("NOT_EXPOSED", result.getSenderIpSource());
        assertEquals("NOT_EXPOSED", result.getSenderIpConfidence());

        // Received-SPF populates connectingIp, NOT senderIp.
        assertEquals("209.85.220.41", result.getConnectingIp());
        assertEquals("Received-SPF", result.getConnectingIpSource());
        assertEquals("CONFIRMED", result.getConnectingIpConfidence());

        // originatingIp must still be the Google relay IP from Received chain.
        assertEquals("8.8.8.8", result.getOriginatingIp());
    }

    @Test
    void senderIp_receivedSpfClientIp_spfPass_populatesConnectingIpOnly() throws Exception {
        EmailParsedResult result =
                parseFixture("received-spf-client-ip-pass-email.eml");

        // Received-SPF must NOT populate senderIp.
        assertNull(result.getSenderIp());
        assertEquals("NOT_EXPOSED", result.getSenderIpSource());
        assertEquals("NOT_EXPOSED", result.getSenderIpConfidence());

        // Received-SPF populates connectingIp.
        assertEquals("1.1.1.1", result.getConnectingIp());
        assertEquals("Received-SPF", result.getConnectingIpSource());
        assertEquals("CONFIRMED", result.getConnectingIpConfidence());
    }

    @Test
    void senderIp_receivedSpfClientIp_spfFail_populatesConnectingIpOnly() throws Exception {
        EmailParsedResult result =
                parseFixture("received-spf-client-ip-fail-email.eml");

        // Received-SPF must NOT populate senderIp.
        assertNull(result.getSenderIp());

        // Received-SPF populates connectingIp.
        assertEquals("8.8.4.4", result.getConnectingIp());
        assertEquals("Received-SPF", result.getConnectingIpSource());
        assertEquals("LIKELY", result.getConnectingIpConfidence());
    }

    @Test
    void senderIp_xOriginatingIpPrivate_fallsThrough_returnsNotExposed() throws Exception {
        EmailParsedResult result =
                parseFixture("x-originating-ip-private-email.eml");

        // 192.168.1.10 is private — must be rejected;
        // no fallback available.
        assertNull(result.getSenderIp());
        assertEquals("NOT_EXPOSED", result.getSenderIpSource());
        assertEquals("NOT_EXPOSED", result.getSenderIpConfidence());
    }

    @Test
    void senderIp_conflictingExplicitHeaders_xOriginatingIpWins() throws Exception {
        EmailParsedResult result =
                parseFixture("conflicting-explicit-headers-email.eml");

        // X-Originating-IP has higher priority than X-Client-IP.
        assertEquals("8.8.8.8", result.getSenderIp());
        assertEquals("X-Originating-IP", result.getSenderIpSource());
        assertEquals("CONFIRMED", result.getSenderIpConfidence());
    }

    @Test
    void senderIp_xOriginatingIpIPv6Public_returnsConfirmed() throws Exception {
        EmailParsedResult result =
                parseFixture("x-originating-ip-ipv6-email.eml");

        assertEquals(
                "2606:4700:4700::1111",
                result.getSenderIp()
        );
        assertEquals(
                "X-Originating-IP",
                result.getSenderIpSource()
        );
        assertEquals(
                "CONFIRMED",
                result.getSenderIpConfidence()
        );
    }

    @Test
    void senderIp_receivedChainAlone_doesNotPopulateSenderIp() throws Exception {
        // multiple-received-email.eml has only Received headers,
        // no explicit client-origin headers.
        // senderIp must remain null;
        // originatingIp gets the Received-chain value.
        EmailParsedResult result =
                parseFixture("multiple-received-email.eml");

        assertNull(result.getSenderIp());
        assertEquals("NOT_EXPOSED", result.getSenderIpSource());
        assertEquals("NOT_EXPOSED", result.getSenderIpConfidence());

        // originatingIp should still be populated from the Received chain.
        assertNotNull(result.getOriginatingIp());
    }
}