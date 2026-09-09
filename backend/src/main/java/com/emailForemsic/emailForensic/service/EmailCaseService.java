package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.AiAnalysisRequest;
import com.emailForemsic.emailForensic.dto.AiAnalysisResponse;
import com.emailForemsic.emailForensic.dto.AbuseIpDbResult;
import com.emailForemsic.emailForensic.dto.AsnResult;
import com.emailForemsic.emailForensic.dto.EmailParsedResult;
import com.emailForemsic.emailForensic.dto.GeoLocationResult;
import com.emailForemsic.emailForensic.dto.RdapResult;
import com.emailForemsic.emailForensic.dto.VirusTotalReputationResult;
import com.emailForemsic.emailForensic.entity.EmailCase;
import com.emailForemsic.emailForensic.entity.EmailHeader;
import com.emailForemsic.emailForensic.entity.EmailIndicator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.emailForemsic.emailForensic.repository.EmailCaseRepository;
import com.emailForemsic.emailForensic.util.SHA256Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.mail.internet.InternetAddress;

@Service
public class EmailCaseService {

    private final ObjectMapper objectMapper =
            new ObjectMapper().findAndRegisterModules();

    @Autowired
    private EmailParserService parserService;

    @Autowired
    private GeoLocationService geoLocationService;

    @Autowired
    private EmailCaseRepository caseRepository;

    @Autowired
    private VirusTotalService virusTotalService;

    @Autowired
    private AbuseIpDbService abuseIpDbService;

    @Autowired
    private AsnService asnService;

    @Autowired
    private RdapService rdapService;

    // ============================================================
    // AI SERVICE
    // Spring Boot → Python FastAPI → RAG → Llama
    // ============================================================

    @Autowired
    private AiServiceClient aiServiceClient;


