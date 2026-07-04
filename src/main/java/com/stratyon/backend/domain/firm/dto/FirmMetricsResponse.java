package com.stratyon.backend.domain.firm.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record FirmMetricsResponse(
        int overallScore,
        String trend,
        double trendPercent,
        long reportCount,
        OffsetDateTime lastReportDate,
        List<KpiDto> kpis
) {}
