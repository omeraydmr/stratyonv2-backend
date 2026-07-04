package com.stratyon.backend.infrastructure.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class GoogleAnalyticsClient {

    @Value("${app.google.analytics.property-id:}")
    private String propertyId;

    @Value("${app.google.analytics.service-account-json:}")
    private String serviceAccountJsonPath;

    private static final String BASE_URL = "https://analyticsdata.googleapis.com/v1beta";
    private static final List<String> SCOPES = List.of("https://www.googleapis.com/auth/analytics.readonly");

    public Map<String, Object> fetchMetrics() {
        if (propertyId.isBlank() || serviceAccountJsonPath.isBlank()) {
            log.debug("GA4 credentials not configured, skipping.");
            return placeholder();
        }

        try {
            String token = getAccessToken();
            RestClient client = RestClient.builder()
                    .baseUrl(BASE_URL)
                    .defaultHeader("Authorization", "Bearer " + token)
                    .build();

            Map<String, Object> body = Map.of(
                    "metrics", List.of(
                            Map.of("name", "sessions"),
                            Map.of("name", "totalUsers"),
                            Map.of("name", "conversions")
                    ),
                    "dateRanges", List.of(Map.of("startDate", "30daysAgo", "endDate", "today"))
            );

            JsonNode response = client.post()
                    .uri("/properties/{propertyId}:runReport", propertyId)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            return parseResponse(response);
        } catch (Exception e) {
            log.error("GA4 API call failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String getAccessToken() throws Exception {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new FileInputStream(serviceAccountJsonPath))
                .createScoped(SCOPES);
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }

    private Map<String, Object> parseResponse(JsonNode response) {
        Map<String, Object> data = new HashMap<>();
        if (response == null || !response.has("rows")) return placeholder();

        JsonNode rows = response.get("rows");
        if (rows.isArray() && rows.size() > 0) {
            JsonNode values = rows.get(0).get("metricValues");
            long sessions = values.get(0).get("value").asLong(0);
            long users = values.get(1).get("value").asLong(0);
            long conversions = values.get(2).get("value").asLong(0);
            double conversionRate = sessions > 0 ? (conversions * 100.0 / sessions) : 0;

            data.put("GA4_SESSIONS", sessions);
            data.put("GA4_USERS", users);
            data.put("GA4_CONVERSIONS", conversions);
            data.put("GA4_CONVERSION_RATE", String.format("%.2f", conversionRate));
        }
        return data.isEmpty() ? placeholder() : data;
    }

    private Map<String, Object> placeholder() {
        return Map.of("GA4_SESSIONS", "N/A", "GA4_USERS", "N/A",
                "GA4_CONVERSIONS", "N/A", "GA4_CONVERSION_RATE", "0");
    }
}