    public EmailCase processAndSaveEml(MultipartFile file) throws Exception {

        byte[] fileBytes = file.getBytes();

        String fileHash =
                SHA256Util.calculateSHA256(fileBytes);

        try (InputStream inputStream =
                     new ByteArrayInputStream(fileBytes)) {

            EmailParsedResult parsedResult =
                    parserService.parseEml(inputStream);

            EmailCase emailCase = EmailCase.builder()
                    .fileName(file.getOriginalFilename())
                    .fileHash(fileHash)
                    .analysisStatus("ANALYZED")
                    .threatScore(0)
                    .originatingIp(parsedResult.getOriginatingIp())
                    .senderIp(parsedResult.getSenderIp())
                    .senderIpSource(parsedResult.getSenderIpSource())
                    .senderIpConfidence(parsedResult.getSenderIpConfidence())
                    .connectingIp(parsedResult.getConnectingIp())
                    .connectingIpSource(parsedResult.getConnectingIpSource())
                    .connectingIpConfidence(parsedResult.getConnectingIpConfidence())
                    .receivedHeaders(
                            serializeReceivedHeaders(parsedResult)
                    )
                    .rawBody(parsedResult.getRawBody())
                    .createdAt(LocalDateTime.now())
                    .build();


            // ========================================================
            // SPOOFING ANALYSIS
            // ========================================================

            analyzeSpoofingRisk(
                    emailCase,
                    parsedResult
            );


            // ========================================================
            // EMAIL HEADER
            // ========================================================

            EmailHeader header = EmailHeader.builder()
                    .subject(parsedResult.getSubject())
                    .senderFrom(parsedResult.getSenderFrom())
                    .replyTo(parsedResult.getReplyTo())
                    .to(parsedResult.getTo())
                    .cc(parsedResult.getCc())
                    .date(parsedResult.getDate())
                    .messageId(parsedResult.getMessageId())
                    .returnPath(parsedResult.getReturnPath())
                    .spfStatus(parsedResult.getSpfStatus())
                    .dkimStatus(parsedResult.getDkimStatus())
                    .dmarcStatus(parsedResult.getDmarcStatus())
                    .build();

            emailCase.setHeader(header);


            // ========================================================
            // IP ENRICHMENT
            // ========================================================

            String enrichmentIp =
                    parsedResult.getSenderIp() != null
                            ? parsedResult.getSenderIp()
                            : (parsedResult.getConnectingIp() != null
                            ? parsedResult.getConnectingIp()
                            : parsedResult.getOriginatingIp());


            if (enrichmentIp != null) {

                // ====================================================
                // GEOLOCATION
                // ====================================================

                GeoLocationResult geoResult;

                try {

                    geoResult =
                            geoLocationService.lookup(enrichmentIp);

                } catch (RuntimeException ex) {

                    geoResult =
                            GeoLocationResult.builder()
                                    .build();
                }

                emailCase.setGeoCountry(
                        geoResult.getCountry()
                );

                emailCase.setGeoCity(
                        geoResult.getCity()
                );

                emailCase.setGeoLatitude(
                        geoResult.getLatitude()
                );

                emailCase.setGeoLongitude(
                        geoResult.getLongitude()
                );

                emailCase.setGeoTimezone(
                        geoResult.getTimezone()
                );


                String geoSummary =
                        buildGeoSummary(geoResult);


                EmailIndicator ipIndicator =
                        EmailIndicator.builder()
                                .type("IP")
                                .value(enrichmentIp)
                                .details(geoSummary)
                                .build();


                // ====================================================
                // ABUSEIPDB
                // ====================================================

                AbuseIpDbResult abuseResult;

                try {

                    abuseResult =
                            abuseIpDbService.checkIp(
                                    enrichmentIp
                            );

                } catch (RuntimeException ex) {

                    abuseResult =
                            AbuseIpDbResult.builder()
                                    .status("ERROR")
                                    .build();
                }

                ipIndicator.setAbuseIpDbStatus(
                        abuseResult.getStatus()
                );

                ipIndicator.setAbuseConfidenceScore(
                        abuseResult.getAbuseConfidenceScore()
                );

                ipIndicator.setTotalReports(
                        abuseResult.getTotalReports()
                );

                ipIndicator.setLastReportedAt(
                        abuseResult.getLastReportedAt()
                );


                // ====================================================
                // ASN
                // ====================================================

                AsnResult asnResult;

                try {

                    asnResult =
                            asnService.lookup(enrichmentIp);

                } catch (RuntimeException ex) {

                    asnResult =
                            AsnResult.builder()
                                    .build();
                }

                ipIndicator.setAsnNumber(
                        asnResult.getAsnNumber()
                );

                ipIndicator.setAsnOrg(
                        asnResult.getAsnOrg()
                );


                // ====================================================
                // RDAP
                // ====================================================

                RdapResult rdapResult;

                try {

                    rdapResult =
                            rdapService.lookup(enrichmentIp);

                } catch (RuntimeException ex) {

                    rdapResult =
                            RdapResult.builder()
                                    .build();
                }

                ipIndicator.setRdapServer(
                        rdapResult.getRdapServer()
                );

                ipIndicator.setRdapRegistry(
                        rdapResult.getRegistry()
                );

                ipIndicator.setRdapHandle(
                        rdapResult.getHandle()
                );

                ipIndicator.setRdapName(
                        rdapResult.getName()
                );

                ipIndicator.setRdapOrganization(
                        rdapResult.getOrganization()
                );

                ipIndicator.setRdapCountry(
                        rdapResult.getCountry()
                );

                ipIndicator.setRdapStartAddress(
                        rdapResult.getStartAddress()
                );

                ipIndicator.setRdapEndAddress(
                        rdapResult.getEndAddress()
                );

                ipIndicator.setRdapCidr(
                        rdapResult.getCidr()
                );


                emailCase.addIndicator(
                        ipIndicator
                );
            }


            // ========================================================
            // URL ANALYSIS
            // ========================================================

            if (parsedResult.getExtractedUrls() != null) {

                for (String url :
                        parsedResult.getExtractedUrls()) {

                    EmailIndicator urlIndicator =
                            EmailIndicator.builder()
                                    .type("URL")
                                    .value(url)
                                    .details(
                                            "Extracted from email body"
                                    )
                                    .build();


                    VirusTotalReputationResult reputation;

                    try {

                        reputation =
                                virusTotalService.checkUrl(url);

                    } catch (RuntimeException exception) {

                        reputation =
                                VirusTotalReputationResult.builder()
                                        .status("ERROR")
                                        .build();
                    }


                    urlIndicator.setVirusTotalStatus(
                            reputation.getStatus()
                    );

                    urlIndicator.setVirusTotalMalicious(
                            reputation.getMalicious()
                    );

                    urlIndicator.setVirusTotalSuspicious(
                            reputation.getSuspicious()
                    );

                    urlIndicator.setVirusTotalHarmless(
                            reputation.getHarmless()
                    );

                    urlIndicator.setVirusTotalUndetected(
                            reputation.getUndetected()
                    );


                    emailCase.addIndicator(
                            urlIndicator
                    );
                }
            }


            // ========================================================
            // SPRING DETERMINISTIC THREAT SCORE
            // ========================================================

            calculateAndSetThreatScore(
                    emailCase
            );


            // ========================================================
            // AI ANALYSIS
            //
            // Spring
            //   ↓
            // Python FastAPI
            //   ↓
            // Objective analysis
            //   ↓
            // CISA RAG
            //   ↓
            // Llama 3.2
            //   ↓
            // AI JSON
            // ========================================================

            try {

                String senderDomain =
                        extractPrimaryDomain(
                                parsedResult.getSenderFrom()
                        );

                if (senderDomain == null) {
                    senderDomain = "";
                }


                String subject =
                        parsedResult.getSubject() != null
                                ? parsedResult.getSubject()
                                : "";


                String bodyText =
                        parsedResult.getRawBody() != null
                                ? parsedResult.getRawBody()
                                : "";


                List<String> urls =
                        parsedResult.getExtractedUrls() != null
                                ? parsedResult.getExtractedUrls()
                                : Collections.emptyList();


                AiAnalysisRequest aiRequest =
                        AiAnalysisRequest.builder()
                                .subject(subject)
                                .bodyText(bodyText)
                                .urls(urls)
                                .senderDomain(senderDomain)
                                .build();


                System.out.println();
                System.out.println(
                        "========================================"
                );

                System.out.println(
                        "Sending email to AI service..."
                );

                System.out.println(
                        "Sender domain: "
                                + senderDomain
                );

                System.out.println(
                        "Body length: "
                                + bodyText.length()
                );

                System.out.println(
                        "URLs: "
                                + urls
                );

                System.out.println(
                        "========================================"
                );


                AiAnalysisResponse aiResponse =
                        aiServiceClient.analyze(
                                aiRequest
                        );


                // ====================================================
                // DISPLAY AI RESPONSE
                // ====================================================

                System.out.println();
                System.out.println(
                        "========================================"
                );

                System.out.println(
                        "AI ANALYSIS RESPONSE"
                );

                System.out.println(
                        "Threat Score: "
                                + aiResponse.getThreatScore()
                );

                System.out.println(
                        "Risk Level: "
                                + aiResponse.getRiskLevel()
                );

                System.out.println(
                        "Verdict: "
                                + aiResponse.getVerdict()
                );

                System.out.println(
                        "Confidence: "
                                + aiResponse.getConfidence()
                );

                System.out.println(
                        "Summary: "
                                + aiResponse.getSummary()
                );

                System.out.println(
                        "Reasoning: "
                                + aiResponse.getReasoning()
                );

                System.out.println(
                        "========================================"
                );


            } catch (Exception aiException) {

                // ====================================================
                // AI FAILURE SHOULD NOT BREAK EMAIL INGESTION
                // ====================================================

                System.err.println();
                System.err.println(
                        "========================================"
                );

                System.err.println(
                        "AI SERVICE ERROR"
                );

                System.err.println(
                        aiException.getMessage()
                );

                System.err.println(
                        "========================================"
                );

                aiException.printStackTrace();
            }


            // ========================================================
            // SAVE EMAIL CASE
            // ========================================================

            return caseRepository.save(
                    emailCase
            );
        }
    }


