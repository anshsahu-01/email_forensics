package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.EmailParsedResult;
import com.emailForemsic.emailForensic.dto.ReceivedHeaderInfo;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailParserService {

    private static final Pattern FROM_BY_PATTERN = Pattern.compile("^from\\s+(.+?)(?:\\s+by\\s+(.+?))?(?:\\s+with\\s|\\s+id\\s|\\s+;|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern IP_PATTERN = Pattern.compile("(?i)(?<![0-9a-f:])(?:[0-9]{1,3}(?:\\.[0-9]{1,3}){3}|[0-9a-f]*:[0-9a-f:]+)(?![0-9a-f:])");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(";\\s*(.+?)\\s*$", Pattern.DOTALL);

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
            List<ReceivedHeaderInfo> receivedHeaders = parseReceivedHeaders(message);
            result.setReceivedHeaders(receivedHeaders);
            result.setOriginatingIp(findOriginatingIp(receivedHeaders));
        } catch (MessagingException e) {
            throw new IllegalArgumentException("Malformed .eml content: unable to parse message headers.", e);
        }

        return result;
    }

    private List<ReceivedHeaderInfo> parseReceivedHeaders(MimeMessage message) throws MessagingException {
        String[] headerValues = message.getHeader("Received");
        if (headerValues == null || headerValues.length == 0) {
            return new ArrayList<>();
        }

        List<ReceivedHeaderInfo> receivedHeaders = new ArrayList<>();
        for (String rawValue : headerValues) {
            receivedHeaders.add(parseReceivedHeader(rawValue));
        }
        return receivedHeaders;
    }

    private ReceivedHeaderInfo parseReceivedHeader(String rawValue) {
        ReceivedHeaderInfo info = ReceivedHeaderInfo.builder().rawValue(rawValue).build();
        if (rawValue == null || rawValue.isBlank()) {
            return info;
        }

        Matcher matcher = FROM_BY_PATTERN.matcher(rawValue.trim());
        String fromSection = null;
        String bySection = null;
        if (matcher.find()) {
            fromSection = matcher.group(1);
            bySection = matcher.group(2);
        }

        info.setFromHost(readHost(fromSection));
        info.setFromIp(firstIp(fromSection));
        if (bySection != null) {
            info.setByHost(readHost(bySection));
            info.setByIp(firstIp(bySection));
        }

        Matcher timestampMatcher = TIMESTAMP_PATTERN.matcher(rawValue);
        if (timestampMatcher.find()) {
            info.setTimestamp(parseReceivedTimestamp(timestampMatcher.group(1)));
        }
        return info;
    }

    private String readHost(String section) {
        if (section == null) {
            return null;
        }
        String value = section.trim();
        if (value.isEmpty()) {
            return null;
        }
        String host = value.split("\\s+", 2)[0].replaceAll("^[\\[(]+|[\\])]+$", "");
        return isIpLiteral(host) ? null : (host.isBlank() ? null : host);
    }

    private String firstIp(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = IP_PATTERN.matcher(value);
        while (matcher.find()) {
            String candidate = matcher.group();
            if (isIpLiteral(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isIpLiteral(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (value.matches("[0-9]{1,3}(?:\\.[0-9]{1,3}){3}")) {
            String[] parts = value.split("\\.");
            for (String part : parts) {
                if (Integer.parseInt(part) > 255) {
                    return false;
                }
            }
            return true;
        }
        if (!value.contains(":")) {
            return false;
        }
        try {
            return InetAddress.getByName(value).getHostAddress() != null;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private Instant parseReceivedTimestamp(String value) {
        try {
            return ZonedDateTime.parse(value.trim(), DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH)).toInstant();
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private String findOriginatingIp(List<ReceivedHeaderInfo> receivedHeaders) {
        List<ReceivedHeaderInfo> oldestFirst = new ArrayList<>(receivedHeaders);
        Collections.reverse(oldestFirst);
        for (ReceivedHeaderInfo header : oldestFirst) {
            if (isPublicIp(header.getFromIp())) {
                return header.getFromIp();
            }
            if (isPublicIp(header.getByIp())) {
                return header.getByIp();
            }
        }
        return null;
    }

    private boolean isPublicIp(String value) {
        if (!isIpLiteral(value)) {
            return false;
        }
        try {
            byte[] address = InetAddress.getByName(value).getAddress();
            if (address.length == 4) {
                int first = address[0] & 0xff;
                int second = address[1] & 0xff;
                return first != 0 && first != 127 && !(first == 10 || (first == 172 && second >= 16 && second <= 31) || (first == 192 && second == 168));
            }
            int first = address[0] & 0xff;
            return !isAllZero(address) && !value.equalsIgnoreCase("::1") && (first & 0xfe) != 0xfc && !(first == 0xfe && ((address[1] & 0xc0) == 0x80));
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private boolean isAllZero(byte[] address) {
        for (byte value : address) {
            if (value != 0) {
                return false;
            }
        }
        return true;
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