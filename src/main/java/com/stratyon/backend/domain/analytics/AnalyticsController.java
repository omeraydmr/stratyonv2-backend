package com.stratyon.backend.domain.analytics;

import com.stratyon.backend.domain.analytics.dto.GoalProgressDto;
import com.stratyon.backend.domain.analytics.dto.ReportsByMonthDto;
import com.stratyon.backend.domain.analytics.dto.ScoreTrendResponse;
import com.stratyon.backend.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/score-trend")
    public ResponseEntity<ApiResponse<ScoreTrendResponse>> scoreTrend(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.of(analyticsService.getScoreTrend(limit)));
    }

    @GetMapping("/reports-by-month")
    public ResponseEntity<ApiResponse<List<ReportsByMonthDto>>> reportsByMonth(
            @RequestParam(defaultValue = "12") int months) {
        return ResponseEntity.ok(ApiResponse.of(analyticsService.getReportsByMonth(months)));
    }

    @GetMapping("/goal-progress")
    public ResponseEntity<ApiResponse<List<GoalProgressDto>>> goalProgress() {
        return ResponseEntity.ok(ApiResponse.of(analyticsService.getGoalProgress()));
    }
}