    // ================================================================
    // SPRING THREAT SCORE
    // ================================================================

    private void calculateAndSetThreatScore(
            EmailCase emailCase) {

        int score = 0;


        if (emailCase.getHeader() != null) {

            if ("fail".equalsIgnoreCase(
                    emailCase.getHeader().getSpfStatus())) {

                score += 10;
            }


            if ("fail".equalsIgnoreCase(
                    emailCase.getHeader().getDkimStatus())) {

                score += 10;
            }


            if ("fail".equalsIgnoreCase(
                    emailCase.getHeader().getDmarcStatus())) {

                score += 10;
            }
        }


        if ("MEDIUM".equalsIgnoreCase(
                emailCase.getSpoofingRisk())) {

            score += 20;

        } else if ("HIGH".equalsIgnoreCase(
                emailCase.getSpoofingRisk())) {

            score += 40;
        }


        boolean maliciousIpCounted =
                false;

        boolean maliciousUrlCounted =
                false;


        if (emailCase.getIndicators() != null) {

            for (EmailIndicator indicator :
                    emailCase.getIndicators()) {

                if ("IP".equalsIgnoreCase(
                        indicator.getType())
                        && !maliciousIpCounted) {

                    if ("MALICIOUS".equalsIgnoreCase(
                            indicator.getAbuseIpDbStatus())
                            ||
                            (indicator.getAbuseConfidenceScore() != null
                                    && indicator.getAbuseConfidenceScore() > 50)) {

                        score += 30;

                        maliciousIpCounted =
                                true;
                    }

                } else if ("URL".equalsIgnoreCase(
                        indicator.getType())
                        && !maliciousUrlCounted) {

                    if ("MALICIOUS".equalsIgnoreCase(
                            indicator.getVirusTotalStatus())) {

                        score += 40;

                        maliciousUrlCounted =
                                true;
                    }
                }
            }
        }


        if (score > 100) {
            score = 100;
        }


        emailCase.setThreatScore(score);
    }


