package com.stratyon.backend.domain.report.dto;

import java.util.List;

public record ReportDetailResponse(
        List<FindingDto> findings,
        List<RecommendationDto> recommendations
) {}
