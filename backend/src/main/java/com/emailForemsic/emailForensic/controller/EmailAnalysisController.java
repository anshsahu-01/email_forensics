package com.emailForemsic.emailForensic.controller;

import com.emailForemsic.emailForensic.entity.EmailCase;
import com.emailForemsic.emailForensic.repository.EmailCaseRepository;
import com.emailForemsic.emailForensic.service.EmailCaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*") // Allows calls from Next.js or Postman during dev
public class EmailAnalysisController {

    private final EmailCaseService caseService;
    private final EmailCaseRepository caseRepository;

    public EmailAnalysisController(EmailCaseService caseService, EmailCaseRepository caseRepository) {
        this.caseService = caseService;
        this.caseRepository = caseRepository;
    }

    // 1. POST Endpoint to analyze uploaded .eml file
    @PostMapping("/emails/analyze")
    public ResponseEntity<?> analyzeEmail(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Uploaded file is empty.");
        }

        try {
            EmailCase savedCase = caseService.processAndSaveEml(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCase);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing email case: " + e.getMessage());
        }
    }

    // 2. GET Endpoint to fetch all past cases for dashboard
    @GetMapping("/cases")
    public ResponseEntity<List<EmailCase>> getAllCases() {
        List<EmailCase> cases = caseRepository.findAll();
        return ResponseEntity.ok(cases);
    }

    // 3. GET Endpoint to fetch a single case by ID
    @GetMapping("/cases/{id}")
    public ResponseEntity<?> getCaseById(@PathVariable Long id) {
        return caseRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}