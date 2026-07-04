package com.stratyon.backend.domain.firm;

import com.stratyon.backend.domain.auth.User;
import com.stratyon.backend.domain.auth.UserRepository;
import com.stratyon.backend.domain.firm.dto.*;
import com.stratyon.backend.domain.report.Report;
import com.stratyon.backend.domain.report.ReportRepository;
import com.stratyon.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FirmService {

    private final FirmRepository firmRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    @Transactional
    public Firm createFirm(CreateFirmRequest req) {
        User user = currentUser();
        Firm firm = Firm.builder()
                .name(req.name())
                .industry(req.industry())
                .size(req.size())
                .goals(req.goals() != null ? req.goals() : List.of())
                .build();
        firm = firmRepository.save(firm);
        user.setFirm(firm);
        userRepository.save(user);
        return firm;
    }

    public Firm getMyFirm() {
        return currentUser().getFirm();
    }

    @Transactional
    public Firm updateMyFirm(UpdateFirmRequest req) {
        Firm firm = requireFirm();
        if (req.name() != null) firm.setName(req.name());
        if (req.industry() != null) firm.setIndustry(req.industry());
        if (req.size() != null) firm.setSize(req.size());
        if (req.goals() != null) firm.setGoals(req.goals());
        return firmRepository.save(firm);
    }

    public FirmMetricsResponse getMetrics() {
        Firm firm = requireFirm();
        List<Report> completed = reportRepository.findByFirmIdAndStatusOrderByCompletedAtDesc(
                firm.getId(), "completed");

        long reportCount = reportRepository.countByFirmId(firm.getId());
        int overallScore = completed.isEmpty() ? 0
                : (int) completed.stream().mapToInt(r -> r.getScore() != null ? r.getScore() : 0).average().orElse(0);

        String trend = "stable";
        double trendPercent = 0;
        if (completed.size() >= 2) {
            int latest = completed.get(0).getScore() != null ? completed.get(0).getScore() : 0;
            int previous = completed.get(1).getScore() != null ? completed.get(1).getScore() : 0;
            trendPercent = previous == 0 ? 0 : Math.round(((double)(latest - previous) / previous) * 1000.0) / 10.0;
            trend = trendPercent > 0 ? "up" : trendPercent < 0 ? "down" : "stable";
        }

        // Build efficiency index (simple proxy: % of reports completed)
        long totalReports = reportCount;
        long completedCount = completed.size();
        int efficiencyIndex = totalReports == 0 ? 100 : (int) ((completedCount * 100.0) / totalReports);

        String riskLevel = overallScore >= 75 ? "Low" : overallScore >= 50 ? "Medium" : "High";
        String riskStatus = overallScore >= 75 ? "good" : overallScore >= 50 ? "warning" : "critical";

        List<KpiDto> kpis = new ArrayList<>();
        kpis.add(new KpiDto("Overall Score", overallScore, "/100", trendPercent, overallScore >= 75 ? "good" : overallScore >= 50 ? "warning" : "critical"));
        kpis.add(new KpiDto("Reports Completed", (int) completedCount, null, null, "good"));
        kpis.add(new KpiDto("Risk Level", riskLevel, null, null, riskStatus));
        kpis.add(new KpiDto("Efficiency Index", efficiencyIndex, "%", null, efficiencyIndex >= 75 ? "good" : "warning"));

        return new FirmMetricsResponse(
                overallScore,
                trend,
                trendPercent,
                reportCount,
                completed.isEmpty() ? null : completed.get(0).getCompletedAt(),
                kpis
        );
    }

    private Firm requireFirm() {
        Firm firm = currentUser().getFirm();
        if (firm == null) throw new ResourceNotFoundException("Firm not found for current user.");
        return firm;
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
