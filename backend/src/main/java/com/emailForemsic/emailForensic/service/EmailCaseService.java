package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.AbuseIpDbResult;
import com.emailForemsic.emailForensic.dto.EmailParsedResult;
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

@Service
public class EmailCaseService {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

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

    public EmailCase processAndSaveEml(MultipartFile file) throws Exception {
        byte[] fileBytes = file.getBytes();
        String fileHash = SHA256Util.calculateSHA256(fileBytes);
        
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            EmailParsedResult parsedResult = parserService.parseEml(inputStream);

            int threatScore = 0;
            if (parsedResult.getOriginatingIp() != null) {
                threatScore += 20;
            }

            EmailCase emailCase = EmailCase.builder()
                    .fileName(file.getOriginalFilename())
                    .fileHash(fileHash)
                    .analysisStatus("ANALYZED")
                    .threatScore(threatScore)
                    .originatingIp(parsedResult.getOriginatingIp())
                    .receivedHeaders(serializeReceivedHeaders(parsedResult))
                    .rawBody(parsedResult.getRawBody())
                    .createdAt(LocalDateTime.now())
                    .build();

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

            if (parsedResult.getOriginatingIp() != null) {
                String geoInfo = geoLocationService.getGeoLocation(parsedResult.getOriginatingIp());
                EmailIndicator ipIndicator = EmailIndicator.builder()
                        .type("IP")
                        .value(parsedResult.getOriginatingIp())
                        .details(geoInfo)
                        .build();

                // Enrich with AbuseIPDB — non-fatal; failure sets status=ERROR on indicator
                AbuseIpDbResult abuseResult;
                try {
                    abuseResult = abuseIpDbService.checkIp(parsedResult.getOriginatingIp());
                } catch (RuntimeException ex) {
                    abuseResult = AbuseIpDbResult.builder().status("ERROR").build();
                }
                ipIndicator.setAbuseIpDbStatus(abuseResult.getStatus());
                ipIndicator.setAbuseConfidenceScore(abuseResult.getAbuseConfidenceScore());
                ipIndicator.setTotalReports(abuseResult.getTotalReports());
                ipIndicator.setLastReportedAt(abuseResult.getLastReportedAt());

                emailCase.addIndicator(ipIndicator);
            }

            if (parsedResult.getExtractedUrls() != null) {
                for (String url : parsedResult.getExtractedUrls()) {
                    EmailIndicator urlIndicator = EmailIndicator.builder()
                            .type("URL")
                            .value(url)
                            .details("Extracted from email body")
                            .build();
                        VirusTotalReputationResult reputation;
                        try {
                            reputation = virusTotalService.checkUrl(url);
                        } catch (RuntimeException exception) {
                            reputation = VirusTotalReputationResult.builder().status("ERROR").build();
                        }
                        urlIndicator.setVirusTotalStatus(reputation.getStatus());
                        urlIndicator.setVirusTotalMalicious(reputation.getMalicious());
                        urlIndicator.setVirusTotalSuspicious(reputation.getSuspicious());
                        urlIndicator.setVirusTotalHarmless(reputation.getHarmless());
                        urlIndicator.setVirusTotalUndetected(reputation.getUndetected());
                    emailCase.addIndicator(urlIndicator);
                }
            }

            return caseRepository.save(emailCase);
        }
    }

    private String serializeReceivedHeaders(EmailParsedResult parsedResult) throws JsonProcessingException {
        if (parsedResult.getReceivedHeaders() == null) {
            return null;
        }
        return objectMapper.writeValueAsString(parsedResult.getReceivedHeaders());
    }
}