package com.stratyon.backend.infrastructure.claude;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stratyon.backend.domain.report.Finding;
import com.stratyon.backend.domain.report.Recommendation;
import com.stratyon.backend.domain.report.Report;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ClaudeClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.claude.model}")
    private String model;

    @Value("${app.claude.max-tokens}")
    private int maxTokens;

    public ClaudeClient(@Value("${app.claude.api-key}") String apiKey,
                        @Value("${app.claude.base-url}") String baseUrl,
                        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String complete(String systemPrompt, List<ClaudeRequest.Message> messages) {
        ClaudeRequest req = new ClaudeRequest(model, maxTokens, systemPrompt, messages);
        try {
            ClaudeResponse response = restClient.post()
                    .uri("/v1/messages")
                    .body(req)
                    .retrieve()
                    .body(ClaudeResponse.class);
            return response != null ? response.firstText() : "";
        } catch (Exception e) {
            log.error("Claude API call failed: {}", e.getMessage());
            return "";
        }
    }

    public List<Finding> generateFindings(Report report, Map<String, Object> data) {
        String prompt = buildFindingsPrompt(report, data);
        String response = complete(
                "You are a strategic business analyst. Return a JSON array of findings.",
                List.of(new ClaudeRequest.Message("user", prompt))
        );
        return parseFindingsFromJson(response);
    }

    public List<Recommendation> generateRecommendations(Report report, Map<String, Object> data, int score) {
        String prompt = buildRecommendationsPrompt(report, data, score);
        String response = complete(
                "You are a strategic business consultant. Return a JSON array of recommendations.",
                List.of(new ClaudeRequest.Message("user", prompt))
        );
        return parseRecommendationsFromJson(response);
    }

    private String buildFindingsPrompt(Report report, Map<String, Object> data) {
        return """
                Analyze the following marketing performance data for company "%s" and generate 3-5 findings.
                Each finding must have: severity (positive/warning/critical), title (short), detail (1-3 sentences).
                Return ONLY a JSON array: [{"severity":"...", "title":"...", "detail":"..."}, ...]

                Data: %s
                """.formatted(report.getFirm().getName(), data.toString());
    }

    private String buildRecommendationsPrompt(Report report, Map<String, Object> data, int score) {
        return """
                Based on the marketing data for "%s" (overall score: %d/100), generate 3-5 strategic recommendations.
                Each must have: priority (high/medium/low), title (short), detail (actionable description),
                effort (High/Medium/Low), impact (High/Medium/Low).
                Return ONLY a JSON array: [{"priority":"...", "title":"...", "detail":"...", "effort":"...", "impact":"..."}, ...]

                Data: %s
                """.formatted(report.getFirm().getName(), score, data.toString());
    }

    @SuppressWarnings("unchecked")
    private List<Finding> parseFindingsFromJson(String json) {
        List<Finding> findings = new ArrayList<>();
        try {
            String cleaned = extractJsonArray(json);
            List<Map<String, String>> list = objectMapper.readValue(cleaned, List.class);
            for (Map<String, String> item : list) {
                findings.add(Finding.builder()
                        .severity(item.getOrDefault("severity", "warning"))
                        .title(item.getOrDefault("title", ""))
                        .detail(item.getOrDefault("detail", ""))
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to parse Claude findings: {}", e.getMessage());
            findings.add(Finding.builder().severity("warning").title("Analysis pending")
                    .detail("AI analysis could not be parsed. Please review manually.").build());
        }
        return findings;
    }

    @SuppressWarnings("unchecked")
    private List<Recommendation> parseRecommendationsFromJson(String json) {
        List<Recommendation> recs = new ArrayList<>();
        try {
            String cleaned = extractJsonArray(json);
            List<Map<String, String>> list = objectMapper.readValue(cleaned, List.class);
            for (Map<String, String> item : list) {
                recs.add(Recommendation.builder()
                        .priority(item.getOrDefault("priority", "medium"))
                        .title(item.getOrDefault("title", ""))
                        .detail(item.getOrDefault("detail", ""))
                        .effort(item.getOrDefault("effort", "Medium"))
                        .impact(item.getOrDefault("impact", "Medium"))
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to parse Claude recommendations: {}", e.getMessage());
        }
        return recs;
    }

    private String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return "[]";
    }
}
