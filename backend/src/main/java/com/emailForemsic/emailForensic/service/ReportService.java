package com.emailForemsic.emailForensic.service;

import com.emailForemsic.emailForensic.entity.EmailCase;
import com.emailForemsic.emailForensic.entity.EmailHeader;
import com.emailForemsic.emailForensic.entity.EmailIndicator;
import com.emailForemsic.emailForensic.repository.EmailCaseRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private final EmailCaseRepository caseRepository;

    public ReportService(EmailCaseRepository caseRepository) {
        this.caseRepository = caseRepository;
    }

    /**
     * Generates a forensic PDF report for an existing email case.
     *
     * @param caseId email case ID
     * @return generated PDF as byte array
     */
    public byte[] generateReport(Long caseId) throws Exception {

        EmailCase emailCase = caseRepository.findById(caseId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Email case not found: " + caseId
                        )
                );

        String html = buildReportHtml(emailCase);

        try (ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            PdfRendererBuilder builder =
                    new PdfRendererBuilder();

            builder.useFastMode();

            builder.withHtmlContent(
                    html,
                    null
            );

            builder.toStream(outputStream);

            builder.run();

            return outputStream.toByteArray();
        }
    }

    /**
     * Builds the complete forensic report HTML.
     */
    private String buildReportHtml(EmailCase emailCase) {

        StringBuilder html = new StringBuilder();

        html.append("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8"/>

                    <style>

                        @page {
                            size: A4;
                            margin: 40px 42px 45px 42px;

                            @bottom-right {
                                content: "Page " counter(page);
                                font-size: 9px;
                                color: #666666;
                            }
                        }

                        body {
                            font-family: Arial, sans-serif;
                            font-size: 10px;
                            color: #222222;
                            line-height: 1.45;
                        }

                        h1 {
                            font-size: 24px;
                            margin-bottom: 6px;
                            color: #111111;
                        }

                        h2 {
                            font-size: 16px;
                            margin-top: 22px;
                            margin-bottom: 10px;
                            padding-bottom: 5px;
                            border-bottom: 1px solid #cccccc;
                            color: #222222;
                        }

                        h3 {
                            font-size: 12px;
                            margin-top: 14px;
                            margin-bottom: 6px;
                        }

                        p {
                            margin: 4px 0;
                        }

                        .cover {
                            margin-top: 90px;
                            text-align: center;
                        }

                        .cover-title {
                            font-size: 28px;
                            font-weight: bold;
                            margin-bottom: 12px;
                        }

                        .cover-subtitle {
                            font-size: 14px;
                            color: #666666;
                        }

                        .case-id {
                            margin-top: 35px;
                            font-size: 12px;
                        }

                        .section {
                            page-break-inside: avoid;
                        }

                        .summary-box {
                            border: 1px solid #cccccc;
                            padding: 12px;
                            margin-top: 10px;
                            margin-bottom: 12px;
                        }

                        .risk-box {
                            border: 1px solid #999999;
                            padding: 12px;
                            margin: 10px 0 15px 0;
                        }

                        table {
                            width: 100%;
                            border-collapse: collapse;
                            margin-top: 7px;
                            margin-bottom: 12px;
                        }

                        th {
                            text-align: left;
                            background: #eeeeee;
                            border: 1px solid #cccccc;
                            padding: 7px;
                            font-size: 9px;
                        }

                        td {
                            border: 1px solid #cccccc;
                            padding: 7px;
                            vertical-align: top;
                            word-wrap: break-word;
                        }

                        .label {
                            width: 30%;
                            font-weight: bold;
                            background: #f7f7f7;
                        }

                        .score {
                            font-size: 22px;
                            font-weight: bold;
                        }

                        .muted {
                            color: #666666;
                        }

                        .mono {
                            font-family: monospace;
                            font-size: 8px;
                            word-wrap: break-word;
                        }

                        .indicator {
                            page-break-inside: avoid;
                            margin-bottom: 12px;
                        }

                        .indicator-title {
                            font-weight: bold;
                            font-size: 11px;
                            margin-bottom: 5px;
                        }

                        .ai-box {
                            border: 1px solid #bbbbbb;
                            padding: 12px;
                            margin-top: 8px;
                        }

                        .footer-note {
                            margin-top: 25px;
                            padding-top: 8px;
                            border-top: 1px solid #cccccc;
                            font-size: 8px;
                            color: #666666;
                        }

                    </style>
                </head>

                <body>
                """);

        // ============================================================
        // COVER
        // ============================================================

        html.append("""
                <div class="cover">

                    <div class="cover-title">
                        EMAIL FORENSIC INVESTIGATION REPORT
                    </div>

                    <div class="cover-subtitle">
                        AI-Powered Email Threat Detection and Forensic Intelligence
                    </div>
                """);

        html.append("<div class=\"case-id\">")
                .append("<strong>Case ID:</strong> ")
                .append(escapeHtml(String.valueOf(emailCase.getId())))
                .append("</div>");

        html.append("""
                </div>

                <div style="page-break-after: always;"></div>
                """);

        // ============================================================
        // CASE SUMMARY
        // ============================================================

        html.append("<div class=\"section\">");

        html.append("<h2>1. Case Summary</h2>");

        html.append("<table>");

        appendRow(
                html,
                "Case ID",
                String.valueOf(emailCase.getId()),
                false
        );

        appendRow(
                html,
                "File Name",
                emailCase.getFileName(),
                false
        );

        appendRow(
                html,
                "SHA-256",
                emailCase.getFileHash(),
                true
        );

        appendRow(
                html,
                "Analysis Status",
                emailCase.getAnalysisStatus(),
                false
        );

        appendRow(
                html,
                "Created At",
                emailCase.getCreatedAt() != null
                        ? emailCase.getCreatedAt().format(
                                DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        )
                        : null,
                false
        );

        appendRow(
                html,
                "Threat Score",
                emailCase.getThreatScore() != null
                        ? String.valueOf(emailCase.getThreatScore())
                        : "N/A",
                false
        );

        appendRow(
                html,
                "Spoofing Risk",
                emailCase.getSpoofingRisk(),
                false
        );

        html.append("</table>");

        html.append("<div class=\"risk-box\">");

        html.append("<div class=\"score\">")
                .append(
                        emailCase.getThreatScore() != null
                                ? emailCase.getThreatScore()
                                : 0
                )
                .append("/100")
                .append("</div>");

        html.append("<p class=\"muted\">Overall threat score</p>");

        html.append("</div>");

        html.append("</div>");

        // ============================================================
        // EXECUTIVE SUMMARY / AI
        // ============================================================

        html.append("<div class=\"section\">");

        html.append("<h2>2. Executive Summary</h2>");

        if (emailCase.getAiSummary() != null
                && !emailCase.getAiSummary().isBlank()) {

            html.append("<div class=\"ai-box\">");

            html.append("<h3>AI Investigation Summary</h3>");

            html.append("<p>")
                    .append(
                            escapeHtml(
                                    emailCase.getAiSummary()
                            )
                    )
                    .append("</p>");

            html.append("</div>");

        } else {

            html.append("<div class=\"summary-box\">");

            html.append(
                    "<p>AI investigation summary is not available for this case.</p>"
            );

            html.append("</div>");
        }

        if (emailCase.getAiVerdict() != null
                || emailCase.getAiRiskLevel() != null
                || emailCase.getAiConfidence() != null) {

            html.append("<table>");

            appendRow(
                    html,
                    "AI Verdict",
                    emailCase.getAiVerdict(),
                    false
            );

            appendRow(
                    html,
                    "AI Risk Level",
                    emailCase.getAiRiskLevel(),
                    false
            );

            appendRow(
                    html,
                    "AI Confidence",
                    emailCase.getAiConfidence() != null
                            ? String.valueOf(
                                    emailCase.getAiConfidence()
                            )
                            : null,
                    false
            );

            html.append("</table>");
        }

        html.append("</div>");

        // ============================================================
        // EMAIL HEADER ANALYSIS
        // ============================================================

        html.append("<div class=\"section\">");

        html.append("<h2>3. Email Header Analysis</h2>");

        EmailHeader header = emailCase.getHeader();

        if (header != null) {

            html.append("<table>");

            appendRow(
                    html,
                    "Subject",
                    header.getSubject(),
                    false
            );

            appendRow(
                    html,
                    "From",
                    header.getSenderFrom(),
                    false
            );

            appendRow(
                    html,
                    "Reply-To",
                    header.getReplyTo(),
                    false
            );

            appendRow(
                    html,
                    "To",
                    header.getTo(),
                    false
            );

            appendRow(
                    html,
                    "CC",
                    header.getCc(),
                    false
            );

            appendRow(
                    html,
                    "Return-Path",
                    header.getReturnPath(),
                    false
            );

            appendRow(
                    html,
                    "Message-ID",
                    header.getMessageId(),
                    true
            );

            appendRow(
                    html,
                    "Date",
                    header.getDate() != null
                            ? header.getDate().toString()
                            : null,
                    false
            );

            appendRow(
                    html,
                    "SPF",
                    header.getSpfStatus(),
                    false
            );

            appendRow(
                    html,
                    "DKIM",
                    header.getDkimStatus(),
                    false
            );

            appendRow(
                    html,
                    "DMARC",
                    header.getDmarcStatus(),
                    false
            );

            html.append("</table>");

        } else {

            html.append(
                    "<p class=\"muted\">Email header information unavailable.</p>"
            );
        }

        html.append("</div>");

        // ============================================================
        // RECEIVED / ROUTING
        // ============================================================

        html.append("<div class=\"section\">");

        html.append("<h2>4. Received / Routing Analysis</h2>");

        appendRowTableStart(html);

        appendRow(
                html,
                "Originating IP",
                emailCase.getOriginatingIp(),
                true
        );

        appendRow(
                html,
                "Sender IP",
                emailCase.getSenderIp(),
                true
        );

        appendRow(
                html,
                "Sender IP Source",
                emailCase.getSenderIpSource(),
                false
        );

        appendRow(
                html,
                "Sender IP Confidence",
                emailCase.getSenderIpConfidence(),
                false
        );

        appendRow(
                html,
                "Connecting IP",
                emailCase.getConnectingIp(),
                true
        );

        appendRow(
                html,
                "Connecting IP Source",
                emailCase.getConnectingIpSource(),
                false
        );

        appendRow(
                html,
                "Connecting IP Confidence",
                emailCase.getConnectingIpConfidence(),
                false
        );

        html.append("</table>");

        if (emailCase.getReceivedHeaders() != null
                && !emailCase.getReceivedHeaders().isBlank()) {

            html.append("<h3>Received Header Chain</h3>");

            html.append("<div class=\"summary-box mono\">");

            html.append(
                    escapeHtml(
                            emailCase.getReceivedHeaders()
                    )
            );

            html.append("</div>");
        }

        html.append("</div>");

        // ============================================================
        // INFRASTRUCTURE INTELLIGENCE
        // ============================================================

        html.append("<div class=\"section\">");

        html.append("<h2>5. Infrastructure Intelligence</h2>");

        appendRowTableStart(html);

        appendRow(
                html,
                "Country",
                emailCase.getGeoCountry(),
                false
        );

        appendRow(
                html,
                "City",
                emailCase.getGeoCity(),
                false
        );

        appendRow(
                html,
                "Latitude",
                emailCase.getGeoLatitude() != null
                        ? String.valueOf(
                                emailCase.getGeoLatitude()
                        )
                        : null,
                false
        );

        appendRow(
                html,
                "Longitude",
                emailCase.getGeoLongitude() != null
                        ? String.valueOf(
                                emailCase.getGeoLongitude()
                        )
                        : null,
                false
        );

        appendRow(
                html,
                "Timezone",
                emailCase.getGeoTimezone(),
                false
        );

        html.append("</table>");

        html.append("</div>");

        // ============================================================
        // INDICATORS
        // ============================================================

        // Section wrapper
        html.append("<div class=\"section\">");

        html.append("<h2>6. IOC / Indicator Analysis</h2>");

        List<EmailIndicator> indicators =
                emailCase.getIndicators();

        if (indicators == null || indicators.isEmpty()) {

            html.append(
                    "<p class=\"muted\">No indicators were extracted from this email.</p>"
            );

        } else {

            int indicatorNumber = 1;

            for (EmailIndicator indicator : indicators) {

                html.append("<div class=\"indicator\">");

                html.append("<div class=\"indicator-title\">")
                        .append("Indicator #")
                        .append(indicatorNumber)
                        .append(" — ")
                        .append(
                                escapeHtml(
                                        indicator.getType()
                                )
                        )
                        .append("</div>");

                html.append("<table>");

                appendRow(
                        html,
                        "Type",
                        indicator.getType(),
                        false
                );

                appendRow(
                        html,
                        "Value",
                        indicator.getValue(),
                        true
                );

                appendRow(
                        html,
                        "Details",
                        indicator.getDetails(),
                        false
                );

                // VirusTotal
                appendRow(
                        html,
                        "VirusTotal Status",
                        indicator.getVirusTotalStatus(),
                        false
                );

                appendRow(
                        html,
                        "VirusTotal Malicious",
                        integerToString(
                                indicator.getVirusTotalMalicious()
                        ),
                        false
                );

                appendRow(
                        html,
                        "VirusTotal Suspicious",
                        integerToString(
                                indicator.getVirusTotalSuspicious()
                        ),
                        false
                );

                appendRow(
                        html,
                        "VirusTotal Harmless",
                        integerToString(
                                indicator.getVirusTotalHarmless()
                        ),
                        false
                );

                appendRow(
                        html,
                        "VirusTotal Undetected",
                        integerToString(
                                indicator.getVirusTotalUndetected()
                        ),
                        false
                );

                // AbuseIPDB
                appendRow(
                        html,
                        "AbuseIPDB Status",
                        indicator.getAbuseIpDbStatus(),
                        false
                );

                appendRow(
                        html,
                        "Abuse Confidence Score",
                        integerToString(
                                indicator.getAbuseConfidenceScore()
                        ),
                        false
                );

                appendRow(
                        html,
                        "Total Reports",
                        integerToString(
                                indicator.getTotalReports()
                        ),
                        false
                );

                appendRow(
                        html,
                        "Last Reported At",
                        indicator.getLastReportedAt(),
                        false
                );

                // ASN
                appendRow(
                        html,
                        "ASN",
                        indicator.getAsnNumber(),
                        false
                );

                appendRow(
                        html,
                        "ASN Organization",
                        indicator.getAsnOrg(),
                        false
                );

                // RDAP
                appendRow(
                        html,
                        "RDAP Server",
                        indicator.getRdapServer(),
                        false
                );

                appendRow(
                        html,
                        "RDAP Registry",
                        indicator.getRdapRegistry(),
                        false
                );

                appendRow(
                        html,
                        "RDAP Handle",
                        indicator.getRdapHandle(),
                        false
                );

                appendRow(
                        html,
                        "RDAP Name",
                        indicator.getRdapName(),
                        false
                );

                appendRow(
                        html,
                        "RDAP Organization",
                        indicator.getRdapOrganization(),
                        false
                );

                appendRow(
                        html,
                        "RDAP Country",
                        indicator.getRdapCountry(),
                        false
                );

                appendRow(
                        html,
                        "RDAP Start Address",
                        indicator.getRdapStartAddress(),
                        true
                );

                appendRow(
                        html,
                        "RDAP End Address",
                        indicator.getRdapEndAddress(),
                        true
                );

                appendRow(
                        html,
                        "RDAP CIDR",
                        indicator.getRdapCidr(),
                        true
                );

                html.append("</table>");

                // Close indicator div
                html.append("</div>");

                indicatorNumber++;
            }
        }

        // IMPORTANT:
        // Close Section 6 wrapper.
        html.append("</div>");

        // ============================================================
        // SPOOFING
        // ============================================================

        html.append("<div class=\"section\">");

        html.append("<h2>7. Sender Spoofing Analysis</h2>");

        appendRowTableStart(html);

        appendRow(
                html,
                "Spoofing Risk",
                emailCase.getSpoofingRisk(),
                false
        );

        appendRow(
                html,
                "Findings",
                emailCase.getSpoofingFindings(),
                true
        );

        html.append("</table>");

        html.append("</div>");

        // ============================================================
        // AI FORENSIC DETAILS
        // ============================================================

        html.append("<div class=\"section\">");

        html.append("<h2>8. AI Forensic Intelligence</h2>");

        appendAiField(
                html,
                "AI Reasoning",
                emailCase.getAiReasoning()
        );

        appendAiField(
                html,
                "AI Indicators",
                emailCase.getAiIndicators()
        );

        appendAiField(
                html,
                "Attack Techniques",
                emailCase.getAiAttackTechniques()
        );

        appendAiField(
                html,
                "AI IOCs",
                emailCase.getAiIocs()
        );

        appendAiField(
                html,
                "Origin Analysis",
                emailCase.getAiOriginAnalysis()
        );

        appendAiField(
                html,
                "RAG Threat Intelligence",
                emailCase.getAiRagInsights()
        );

        appendAiField(
                html,
                "WHOIS / RDAP Analysis",
                emailCase.getAiWhoisAnalysis()
        );

        appendAiField(
                html,
                "URL Analysis",
                emailCase.getAiUrlAnalysis()
        );

        if (emailCase.getAiError() != null
                && !emailCase.getAiError().isBlank()) {

            appendAiField(
                    html,
                    "AI Service Error",
                    emailCase.getAiError()
            );
        }

        html.append("</div>");

        // ============================================================
        // CONCLUSION
        // ============================================================

        html.append("<div class=\"section\">");

        html.append("<h2>9. Investigation Conclusion</h2>");

        html.append("<div class=\"summary-box\">");

        html.append("<p>");

        html.append(
                "This forensic report consolidates the available "
                        + "email header, routing, infrastructure, IOC, "
                        + "reputation, spoofing, and AI analysis evidence "
                        + "associated with the investigated email case."
        );

        html.append("</p>");

        html.append("<p>");

        html.append("<strong>Final Threat Score:</strong> ");

        html.append(
                emailCase.getThreatScore() != null
                        ? emailCase.getThreatScore()
                        : "N/A"
        );

        html.append("</p>");

        if (emailCase.getAiVerdict() != null) {

            html.append("<p>");

            html.append("<strong>AI Verdict:</strong> ");

            html.append(
                    escapeHtml(
                            emailCase.getAiVerdict()
                    )
            );

            html.append("</p>");
        }

        html.append("</div>");

        html.append("</div>");

        // ============================================================
        // FOOTER NOTE
        // ============================================================

        html.append("""
                <div class="footer-note">
                    This report was generated from the forensic data
                    stored for the selected email case. External
                    intelligence results may depend on the availability
                    and accuracy of third-party intelligence providers.
                </div>
                """);

        html.append("""
                </body>
                </html>
                """);

        return html.toString();
    }

    // ================================================================
    // TABLE HELPERS
    // ================================================================

    private void appendRowTableStart(
            StringBuilder html) {

        html.append("<table>");
    }

    private void appendRow(
            StringBuilder html,
            String label,
            String value,
            boolean mono) {

        html.append("<tr>");

        html.append("<td class=\"label\">")
                .append(
                        escapeHtml(
                                label
                        )
                )
                .append("</td>");

        html.append("<td>");

        if (mono) {
            html.append(
                    "<span class=\"mono\">"
            );
        }

        if (value == null || value.isBlank()) {

            html.append(
                    "<span class=\"muted\">N/A</span>"
            );

        } else {

            html.append(
                    escapeHtml(value)
            );
        }

        if (mono) {
            html.append("</span>");
        }

        html.append("</td>");

        html.append("</tr>");
    }

    private void appendAiField(
            StringBuilder html,
            String label,
            String value) {

        if (value == null || value.isBlank()) {
            return;
        }

        html.append("<div class=\"ai-box\">");

        html.append("<h3>")
                .append(
                        escapeHtml(label)
                )
                .append("</h3>");

        html.append("<div class=\"mono\">")
                .append(
                        escapeHtml(value)
                )
                .append("</div>");

        html.append("</div>");
    }

    // ================================================================
    // UTILITY HELPERS
    // ================================================================

    private String integerToString(
            Integer value) {

        return value != null
                ? String.valueOf(value)
                : null;
    }

    private String escapeHtml(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}