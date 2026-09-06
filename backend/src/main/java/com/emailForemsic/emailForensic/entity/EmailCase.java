package com.emailForemsic.emailForensic.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "email_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileHash;
    private String analysisStatus;
    private Integer threatScore;

    private String originatingIp;

    @Column(columnDefinition = "TEXT")
    private String receivedHeaders;

    /**
     * Intentionally left unpopulated.
     * Not persisted for storage optimization and privacy/security reasons.
     */
    @Column(columnDefinition = "TEXT")
    private String rawBody;

    private String spoofingRisk;

    @Column(columnDefinition = "TEXT")
    private String spoofingFindings;

    // -----------------------------------------------------------------------
    // Approximate IP geolocation — populated from MaxMind GeoIP2 lookup.
    // All fields are nullable; existing records without geolocation remain valid.
    // Coordinates represent approximate IP-based location, NOT exact physical location.
    // -----------------------------------------------------------------------
    private String geoCountry;
    private String geoCity;
    private Double geoLatitude;
    private Double geoLongitude;
    private String geoTimezone;

    // -----------------------------------------------------------------------
    // Sender IP Intelligence — populated only from explicit evidentiary headers.
    // senderIp is NEVER derived from the Received chain alone.
    // originatingIp (above) retains its existing semantics: earliest public IP
    // in the Received chain, which may be a mail relay/infrastructure IP.
    // -----------------------------------------------------------------------

    /**
     * Sender/client IP from explicit client-origin headers only
     * (X-Originating-IP, X-Sender-IP, X-Client-IP, X-Real-IP, Received-SPF client-ip=).
     * Null = sender device IP was not exposed by message headers.
     */
    private String senderIp;

    /**
     * The specific header that provided senderIp.
     * Values: "X-Originating-IP", "X-Sender-IP", "X-Client-IP", "X-Real-IP",
     *         "Received-SPF", or "NOT_EXPOSED".
     */
    private String senderIpSource;

    /**
     * Confidence classification for senderIp:
     * "CONFIRMED"   — explicit client-origin header present (unauthenticated, may be forged).
     * "NOT_EXPOSED" — no credible explicit evidence; sender device IP is unknown.
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

    private LocalDateTime createdAt;

    @JsonManagedReference
    @OneToOne(mappedBy = "emailCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private EmailHeader header;

    @JsonManagedReference
    @Builder.Default
    @OneToMany(mappedBy = "emailCase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmailIndicator> indicators = new ArrayList<>();

    public void setHeader(EmailHeader header) {
        this.header = header;
        if (header != null) {
            header.setEmailCase(this);
        }
    }

    public void addIndicator(EmailIndicator indicator) {
        if (this.indicators == null) {
            this.indicators = new ArrayList<>();
        }
        this.indicators.add(indicator);
        indicator.setEmailCase(this);
    }
}