    // ================================================================
    // RECEIVED HEADERS
    // ================================================================

    private String serializeReceivedHeaders(
            EmailParsedResult parsedResult)
            throws JsonProcessingException {

        if (parsedResult.getReceivedHeaders() == null) {
            return null;
        }


        return objectMapper.writeValueAsString(
                parsedResult.getReceivedHeaders()
        );
    }


    // ================================================================
    // GEOLOCATION SUMMARY
    // ================================================================

    private String buildGeoSummary(
            GeoLocationResult geo) {

        if (geo == null) {
            return "Unknown Location";
        }


        StringBuilder sb =
                new StringBuilder();


        if (geo.getCity() != null) {

            sb.append(
                    geo.getCity()
            );
        }


        if (geo.getCountry() != null) {

            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append(
                    geo.getCountry()
            );
        }


        if (geo.getTimezone() != null) {

            if (sb.length() > 0) {

                sb.append(" (")
                        .append(geo.getTimezone())
                        .append(")");

            } else {

                sb.append(
                        geo.getTimezone()
                );
            }
        }


        return sb.length() > 0
                ? sb.toString()
                : "Unknown Location";
    }


    // ================================================================
    // SPOOFING ANALYSIS
    // ================================================================

    private void analyzeSpoofingRisk(
            EmailCase emailCase,
            EmailParsedResult parsedResult) {

        List<String> fromDomains =
                extractDomains(
                        parsedResult.getSenderFrom()
                );

        List<String> replyToDomains =
                extractDomains(
                        parsedResult.getReplyTo()
                );

        List<String> returnPathDomains =
                extractDomains(
                        parsedResult.getReturnPath()
                );


        List<String> findings =
                new ArrayList<>();


        boolean fromReplyToMismatch =
                false;


        if (!fromDomains.isEmpty()
                && !replyToDomains.isEmpty()) {

            String primaryFrom =
                    fromDomains.get(0);


            if (!replyToDomains.contains(
                    primaryFrom)) {

                fromReplyToMismatch =
                        true;

                findings.add(
                        "FROM_REPLY_TO_MISMATCH"
                );
            }
        }


        boolean fromReturnPathMismatch =
                false;


        if (!fromDomains.isEmpty()
                && !returnPathDomains.isEmpty()) {

            String primaryFrom =
                    fromDomains.get(0);


            if (!returnPathDomains.contains(
                    primaryFrom)) {

                fromReturnPathMismatch =
                        true;

                findings.add(
                        "FROM_RETURN_PATH_MISMATCH"
                );
            }
        }


        String dmarcStatus =
                parsedResult.getDmarcStatus() != null
                        ? parsedResult.getDmarcStatus().toLowerCase()
                        : "unknown";


        boolean isDmarcPass =
                "pass".equals(dmarcStatus);


        boolean authIndicatesFailure =
                "fail".equalsIgnoreCase(
                        parsedResult.getSpfStatus())
                        ||
                        "softfail".equalsIgnoreCase(
                                parsedResult.getSpfStatus())
                        ||
                        "fail".equalsIgnoreCase(
                                parsedResult.getDkimStatus())
                        ||
                        "fail".equalsIgnoreCase(
                                parsedResult.getDmarcStatus());


        String risk =
                "UNKNOWN";


        if (fromDomains.isEmpty()) {

            risk = "UNKNOWN";

        } else if (
                fromReplyToMismatch
                        && fromReturnPathMismatch) {

            risk = "HIGH";

        } else if (
                (fromReplyToMismatch
                        || fromReturnPathMismatch)
                        && !isDmarcPass) {

            risk = "HIGH";

        } else if (
                (fromReplyToMismatch
                        || fromReturnPathMismatch)
                        && isDmarcPass) {

            risk = "MEDIUM";

        } else if (
                !fromReplyToMismatch
                        && !fromReturnPathMismatch
                        && !authIndicatesFailure) {

            risk = "LOW";
        }


        emailCase.setSpoofingRisk(
                risk
        );


        try {

            emailCase.setSpoofingFindings(
                    objectMapper.writeValueAsString(
                            findings
                    )
            );

        } catch (JsonProcessingException e) {

            emailCase.setSpoofingFindings(
                    "[]"
            );
        }
    }


