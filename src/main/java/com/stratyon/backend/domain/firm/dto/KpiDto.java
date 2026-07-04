package com.stratyon.backend.domain.firm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KpiDto(
        String label,
        Object value,
        String unit,
        Double change,
        String status
) {}
