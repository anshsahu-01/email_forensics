package com.emailForemsic.emailForensic;

import com.emailForemsic.emailForensic.dto.EmailParsedResult;
import com.emailForemsic.emailForensic.service.EmailParserService;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;

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
}
