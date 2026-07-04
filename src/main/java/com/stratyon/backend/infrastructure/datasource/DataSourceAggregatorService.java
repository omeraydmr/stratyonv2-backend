package com.stratyon.backend.infrastructure.datasource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSourceAggregatorService {

    private final GoogleAnalyticsClient googleAnalyticsClient;
    private final GoogleAdsClient googleAdsClient;
    private final MetaAdsClient metaAdsClient;

    public Map<String, Object> aggregate(UUID firmId) {
        Map<String, Object> data = new HashMap<>();

        try {
            data.putAll(googleAnalyticsClient.fetchMetrics());
        } catch (Exception e) {
            log.warn("GA4 fetch failed: {}", e.getMessage());
            data.putAll(ga4Defaults());
        }

        try {
            data.putAll(googleAdsClient.fetchMetrics());
        } catch (Exception e) {
            log.warn("Google Ads fetch failed: {}", e.getMessage());
            data.putAll(gadsDefaults());
        }

        try {
            data.putAll(metaAdsClient.fetchMetrics());
        } catch (Exception e) {
            log.warn("Meta Ads fetch failed: {}", e.getMessage());
            data.putAll(metaDefaults());
        }

        return data;
    }

    private Map<String, Object> ga4Defaults() {
        return Map.of("GA4_SESSIONS", "N/A", "GA4_USERS", "N/A", "GA4_CONVERSION_RATE", "0");
    }

    private Map<String, Object> gadsDefaults() {
        return Map.of("GADS_SPEND", "N/A", "GADS_IMPRESSIONS", "N/A", "GADS_ROAS", "0");
    }

    private Map<String, Object> metaDefaults() {
        return Map.of("META_REACH", "N/A", "META_SPEND", "N/A", "META_CPR", "0");
    }
}
