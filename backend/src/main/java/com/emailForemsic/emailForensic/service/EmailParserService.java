package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.EmailParsedResult;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.Properties;

@Service
public class EmailParserService {

    public EmailParsedResult parseEml(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("Input stream cannot be null.");
        }

        Session session = Session.getInstance(new Properties());
        EmailParsedResult result = new EmailParsedResult();

        try {
            MimeMessage message = new MimeMessage(session, inputStream);

            result.setSubject(readDecodedHeaderValue(message, "Subject"));
            result.setSenderFrom(readAddressHeaderValue(message, "From"));
            result.setTo(readAddressHeaderValue(message, "To"));
            result.setCc(readAddressHeaderValue(message, "Cc"));
            result.setReplyTo(readAddressHeaderValue(message, "Reply-To"));
            result.setDate(readSentDate(message));
            result.setMessageId(readSingleHeaderValue(message, "Message-ID"));
            result.setReturnPath(readSingleHeaderValue(message, "Return-Path"));
        } catch (MessagingException e) {
            throw new IllegalArgumentException("Malformed .eml content: unable to parse message headers.", e);
        }

        return result;
    }

    private String readAddressHeaderValue(MimeMessage message, String headerName) throws MessagingException {
        String[] headerValues = message.getHeader(headerName);
        if (headerValues == null || headerValues.length == 0) {
            return null;
        }

        StringBuilder joined = new StringBuilder();
        for (String headerValue : headerValues) {
            if (headerValue == null || headerValue.trim().isEmpty()) {
                continue;
            }

            try {
                InternetAddress[] addresses = InternetAddress.parse(headerValue, false);
                StringBuilder addressesText = new StringBuilder();
                for (InternetAddress address : addresses) {
                    String formatted = address.toUnicodeString();
                    if (formatted != null && !formatted.isBlank()) {
                        if (addressesText.length() > 0) {
                            addressesText.append(", ");
                        }
                        addressesText.append(formatted);
                    }
                }

                if (addressesText.length() > 0) {
                    if (joined.length() > 0) {
                        joined.append(", ");
                    }
                    joined.append(addressesText);
                } else {
                    String decoded = decodeHeaderText(headerValue);
                    if (decoded != null && !decoded.isBlank()) {
                        if (joined.length() > 0) {
                            joined.append(", ");
                        }
                        joined.append(decoded);
                    }
                }
            } catch (Exception e) {
                String decoded = decodeHeaderText(headerValue);
                if (decoded != null && !decoded.isBlank()) {
                    if (joined.length() > 0) {
                        joined.append(", ");
                    }
                    joined.append(decoded);
                }
            }
        }

        return joined.length() == 0 ? null : joined.toString();
    }

    private String readSingleHeaderValue(MimeMessage message, String headerName) throws MessagingException {
        String[] headerValues = message.getHeader(headerName);
        if (headerValues == null || headerValues.length == 0) {
            return null;
        }

        String rawValue = headerValues[0];
        return decodeHeaderText(rawValue);
    }

    private String readDecodedHeaderValue(MimeMessage message, String headerName) throws MessagingException {
        String[] headerValues = message.getHeader(headerName);
        if (headerValues == null || headerValues.length == 0) {
            return null;
        }

        StringBuilder merged = new StringBuilder();
        for (String headerValue : headerValues) {
            String decoded = decodeHeaderText(headerValue);
            if (decoded != null && !decoded.isBlank()) {
                if (merged.length() > 0) {
                    merged.append(" ");
                }
                merged.append(decoded);
            }
        }

        String value = merged.toString();
        return value.isBlank() ? null : value;
    }

    private Instant readSentDate(MimeMessage message) throws MessagingException {
        Date sentDate = message.getSentDate();
        if (sentDate == null) {
            return null;
        }
        return sentDate.toInstant();
    }

    private String decodeHeaderText(String rawValue) {
        if (rawValue == null) {
            return null;
        }

        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            String decoded = MimeUtility.decodeText(trimmed);
            return decoded == null || decoded.isBlank() ? trimmed : decoded;
        } catch (Exception e) {
            return trimmed;
        }
    }
}