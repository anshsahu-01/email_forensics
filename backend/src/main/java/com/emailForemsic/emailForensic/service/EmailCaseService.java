package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.dto.EmailParsedResult;
import com.emailForemsic.emailForensic.entity.EmailCase;
import com.emailForemsic.emailForensic.entity.EmailHeader;
import com.emailForemsic.emailForensic.entity.EmailIndicator;
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

    @Autowired
    private EmailParserService parserService;

    @Autowired
    private GeoLocationService geoLocationService;

    @Autowired
    private EmailCaseRepository caseRepository;

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
                    .rawBody(parsedResult.getRawBody())
                    .createdAt(LocalDateTime.now())
                    .build();

            EmailHeader header = EmailHeader.builder()
                    .subject(parsedResult.getSubject())
                    .senderFrom(parsedResult.getSenderFrom())
                    .replyTo(parsedResult.getReplyTo())
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
                emailCase.addIndicator(ipIndicator);
            }

            if (parsedResult.getExtractedUrls() != null) {
                for (String url : parsedResult.getExtractedUrls()) {
                    EmailIndicator urlIndicator = EmailIndicator.builder()
                            .type("URL")
                            .value(url)
                            .details("Extracted from email body")
                            .build();
                    emailCase.addIndicator(urlIndicator);
                }
            }

            return caseRepository.save(emailCase);
        }
    }
}