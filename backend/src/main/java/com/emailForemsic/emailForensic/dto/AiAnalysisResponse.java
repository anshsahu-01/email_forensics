package com.emailForemsic.emailForensic.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AiAnalysisResponse {

    private Double threatScore;

    private String riskLevel;

    private String verdict;

    private Double confidence;

    private List<Object> indicators;

    private List<Object> attackTechniques;

    private Map<String, Object> iocs;

    private Map<String, Object> originAnalysis;

    private List<Object> evidence;

    private List<String> reasoning;

    private String summary;

    private String ragInsights;

    private Map<String, Object> whoisAnalysis;

    private Map<String, Object> urlAnalysis;

    private Map<String, Object> graphs;

    private String error;
}