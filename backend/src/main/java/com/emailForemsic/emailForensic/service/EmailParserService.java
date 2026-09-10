package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.EmailParsedResult;
import com.emailForemsic.emailForensic.dto.ReceivedHeaderInfo;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailParserService {

    private static final Pattern FROM_BY_PATTERN =
            Pattern.compile(
                    "^from\\s+(.+?)(?:\\s+by\\s+(.+?))?(?:\\s+with\\s|\\s+id\\s|\\s+;|$)",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
            );

    private static final Pattern IP_PATTERN =
            Pattern.compile(
                    "(?i)(?<![0-9a-f:])(?:[0-9]{1,3}(?:\\.[0-9]{1,3}){3}|[0-9a-f]*:[0-9a-f:]+)(?![0-9a-f:])"
            );

    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile(
                    ";\\s*(.+?)\\s*$",
                    Pattern.DOTALL
            );

    private static final Pattern AUTHENTICATION_RESULT_PATTERN =
            Pattern.compile(
                    "\\b(spf|dkim|dmarc)\\s*=\\s*(pass|fail|softfail|neutral|none|temperror|permerror|unknown)\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern RECEIVED_SPF_RESULT_PATTERN =
            Pattern.compile(
                    "^\\s*(pass|fail|softfail|neutral|none|temperror|permerror|unknown)\\b",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern RECEIVED_SPF_CLIENT_IP_PATTERN =
            Pattern.compile(
                    "client-ip=([^;\\s]+)",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern URL_PATTERN =
            Pattern.compile(
                    "https?://[^\\s<>\\\"'\\[\\]]+",
                    Pattern.CASE_INSENSITIVE
            );

    public EmailParsedResult parseEml(InputStream inputStream) {

        if (inputStream == null) {
            throw new IllegalArgumentException(
                    "Input stream cannot be null."
            );
        }

        Session session =
                Session.getInstance(new Properties());

        EmailParsedResult result =
                new EmailParsedResult();

        try {

            MimeMessage message =
                    new MimeMessage(session, inputStream);

            // ============================================================
            // BASIC EMAIL HEADERS
            // ============================================================

            result.setSubject(
                    readDecodedHeaderValue(
                            message,
                            "Subject"
                    )
            );

            result.setSenderFrom(
                    readAddressHeaderValue(
                            message,
                            "From"
                    )
            );

            result.setTo(
                    readAddressHeaderValue(
                            message,
                            "To"
                    )
            );

            result.setCc(
                    readAddressHeaderValue(
                            message,
                            "Cc"
                    )
            );

            result.setReplyTo(
                    readAddressHeaderValue(
                            message,
                            "Reply-To"
                    )
            );

            result.setDate(
                    readSentDate(message)
            );

            result.setMessageId(
                    readSingleHeaderValue(
                            message,
                            "Message-ID"
                    )
            );

            result.setReturnPath(
                    readSingleHeaderValue(
                            message,
                            "Return-Path"
                    )
            );

            // ============================================================
            // AUTHENTICATION RESULTS
            // ============================================================

            readAuthenticationResults(
                    message,
                    result
            );

            // ============================================================
            // SENDER / CONNECTING IP
            // ============================================================

            resolveSenderIp(
                    message,
                    result
            );

            resolveConnectingIp(
                    message,
                    result
            );

            // ============================================================
            // RECEIVED HEADERS
            // ============================================================

            List<ReceivedHeaderInfo> receivedHeaders =
                    parseReceivedHeaders(message);

            result.setReceivedHeaders(
                    receivedHeaders
            );

            result.setOriginatingIp(
                    findOriginatingIp(
                            receivedHeaders
                    )
            );

            // ============================================================
            // EMAIL BODY
            //
            // This is important for the AI service.
            //
            // Preference:
            // 1. text/plain
            // 2. text/html converted to readable text
            //
            // Attachments are ignored.
            // ============================================================

            String bodyText =
                    extractBodyText(message);

            result.setRawBody(
                    bodyText
            );

            // ============================================================
            // URL EXTRACTION
            // ============================================================

            result.setExtractedUrls(
                    extractUrls(message)
            );

        } catch (MessagingException e) {

            throw new IllegalArgumentException(
                    "Malformed .eml content: unable to parse message.",
                    e
            );
        }

        return result;
    }

    // ========================================================================
    // EMAIL BODY EXTRACTION
    // ========================================================================

    /**
     * Extracts the readable email body from the MIME message.
     *
     * For multipart messages:
     * - Prefer text/plain.
     * - Fall back to text/html.
     * - Ignore attachments.
     *
     * For HTML:
     * - HTML tags are removed.
     * - Visible text is retained.
     *
     * This method is intentionally separate from URL extraction so that
     * the AI service receives a clean readable body.
     */
    private String extractBodyText(Part part)
            throws MessagingException {

        if (part == null) {
            return "";
        }

        // Never treat attachments as email body content.
        if (Part.ATTACHMENT.equalsIgnoreCase(
                part.getDisposition())) {
            return "";
        }

        Object content;

        try {
            content = part.getContent();
        } catch (Exception e) {
            return "";
        }

        // ====================================================================
        // MULTIPART MESSAGE
        // ====================================================================

        if (content instanceof Multipart multipart) {

            String plainText = "";
            String htmlText = "";

            for (int index = 0;
                 index < multipart.getCount();
                 index++) {

                Part bodyPart =
                        multipart.getBodyPart(index);

                if (Part.ATTACHMENT.equalsIgnoreCase(
                        bodyPart.getDisposition())) {
                    continue;
                }

                String extracted =
                        extractBodyText(bodyPart);

                if (extracted == null
                        || extracted.isBlank()) {
                    continue;
                }

                String contentType =
                        safeContentType(bodyPart);

                if (contentType.startsWith(
                        "text/plain")) {

                    if (plainText.isBlank()) {
                        plainText = extracted;
                    }

                } else if (contentType.startsWith(
                        "text/html")) {

                    if (htmlText.isBlank()) {
                        htmlText = extracted;
                    }

                } else {

                    // If the MIME type is unusual but contains readable
                    // content, use it only as a fallback.
                    if (plainText.isBlank()
                            && htmlText.isBlank()) {
                        plainText = extracted;
                    }
                }
            }

            // Prefer the plain-text version because it is cleaner for
            // forensic analysis and LLM processing.
            if (!plainText.isBlank()) {
                return normalizeBodyText(
                        plainText
                );
            }

            if (!htmlText.isBlank()) {
                return normalizeBodyText(
                        htmlText
                );
            }

            return "";
        }

        // ====================================================================
        // SIMPLE STRING BODY
        // ====================================================================

        if (content instanceof String text) {

            String contentType =
                    safeContentType(part);

            if (contentType.startsWith(
                    "text/plain")) {

                return normalizeBodyText(
                        text
                );
            }

            if (contentType.startsWith(
                    "text/html")) {

                Document document =
                        Jsoup.parse(text);

                return normalizeBodyText(
                        document.text()
                );
            }

            // Some malformed/non-standard emails may still expose their
            // body as String. Keep it as a fallback.
            return normalizeBodyText(
                    text
            );
        }

        return "";
    }

    /**
     * Safely obtains a MIME content type.
     */
    private String safeContentType(
            Part part) {

        try {

            String contentType =
                    part.getContentType();

            if (contentType == null) {
                return "";
            }

            return contentType
                    .toLowerCase(Locale.ROOT);

        } catch (MessagingException e) {

            return "";
        }
    }

    /**
     * Normalizes extracted body text without destroying forensic content.
     *
     * It:
     * - normalizes CRLF/CR to LF
     * - removes excessive blank lines
     * - trims leading/trailing whitespace
     */
    private String normalizeBodyText(
            String text) {

        if (text == null
                || text.isBlank()) {

            return "";
        }

        String normalized =
                text.replace("\r\n", "\n")
                        .replace('\r', '\n');

        normalized =
                normalized.replaceAll(
                        "[ \\t]+\\n",
                        "\n"
                );

        normalized =
                normalized.replaceAll(
                        "\\n{3,}",
                        "\n\n"
                );

        return normalized.trim();
    }

    // ========================================================================
    // URL EXTRACTION
    // ========================================================================

    private List<String> extractUrls(
            Part part)
            throws MessagingException {

        LinkedHashSet<String> urls =
                new LinkedHashSet<>();

        collectUrls(
                part,
                urls
        );

        return new ArrayList<>(
                urls
        );
    }

    private void collectUrls(
            Part part,
            LinkedHashSet<String> urls)
            throws MessagingException {

        if (part == null) {
            return;
        }

        if (Part.ATTACHMENT.equalsIgnoreCase(
                part.getDisposition())) {
            return;
        }

        Object content;

        try {

            content =
                    part.getContent();

        } catch (Exception e) {

            return;
        }

        // ====================================================================
        // MULTIPART
        // ====================================================================

        if (content instanceof Multipart multipart) {

            for (int index = 0;
                 index < multipart.getCount();
                 index++) {

                collectUrls(
                        multipart.getBodyPart(index),
                        urls
                );
            }

            return;
        }

        // ====================================================================
        // STRING CONTENT
        // ====================================================================

        if (!(content instanceof String text)) {
            return;
        }

        String contentType =
                safeContentType(part);

        // ====================================================================
        // HTML
        // ====================================================================

        if (contentType.startsWith(
                "text/html")) {

            Document document =
                    Jsoup.parse(text);

            // First extract the actual href values.
            for (Element link :
                    document.select("a[href]")) {

                String href =
                        link.attr("abs:href");

                addNormalizedUrl(
                        href,
                        urls
                );

                // Also inspect visible link text.
                addTextUrls(
                        link.text(),
                        urls
                );
            }

            // Then inspect all visible HTML text.
            addTextUrls(
                    document.text(),
                    urls
            );

        }

        // ====================================================================
        // PLAIN TEXT
        // ====================================================================

        else if (contentType.startsWith(
                "text/plain")) {

            addTextUrls(
                    text,
                    urls
            );
        }
    }

    /**
     * Finds HTTP/HTTPS URLs inside arbitrary text.
     *
     * This handles normal URLs as well as URLs surrounded by common
     * Markdown/text punctuation.
     */
    private void addTextUrls(
            String text,
            LinkedHashSet<String> urls) {

        if (text == null
                || text.isBlank()) {
            return;
        }

        Matcher matcher =
                URL_PATTERN.matcher(text);

        while (matcher.find()) {

            addNormalizedUrl(
                    matcher.group(),
                    urls
            );
        }
    }

    /**
     * Validates and normalizes one URL candidate.
     *
     * Only HTTP and HTTPS URLs with a valid host are retained.
     */
    private void addNormalizedUrl(
            String candidate,
            LinkedHashSet<String> urls) {

        if (candidate == null
                || candidate.isBlank()) {
            return;
        }

        String normalized =
                candidate.trim();

        // Remove common surrounding punctuation.
        while (!normalized.isEmpty()
                && ".,;:!?)]}>\"'".indexOf(
                        normalized.charAt(
                                normalized.length() - 1
                        )
                ) >= 0) {

            normalized =
                    normalized.substring(
                            0,
                            normalized.length() - 1
                    );
        }

        // Remove common opening Markdown/bracket decoration if present.
        while (!normalized.isEmpty()
                && "([{<".indexOf(
                        normalized.charAt(0)
                ) >= 0) {

            normalized =
                    normalized.substring(1);
        }

        if (normalized.isBlank()) {
            return;
        }

        try {

            URI uri =
                    new URI(normalized);

            String scheme =
                    uri.getScheme();

            if (!"http".equalsIgnoreCase(
                    scheme)
                    && !"https".equalsIgnoreCase(
                    scheme)) {

                return;
            }

            String host =
                    uri.getHost();

            if (host == null
                    || host.isBlank()) {

                return;
            }

            urls.add(
                    normalized
            );

        } catch (Exception e) {

            // Ignore malformed individual URL candidates.
            // One bad URL must never break the email parser.
        }
    }

    // ========================================================================
    // AUTHENTICATION RESULTS
    // ========================================================================

    /**
     * Reads authentication evidence carried by the message headers only.
     *
     * This does NOT perform:
     * - DNS lookups
     * - SPF policy evaluation
     * - DKIM cryptographic verification
     *
     * Authentication-Results entries are considered in header order and
     * the first valid result for each mechanism wins.
     */
    private void readAuthenticationResults(
            MimeMessage message,
            EmailParsedResult result)
            throws MessagingException {

        String spfStatus = null;
        String dkimStatus = null;
        String dmarcStatus = null;

        String[] authenticationHeaders =
                message.getHeader(
                        "Authentication-Results"
                );

        if (authenticationHeaders != null) {

            for (String headerValue :
                    authenticationHeaders) {

                if (headerValue == null) {
                    continue;
                }

                Matcher matcher =
                        AUTHENTICATION_RESULT_PATTERN
                                .matcher(headerValue);

                while (matcher.find()) {

                    String mechanism =
                            matcher.group(1)
                                    .toLowerCase(
                                            Locale.ROOT
                                    );

                    String status =
                            matcher.group(2)
                                    .toLowerCase(
                                            Locale.ROOT
                                    );

                    if (mechanism.equals("spf")
                            && spfStatus == null) {

                        spfStatus = status;

                    } else if (
                            mechanism.equals("dkim")
                                    && dkimStatus == null) {

                        dkimStatus = status;

                    } else if (
                            mechanism.equals("dmarc")
                                    && dmarcStatus == null) {

                        dmarcStatus = status;
                    }
                }
            }
        }

        // Fallback to Received-SPF if SPF wasn't found in
        // Authentication-Results.
        if (spfStatus == null) {

            String[] receivedSpfHeaders =
                    message.getHeader(
                            "Received-SPF"
                    );

            if (receivedSpfHeaders != null) {

                for (String headerValue :
                        receivedSpfHeaders) {

                    if (headerValue == null) {
                        continue;
                    }

                    Matcher matcher =
                            RECEIVED_SPF_RESULT_PATTERN
                                    .matcher(headerValue);

                    if (matcher.find()) {

                        spfStatus =
                                matcher.group(1)
                                        .toLowerCase(
                                                Locale.ROOT
                                        );

                        break;
                    }
                }
            }
        }

        result.setSpfStatus(
                spfStatus == null
                        ? "none"
                        : spfStatus
        );

        result.setDkimStatus(
                dkimStatus == null
                        ? "none"
                        : dkimStatus
        );

        result.setDmarcStatus(
                dmarcStatus == null
                        ? "none"
                        : dmarcStatus
        );
    }

    // ========================================================================
    // RECEIVED HEADERS
    // ========================================================================

    private List<ReceivedHeaderInfo> parseReceivedHeaders(
            MimeMessage message)
            throws MessagingException {

        String[] headerValues =
                message.getHeader(
                        "Received"
                );

        if (headerValues == null
                || headerValues.length == 0) {

            return new ArrayList<>();
        }

        List<ReceivedHeaderInfo> receivedHeaders =
                new ArrayList<>();

        for (String rawValue :
                headerValues) {

            receivedHeaders.add(
                    parseReceivedHeader(
                            rawValue
                    )
            );
        }

        return receivedHeaders;
    }

    private ReceivedHeaderInfo parseReceivedHeader(
            String rawValue) {

        ReceivedHeaderInfo info =
                ReceivedHeaderInfo.builder()
                        .rawValue(rawValue)
                        .build();

        if (rawValue == null
                || rawValue.isBlank()) {

            return info;
        }

        Matcher matcher =
                FROM_BY_PATTERN.matcher(
                        rawValue.trim()
                );

        String fromSection = null;
        String bySection = null;

        if (matcher.find()) {

            fromSection =
                    matcher.group(1);

            bySection =
                    matcher.group(2);
        }

        info.setFromHost(
                readHost(fromSection)
        );

        info.setFromIp(
                firstIp(fromSection)
        );

        if (bySection != null) {

            info.setByHost(
                    readHost(bySection)
            );

            info.setByIp(
                    firstIp(bySection)
            );
        }

        Matcher timestampMatcher =
                TIMESTAMP_PATTERN.matcher(
                        rawValue
                );

        if (timestampMatcher.find()) {

            info.setTimestamp(
                    parseReceivedTimestamp(
                            timestampMatcher.group(1)
                    )
            );
        }

        return info;
    }

    private String readHost(
            String section) {

        if (section == null) {
            return null;
        }

        String value =
                section.trim();

        if (value.isEmpty()) {
            return null;
        }

        String host =
                value.split(
                        "\\s+",
                        2
                )[0]
                .replaceAll(
                        "^[\\[(]+|[\\])]+$",
                        ""
                );

        return isIpLiteral(host)
                ? null
                : (
                    host.isBlank()
                            ? null
                            : host
                );
    }

    private String firstIp(
            String value) {

        if (value == null) {
            return null;
        }

        Matcher matcher =
                IP_PATTERN.matcher(
                        value
                );

        while (matcher.find()) {

            String candidate =
                    matcher.group();

            if (isIpLiteral(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    // ========================================================================
    // IP VALIDATION
    // ========================================================================

    private boolean isIpLiteral(
            String value) {

        if (value == null
                || value.isBlank()) {

            return false;
        }

        // IPv4
        if (value.matches(
                "[0-9]{1,3}(?:\\.[0-9]{1,3}){3}"
        )) {

            String[] parts =
                    value.split("\\.");

            for (String part :
                    parts) {

                if (Integer.parseInt(part) > 255) {
                    return false;
                }
            }

            return true;
        }

        // IPv6
        if (!value.contains(":")) {
            return false;
        }

        try {

            return InetAddress
                    .getByName(value)
                    .getHostAddress() != null;

        } catch (UnknownHostException e) {

            return false;
        }
    }

    private Instant parseReceivedTimestamp(
            String value) {

        try {

            return ZonedDateTime.parse(
                    value.trim(),
                    DateTimeFormatter.RFC_1123_DATE_TIME
                            .withLocale(
                                    Locale.ENGLISH
                            )
            ).toInstant();

        } catch (DateTimeParseException e) {

            return null;
        }
    }

    // ========================================================================
    // ORIGINATING IP
    // ========================================================================

    private String findOriginatingIp(
            List<ReceivedHeaderInfo> receivedHeaders) {

        List<ReceivedHeaderInfo> oldestFirst =
                new ArrayList<>(
                        receivedHeaders
                );

        Collections.reverse(
                oldestFirst
        );

        for (ReceivedHeaderInfo header :
                oldestFirst) {

            if (isPublicIp(
                    header.getFromIp()
            )) {

                return header.getFromIp();
            }

            if (isPublicIp(
                    header.getByIp()
            )) {

                return header.getByIp();
            }
        }

        return null;
    }

    private boolean isPublicIp(
            String value) {

        if (!isIpLiteral(value)) {
            return false;
        }

        try {

            InetAddress addr =
                    InetAddress.getByName(
                            value
                    );

            if (addr.isAnyLocalAddress()
                    || addr.isLoopbackAddress()
                    || addr.isLinkLocalAddress()) {

                return false;
            }

            if (addr.isMulticastAddress()) {
                return false;
            }

            byte[] address =
                    addr.getAddress();

            // ================================================================
            // IPv4
            // ================================================================

            if (address.length == 4) {

                int first =
                        address[0] & 0xff;

                int second =
                        address[1] & 0xff;

                // Private:
                // 10.0.0.0/8
                // 172.16.0.0/12
                // 192.168.0.0/16
                if (first == 10
                        || (first == 172
                        && second >= 16
                        && second <= 31)
                        || (first == 192
                        && second == 168)) {

                    return false;
                }

                // Carrier-grade NAT:
                // 100.64.0.0/10
                if (first == 100
                        && second >= 64
                        && second <= 127) {

                    return false;
                }

                // Documentation:
                // 192.0.2.0/24
                if (first == 192
                        && second == 0
                        && (address[2] & 0xff) == 2) {

                    return false;
                }

                // TEST-NET-2:
                // 198.51.100.0/24
                if (first == 198
                        && second == 51
                        && (address[2] & 0xff) == 100) {

                    return false;
                }

                // TEST-NET-3:
                // 203.0.113.0/24
                if (first == 203
                        && second == 0
                        && (address[2] & 0xff) == 113) {

                    return false;
                }

                // Loopback:
                // 127.0.0.0/8
                if (first == 127) {
                    return false;
                }

                // Link-local:
                // 169.254.0.0/16
                if (first == 169
                        && second == 254) {

                    return false;
                }

                return true;
            }

            // ================================================================
            // IPv6
            // ================================================================

            if (address.length == 16) {

                int first =
                        address[0] & 0xff;

                int second =
                        address[1] & 0xff;

                // Unique Local:
                // fc00::/7
                if ((first & 0xfe) == 0xfc) {
                    return false;
                }

                // Documentation:
                // 2001:db8::/32
                if (first == 0x20
                        && second == 0x01
                        && (address[2] & 0xff) == 0x0d
                        && (address[3] & 0xff) == 0xb8) {

                    return false;
                }

                // IPv4-mapped IPv6:
                // ::ffff:0:0/96
                boolean isIpv4Mapped =
                        true;

                for (int i = 0; i < 10; i++) {

                    if (address[i] != 0) {

                        isIpv4Mapped =
                                false;

                        break;
                    }
                }

                if (isIpv4Mapped
                        && (address[10] & 0xff) == 0xff
                        && (address[11] & 0xff) == 0xff) {

                    return false;
                }

                return true;
            }

        } catch (UnknownHostException e) {

            return false;
        }

        return false;
    }

    // ========================================================================
    // SENDER IP
    // ========================================================================

    /**
     * Determines the best-evidence sender/client IP by examining explicit
     * client-origin headers in deterministic priority order.
     *
     * The Received chain is intentionally NOT used here.
     * It is handled separately by findOriginatingIp().
     *
     * Priority:
     *
     * 1. X-Originating-IP
     * 2. X-Sender-IP
     * 3. X-Client-IP
     * 4. X-Real-IP
     */
    private void resolveSenderIp(
            MimeMessage message,
            EmailParsedResult result)
            throws MessagingException {

        String[][] explicitHeaders = {

                {
                        "X-Originating-IP",
                        "X-Originating-IP"
                },

                {
                        "X-Sender-IP",
                        "X-Sender-IP"
                },

                {
                        "X-Client-IP",
                        "X-Client-IP"
                },

                {
                        "X-Real-IP",
                        "X-Real-IP"
                }
        };

        for (String[] candidate :
                explicitHeaders) {

            String ip =
                    extractPublicIpFromSingleHeader(
                            message,
                            candidate[0]
                    );

            if (ip != null) {

                result.setSenderIp(
                        ip
                );

                result.setSenderIpSource(
                        candidate[1]
                );

                result.setSenderIpConfidence(
                        "CONFIRMED"
                );

                return;
            }
        }

        // No credible explicit evidence found.
        result.setSenderIp(null);

        result.setSenderIpSource(
                "NOT_EXPOSED"
        );

        result.setSenderIpConfidence(
                "NOT_EXPOSED"
        );
    }

    // ========================================================================
    // CONNECTING IP
    // ========================================================================

    /**
     * Extracts the connecting IP observed by the receiving MTA.
     *
     * Typically populated from:
     *
     * Received-SPF: ... client-ip=...
     *
     * Confidence:
     *
     * PASS     -> CONFIRMED
     * otherwise -> LIKELY
     */
    private void resolveConnectingIp(
            MimeMessage message,
            EmailParsedResult result)
            throws MessagingException {

        String spfClientIp =
                extractReceivedSpfClientIp(
                        message
                );

        if (spfClientIp != null
                && isPublicIp(spfClientIp)) {

            String spfStatus =
                    result.getSpfStatus();

            String confidence =
                    "pass".equalsIgnoreCase(
                            spfStatus
                    )
                            ? "CONFIRMED"
                            : "LIKELY";

            result.setConnectingIp(
                    spfClientIp
            );

            result.setConnectingIpSource(
                    "Received-SPF"
            );

            result.setConnectingIpConfidence(
                    confidence
            );

            return;
        }

        result.setConnectingIp(
                null
        );

        result.setConnectingIpSource(
                null
        );

        result.setConnectingIpConfidence(
                null
        );
    }

    // ========================================================================
    // EXPLICIT IP HEADER
    // ========================================================================

    /**
     * Reads a named header and returns the value only if it represents a
     * publicly routable IP address.
     *
     * Common bracket decoration is removed:
     *
     * [1.2.3.4]
     */
    private String extractPublicIpFromSingleHeader(
            MimeMessage message,
            String headerName)
            throws MessagingException {

        String[] values =
                message.getHeader(
                        headerName
                );

        if (values == null
                || values.length == 0) {

            return null;
        }

        String raw =
                values[0]
                        .trim()
                        .replaceAll(
                                "^\\[|\\]$",
                                ""
                        )
                        .trim();

        return isPublicIp(raw)
                ? raw
                : null;
    }

    // ========================================================================
    // RECEIVED-SPF CLIENT IP
    // ========================================================================

    /**
     * Extracts the client-ip value from Received-SPF.
     */
    private String extractReceivedSpfClientIp(
            MimeMessage message)
            throws MessagingException {

        String[] headers =
                message.getHeader(
                        "Received-SPF"
                );

        if (headers == null) {
            return null;
        }

        for (String header :
                headers) {

            if (header == null) {
                continue;
            }

            Matcher matcher =
                    RECEIVED_SPF_CLIENT_IP_PATTERN
                            .matcher(header);

            if (matcher.find()) {

                return matcher.group(1)
                        .trim();
            }
        }

        return null;
    }

    // ========================================================================
    // ADDRESS HEADER
    // ========================================================================

    private String readAddressHeaderValue(
            MimeMessage message,
            String headerName)
            throws MessagingException {

        String[] headerValues =
                message.getHeader(
                        headerName
                );

        if (headerValues == null
                || headerValues.length == 0) {

            return null;
        }

        StringBuilder joined =
                new StringBuilder();

        for (String headerValue :
                headerValues) {

            if (headerValue == null
                    || headerValue.trim().isEmpty()) {

                continue;
            }

            try {

                InternetAddress[] addresses =
                        InternetAddress.parse(
                                headerValue,
                                false
                        );

                StringBuilder addressesText =
                        new StringBuilder();

                for (InternetAddress address :
                        addresses) {

                    String formatted =
                            address.toUnicodeString();

                    if (formatted != null
                            && !formatted.isBlank()) {

                        if (addressesText.length() > 0) {
                            addressesText.append(
                                    ", "
                            );
                        }

                        addressesText.append(
                                formatted
                        );
                    }
                }

                if (addressesText.length() > 0) {

                    if (joined.length() > 0) {
                        joined.append(
                                ", "
                        );
                    }

                    joined.append(
                            addressesText
                    );

                } else {

                    String decoded =
                            decodeHeaderText(
                                    headerValue
                            );

                    if (decoded != null
                            && !decoded.isBlank()) {

                        if (joined.length() > 0) {
                            joined.append(
                                    ", "
                            );
                        }

                        joined.append(
                                decoded
                        );
                    }
                }

            } catch (Exception e) {

                String decoded =
                        decodeHeaderText(
                                headerValue
                        );

                if (decoded != null
                        && !decoded.isBlank()) {

                    if (joined.length() > 0) {
                        joined.append(
                                ", "
                        );
                    }

                    joined.append(
                            decoded
                    );
                }
            }
        }

        return joined.length() == 0
                ? null
                : joined.toString();
    }

    // ========================================================================
    // SINGLE HEADER
    // ========================================================================

    private String readSingleHeaderValue(
            MimeMessage message,
            String headerName)
            throws MessagingException {

        String[] headerValues =
                message.getHeader(
                        headerName
                );

        if (headerValues == null
                || headerValues.length == 0) {

            return null;
        }

        String rawValue =
                headerValues[0];

        return decodeHeaderText(
                rawValue
        );
    }

    // ========================================================================
    // DECODED HEADER
    // ========================================================================

    private String readDecodedHeaderValue(
            MimeMessage message,
            String headerName)
            throws MessagingException {

        String[] headerValues =
                message.getHeader(
                        headerName
                );

        if (headerValues == null
                || headerValues.length == 0) {

            return null;
        }

        StringBuilder merged =
                new StringBuilder();

        for (String headerValue :
                headerValues) {

            String decoded =
                    decodeHeaderText(
                            headerValue
                    );

            if (decoded != null
                    && !decoded.isBlank()) {

                if (merged.length() > 0) {
                    merged.append(
                            " "
                    );
                }

                merged.append(
                        decoded
                );
            }
        }

        String value =
                merged.toString();

        return value.isBlank()
                ? null
                : value;
    }

    // ========================================================================
    // SENT DATE
    // ========================================================================

    private Instant readSentDate(
            MimeMessage message)
            throws MessagingException {

        Date sentDate =
                message.getSentDate();

        if (sentDate == null) {
            return null;
        }

        return sentDate.toInstant();
    }

    // ========================================================================
    // HEADER DECODING
    // ========================================================================

    private String decodeHeaderText(
            String rawValue) {

        if (rawValue == null) {
            return null;
        }

        String trimmed =
                rawValue.trim();

        if (trimmed.isEmpty()) {
            return null;
        }

        try {

            String decoded =
                    MimeUtility.decodeText(
                            trimmed
                    );

            return decoded == null
                    || decoded.isBlank()
                    ? trimmed
                    : decoded;

        } catch (Exception e) {

            return trimmed;
        }
    }
}