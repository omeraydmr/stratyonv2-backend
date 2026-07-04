package com.stratyon.backend.infrastructure.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class GoogleAdsClient {

    @Value("${app.google.ads.developer-token:}")
    private String developerToken;

    @Value("${app.google.ads.client-id:}")
    private String clientId;

    @Value("${app.google.ads.client-secret:}")
    private String clientSecret;

    @Value("${app.google.ads.refresh-token:}")
    private String refreshToken;

    @Value("${app.google.ads.customer-id:}")
    private String customerId;

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String ADS_BASE_URL = "https://googleads.googleapis.com/v17";

    public Map<String, Object> fetchMetrics() {
        if (developerToken.isBlank() || customerId.isBlank()) {
            log.debug("Google Ads credentials not configured, skipping.");
            return placeholder();
        }

        try {
            String accessToken = refreshAccessToken();
            RestClient client = RestClient.builder()
                    .baseUrl(ADS_BASE_URL)
                    .defaultHeader("Authorization", "Bearer " + accessToken)
                    .defaultHeader("developer-token", developerToken)
                    .build();

            String query = """
                    SELECT
                      metrics.cost_micros,
                      metrics.impressions,
                      metrics.clicks,
                      metrics.conversions_value,
                      metrics.conversions
                    FROM campaign
                    WHERE segments.date DURING LAST_30_DAYS
                    """;

            Map<String, Object> body = Map.of("query", query);

            JsonNode response = client.post()
                    .uri("/customers/{customerId}/googleAds:search", customerId)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            return parseResponse(response);
        } catch (Exception e) {
            log.error("Google Ads API call failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private String refreshAccessToken() {
        RestClient tokenClient = RestClient.create();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", refreshToken);
        form.add("grant_type", "refresh_token");

        JsonNode response = tokenClient.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("access_token")) {
            throw new RuntimeException("Failed to refresh Google Ads access token");
        }
        return response.get("access_token").asText();
    }

    private Map<String, Object> parseResponse(JsonNode response) {
        Map<String, Object> data = new HashMap<>();
        if (response == null || !response.has("results")) return placeholder();

        long totalCostMicros = 0;
        long totalImpressions = 0;
        long totalClicks = 0;
        double totalConversionsValue = 0;

        for (JsonNode result : response.get("results")) {
            JsonNode metrics = result.get("metrics");
            if (metrics != null) {
                totalCostMicros += metrics.path("costMicros").asLong(0);
                totalImpressions += metrics.path("impressions").asLong(0);
                totalClicks += metrics.path("clicks").asLong(0);
                totalConversionsValue += metrics.path("conversionsValue").asDouble(0);
            }
        }

        double spend = totalCostMicros / 1_000_000.0;
        double roas = spend > 0 ? totalConversionsValue / spend : 0;

        data.put("GADS_SPEND", String.format("%.2f", spend));
        data.put("GADS_IMPRESSIONS", totalImpressions);
        data.put("GADS_CLICKS", totalClicks);
        data.put("GADS_CONVERSIONS_VALUE", String.format("%.2f", totalConversionsValue));
        data.put("GADS_ROAS", String.format("%.2f", roas));
        return data;
    }

    private Map<String, Object> placeholder() {
        return Map.of("GADS_SPEND", "N/A", "GADS_IMPRESSIONS", "N/A",
                "GADS_CLICKS", "N/A", "GADS_ROAS", "0");
    }
}
