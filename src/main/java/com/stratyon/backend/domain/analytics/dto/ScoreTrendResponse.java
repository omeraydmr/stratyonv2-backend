package com.stratyon.backend.domain.analytics.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ScoreTrendResponse(
        List<ScoreTrendPoint> points,
        double average,
        int highest,
        int lowest,
        String trend,
        double trendPercent
) {
    public record ScoreTrendPoint(
            String reportId,
            String reportTitle,
            int score,
            OffsetDateTime completedAt
    ) {}
}
