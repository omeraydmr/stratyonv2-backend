package com.stratyon.backend.infrastructure.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class MetaAdsClient {

    @Value("${app.meta.access-token:}")
    private String accessToken;

    @Value("${app.meta.ad-account-id:}")
    private String adAccountId;

    @Value("${app.meta.api-version:v20.0}")
    private String apiVersion;

    private static final String BASE_URL = "https://graph.facebook.com";

    public Map<String, Object> fetchMetrics() {
        if (accessToken.isBlank() || adAccountId.isBlank()) {
            log.debug("Meta Ads credentials not configured, skipping.");
            return placeholder();
        }

        try {
            RestClient client = RestClient.builder()
                    .baseUrl(BASE_URL)
                    .build();

            JsonNode response = client.get()
                    .uri("/{version}/{adAccountId}/insights?fields=reach,spend,cost_per_result&date_preset=last_30d&access_token={token}",
                            apiVersion, adAccountId, accessToken)
                    .retrieve()
                    .body(JsonNode.class);

            return parseResponse(response);
        } catch (Exception e) {
            log.error("Meta Ads API call failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> parseResponse(JsonNode response) {
        Map<String, Object> data = new HashMap<>();
        if (response == null || !response.has("data")) return placeholder();

        JsonNode dataArray = response.get("data");
        if (!dataArray.isArray() || dataArray.isEmpty()) return placeholder();

        long totalReach = 0;
        double totalSpend = 0;
        double totalCpr = 0;
        int count = 0;

        for (JsonNode item : dataArray) {
            totalReach += item.path("reach").asLong(0);
            totalSpend += item.path("spend").asDouble(0);
            JsonNode cpr = item.path("cost_per_result");
            if (!cpr.isMissingNode()) {
                totalCpr += cpr.asDouble(0);
                count++;
            }
        }

        data.put("META_REACH", totalReach);
        data.put("META_SPEND", String.format("%.2f", totalSpend));
        data.put("META_CPR", count > 0 ? String.format("%.2f", totalCpr / count) : "N/A");
        return data;
    }

    private Map<String, Object> placeholder() {
        return Map.of("META_REACH", "N/A", "META_SPEND", "N/A", "META_CPR", "0");
    }
}
