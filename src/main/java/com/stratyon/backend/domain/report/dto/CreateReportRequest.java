package com.stratyon.backend.domain.report.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateReportRequest(
        @NotBlank String title,
        List<String> tags
) {}
