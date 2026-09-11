package com.emailForemsic.emailForensic.controller;

import com.emailForemsic.emailForensic.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cases")
@CrossOrigin(origins = "*") // Allow cross-origin requests from Next.js dev server (mirrors EmailAnalysisController)
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/{caseId}/report")
    public ResponseEntity<byte[]> generateReport(
            @PathVariable Long caseId) {

        try {

            byte[] pdf = reportService.generateReport(caseId);

            String fileName =
                    "email-forensic-report-" + caseId + ".pdf";

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_PDF
            );

            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(fileName)
                            .build()
            );

            headers.setContentLength(pdf.length);

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(pdf);

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .notFound()
                    .build();

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .build();
        }
    }
}