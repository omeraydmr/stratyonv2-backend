package com.stratyon.backend.domain.report;

import com.stratyon.backend.infrastructure.claude.ClaudeClient;
import com.stratyon.backend.infrastructure.datasource.DataSourceAggregatorService;
import com.stratyon.backend.infrastructure.s3.S3Service;
import com.stratyon.backend.infrastructure.template.TemplateFillerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationService {

    private final ReportRepository reportRepository;
    private final S3Service s3Service;
    private final TemplateFillerService templateFillerService;
    private final DataSourceAggregatorService dataSourceAggregatorService;
    private final ClaudeClient claudeClient;

    @Async("reportExecutor")
    @Transactional
    public void generateAsync(UUID reportId) {
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) return;

        try {
            report.setStatus("in_progress");
            reportRepository.save(report);

            // 1. Fetch data from external APIs
            Map<String, Object> data = dataSourceAggregatorService.aggregate(report.getFirm().getId());
            data.put("COMPANY_NAME", report.getFirm().getName());
            data.put("REPORT_DATE", OffsetDateTime.now().toLocalDate().toString());
            data.put("REPORT_TITLE", report.getTitle());

            // 2. Fill DOCX template and convert to PDF
            byte[] pdfBytes = templateFillerService.generatePdf(data);

            // 3. Compute score from data
            int score = computeScore(data);

            // 4. Generate findings and recommendations via Claude
            List<Finding> findings = claudeClient.generateFindings(report, data);
            List<Recommendation> recommendations = claudeClient.generateRecommendations(report, data, score);

            for (Finding f : findings) f.setReport(report);
            for (Recommendation r : recommendations) r.setReport(report);

            // 5. Upload PDF to S3
            String s3Key = "reports/" + report.getFirm().getId() + "/" + reportId + ".pdf";
            s3Service.upload(pdfBytes, s3Key);

            // 6. Build summary
            String summary = buildSummary(findings, score);

            // 7. Persist results
            report.setStatus("completed");
            report.setScore(score);
            report.setSummary(summary);
            report.setS3Key(s3Key);
            report.setCompletedAt(OffsetDateTime.now());
            report.getFindings().addAll(findings);
            report.getRecommendations().addAll(recommendations);
            reportRepository.save(report);

            log.info("Report {} generated successfully with score {}", reportId, score);

        } catch (Exception e) {
            log.error("Report generation failed for {}: {}", reportId, e.getMessage(), e);
            report.setStatus("failed");
            reportRepository.save(report);
        }
    }

    private int computeScore(Map<String, Object> data) {
        int score = 50; // baseline
        try {
            double conversionRate = parseDouble(data.get("GA4_CONVERSION_RATE"));
            double roas = parseDouble(data.get("GADS_ROAS"));
            double cpr = parseDouble(data.get("META_CPR"));

            // Simple weighted formula
            if (conversionRate > 3) score += 15;
            else if (conversionRate > 1) score += 8;

            if (roas > 4) score += 20;
            else if (roas > 2) score += 10;

            if (cpr > 0 && cpr < 10) score += 15;
            else if (cpr < 20) score += 5;

            score = Math.min(100, Math.max(0, score));
        } catch (Exception ignored) {}
        return score;
    }

    private String buildSummary(List<Finding> findings, int score) {
        long positives = findings.stream().filter(f -> "positive".equals(f.getSeverity())).count();
        long warnings = findings.stream().filter(f -> "warning".equals(f.getSeverity())).count();
        long criticals = findings.stream().filter(f -> "critical".equals(f.getSeverity())).count();
        return String.format("Overall performance score: %d/100. Found %d positive indicators, %d areas needing attention, and %d critical issues.",
                score, positives, warnings, criticals);
    }

    private double parseDouble(Object val) {
        if (val == null) return 0;
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0; }
    }
}
