package com.stratyon.backend.domain.report.dto;

import java.util.UUID;

public record RecommendationDto(UUID id, String priority, String title, String detail, String effort, String impact) {}
