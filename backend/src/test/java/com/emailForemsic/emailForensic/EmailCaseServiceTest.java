package com.emailForemsic.emailForensic;

import com.emailForemsic.emailForensic.dto.EmailParsedResult;
import com.emailForemsic.emailForensic.dto.ReceivedHeaderInfo;
import com.emailForemsic.emailForensic.entity.EmailCase;
import com.emailForemsic.emailForensic.repository.EmailCaseRepository;
import com.emailForemsic.emailForensic.service.EmailCaseService;
import com.emailForemsic.emailForensic.service.EmailParserService;
import com.emailForemsic.emailForensic.service.GeoLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailCaseServiceTest {

    private EmailParserService parserService;
    private GeoLocationService geoLocationService;
    private EmailCaseRepository caseRepository;
    private EmailCaseService emailCaseService;

    @BeforeEach
    void setUp() throws Exception {
        parserService = mock(EmailParserService.class);
        geoLocationService = mock(GeoLocationService.class);
        caseRepository = mock(EmailCaseRepository.class);
        emailCaseService = new EmailCaseService();

        setField(emailCaseService, "parserService", parserService);
        setField(emailCaseService, "geoLocationService", geoLocationService);
        setField(emailCaseService, "caseRepository", caseRepository);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void copiesBasicParsedHeadersIntoEmailHeaderBeforeSaving() throws Exception {
        EmailParsedResult parsedResult = EmailParsedResult.builder()
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
                .originatingIp("203.0.113.25")
                .build();

        when(parserService.parseEml(any(InputStream.class))).thenReturn(parsedResult);
        when(caseRepository.save(any(EmailCase.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MultipartFile file = new MockMultipartFile(
                "file",
                "basic-email.eml",
                "message/rfc822",
                "test email content".getBytes()
        );

        EmailCase savedCase = emailCaseService.processAndSaveEml(file);

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

        ArgumentCaptor<EmailCase> captor = ArgumentCaptor.forClass(EmailCase.class);
        verify(caseRepository).save(captor.capture());
        EmailCase persistedCase = captor.getValue();
        assertEquals(parsedResult.getTo(), persistedCase.getHeader().getTo());
    }
}
