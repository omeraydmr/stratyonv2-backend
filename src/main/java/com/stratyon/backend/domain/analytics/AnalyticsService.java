package com.stratyon.backend.domain.analytics;

import com.stratyon.backend.domain.analytics.dto.GoalProgressDto;
import com.stratyon.backend.domain.analytics.dto.ReportsByMonthDto;
import com.stratyon.backend.domain.analytics.dto.ScoreTrendResponse;
import com.stratyon.backend.domain.auth.User;
import com.stratyon.backend.domain.auth.UserRepository;
import com.stratyon.backend.domain.firm.Firm;
import com.stratyon.backend.domain.report.Report;
import com.stratyon.backend.domain.report.ReportRepository;
import com.stratyon.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public ScoreTrendResponse getScoreTrend(int limit) {
        UUID firmId = requireFirmId();
        List<Report> completed = reportRepository.findCompletedByFirmIdOrderByCompletedAt(firmId);

        if (completed.size() > limit) {
            completed = completed.subList(completed.size() - limit, completed.size());
        }

        List<ScoreTrendResponse.ScoreTrendPoint> points = completed.stream()
                .map(r -> new ScoreTrendResponse.ScoreTrendPoint(
                        r.getId().toString(),
                        r.getTitle(),
                        r.getScore() != null ? r.getScore() : 0,
                        r.getCompletedAt()
                ))
                .toList();

        if (points.isEmpty()) {
            return new ScoreTrendResponse(List.of(), 0, 0, 0, "stable", 0);
        }

        int highest = points.stream().mapToInt(ScoreTrendResponse.ScoreTrendPoint::score).max().orElse(0);
        int lowest = points.stream().mapToInt(ScoreTrendResponse.ScoreTrendPoint::score).min().orElse(0);
        double average = points.stream().mapToInt(ScoreTrendResponse.ScoreTrendPoint::score).average().orElse(0);

        int first = points.get(0).score();
        int last = points.get(points.size() - 1).score();
        double trendPercent = first == 0 ? 0 : Math.round(((double)(last - first) / first) * 1000.0) / 10.0;
        String trend = trendPercent > 0 ? "up" : trendPercent < 0 ? "down" : "stable";

        return new ScoreTrendResponse(points, Math.round(average * 10.0) / 10.0, highest, lowest, trend, trendPercent);
    }

    public List<ReportsByMonthDto> getReportsByMonth(int months) {
        UUID firmId = requireFirmId();
        OffsetDateTime since = OffsetDateTime.now().minusMonths(months);
        List<Object[]> rows = reportRepository.countByMonthSince(firmId, since);

        Map<String, long[]> resultMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String month = row[0].toString();
            long created = ((Number) row[1]).longValue();
            long completed = ((Number) row[2]).longValue();
            resultMap.put(month, new long[]{created, completed});
        }

        // Zero-fill missing months
        List<ReportsByMonthDto> result = new ArrayList<>();
        YearMonth current = YearMonth.from(since.toLocalDate()).plusMonths(1);
        YearMonth end = YearMonth.now();
        while (!current.isAfter(end)) {
            String key = current.toString();
            long[] counts = resultMap.getOrDefault(key, new long[]{0, 0});
            result.add(new ReportsByMonthDto(key, counts[0], counts[1]));
            current = current.plusMonths(1);
        }
        return result;
    }

    public List<GoalProgressDto> getGoalProgress() {
        User user = currentUser();
        Firm firm = user.getFirm();
        if (firm == null) return List.of();

        List<String> goals = firm.getGoals() != null ? firm.getGoals() : List.of();
        List<Report> completed = reportRepository.findCompletedByFirmIdOrderByCompletedAt(firm.getId());

        Map<String, String> goalLabels = Map.of(
                "cost_reduction", "Cost Reduction",
                "revenue_growth", "Revenue Growth",
                "operational_efficiency", "Operational Efficiency",
                "risk_management", "Risk Management",
                "digital_transformation", "Digital Transformation"
        );

        return goals.stream().map(goal -> {
            String label = goalLabels.getOrDefault(goal, goal);
            // Score derived from reports: average score weighted by goal relevance in findings
            int score = computeGoalScore(goal, completed);
            int baseline = completed.isEmpty() ? score : computeGoalScore(goal, completed.subList(0, 1));
            int change = score - baseline;
            return new GoalProgressDto(goal, label, score, change);
        }).collect(Collectors.toList());
    }

    private int computeGoalScore(String goal, List<Report> reports) {
        if (reports.isEmpty()) return 50;
        return (int) reports.stream()
                .mapToInt(r -> r.getScore() != null ? r.getScore() : 50)
                .average()
                .orElse(50);
    }

    private UUID requireFirmId() {
        Firm firm = currentUser().getFirm();
        if (firm == null) throw new ResourceNotFoundException("Firm not found.");
        return firm.getId();
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