    // ================================================================
    // EXTRACT DOMAINS
    // ================================================================

    private List<String> extractDomains(
            String headerValue) {

        if (headerValue == null
                || headerValue.isBlank()) {

            return Collections.emptyList();
        }


        List<String> domains =
                new ArrayList<>();


        try {

            InternetAddress[] addresses =
                    InternetAddress.parse(
                            headerValue,
                            false
                    );


            for (InternetAddress address :
                    addresses) {

                String email =
                        address.getAddress();


                if (email != null
                        && email.contains("@")) {

                    String domain =
                            email.substring(
                                            email.lastIndexOf('@') + 1
                                    )
                                    .toLowerCase(
                                            java.util.Locale.ROOT
                                    )
                                    .trim();


                    if (!domain.isEmpty()) {

                        domains.add(
                                domain
                        );
                    }
                }
            }

        } catch (Exception e) {

            String[] parts =
                    headerValue.split(",");


            for (String part : parts) {

                if (part.contains("@")) {

                    String emailPart =
                            part.substring(
                                            part.lastIndexOf('@') + 1
                                    )
                                    .toLowerCase(
                                            java.util.Locale.ROOT
                                    )
                                    .trim();


                    emailPart =
                            emailPart
                                    .replaceAll(
                                            ">$",
                                            ""
                                    )
                                    .trim();


                    if (!emailPart.isEmpty()) {

                        domains.add(
                                emailPart
                        );
                    }
                }
            }
        }


        return domains.stream()
                .distinct()
                .toList();
    }


    // ================================================================
    // PRIMARY SENDER DOMAIN
    // ================================================================

    private String extractPrimaryDomain(
            String senderFrom) {

        if (senderFrom == null
                || senderFrom.isBlank()) {

            return "";
        }


        try {

            String email =
                    senderFrom.trim();


            if (email.contains("<")
                    && email.contains(">")) {

                email =
                        email.substring(
                                email.indexOf("<") + 1,
                                email.indexOf(">")
                        );
            }


            email =
                    email.replace(
                            "\"",
                            ""
                    ).trim();


            int atIndex =
                    email.lastIndexOf("@");


            if (atIndex == -1
                    || atIndex == email.length() - 1) {

                return "";
            }


            return email.substring(
                            atIndex + 1
                    )
                    .trim()
                    .toLowerCase(
                            java.util.Locale.ROOT
                    );

        } catch (Exception e) {

            return "";
        }
    }
}