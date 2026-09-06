package com.emailForemsic.emailForensic.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailParsedResult {
    private String subject;
    private String senderFrom;
    private String replyTo;
    private String to;
    private String cc;
    private Instant date;
    private String messageId;
    private String returnPath;
    private String spfStatus;
    private String dkimStatus;
    private String dmarcStatus;
    /**

     * Intentionally left unpopulated for privacy/security reasons.

     */

    private String rawBody;
    private List<ReceivedHeaderInfo> receivedHeaders;
    private List<String> extractedUrls;
    private String originatingIp;

    /**
     * Sender/client IP determined from explicit evidentiary headers only
     * (X-Originating-IP, X-Sender-IP, X-Client-IP, X-Real-IP, Received-SPF client-ip=).
     * Null when no credible explicit evidence is present in the message headers.
     * Never populated from the Received chain alone — use originatingIp for that.
     */
    private String senderIp;

    /**
     * The header that supplied senderIp, e.g. "X-Originating-IP", "Received-SPF", "NOT_EXPOSED".
     * "NOT_EXPOSED" means no credible explicit evidence was found.
     */
    private String senderIpSource;

    /**
     * Evidence confidence for senderIp:
     * "CONFIRMED" — explicit client-origin header present (unauthenticated but direct claim).
     * "LIKELY"    — connecting IP from Received-SPF where SPF did not pass.
     * "NOT_EXPOSED" — sender device IP was not exposed by message headers.
     */
    private String senderIpConfidence;

    /**
     * Connecting IP observed by the receiving MTA.
     * Typically populated from Received-SPF client-ip=.
     * This may be a mail infrastructure relay and should NEVER be confused with senderIp.
     */
    private String connectingIp;

    private String connectingIpSource;

    private String connectingIpConfidence;
}