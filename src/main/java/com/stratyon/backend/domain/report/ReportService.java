package com.stratyon.backend.domain.report;

import com.stratyon.backend.domain.auth.User;
import com.stratyon.backend.domain.auth.UserRepository;
import com.stratyon.backend.domain.report.dto.*;
import com.stratyon.backend.infrastructure.s3.S3Service;
import com.stratyon.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReportGenerationService reportGenerationService;
    private final S3Service s3Service;

    public List<Report> listReports() {
        return reportRepository.findByFirmIdOrderByCreatedAtDesc(currentFirmId());
    }

    @Transactional
    public Report createReport(CreateReportRequest req) {
        User user = currentUser();
        if (user.getFirm() == null) throw new IllegalArgumentException("Complete the firm survey first.");

        Report report = Report.builder()
                .firm(user.getFirm())
                .title(req.title())
                .status("pending")
                .tags(req.tags() != null ? req.tags() : List.of())
                .build();
        report = reportRepository.save(report);

        reportGenerationService.generateAsync(report.getId());
        return report;
    }

    public Report getReport(UUID id) {
        return reportRepository.findByIdAndFirmId(id, currentFirmId())
                .orElseThrow(() -> new ResourceNotFoundException("Report not found."));
    }

    public ReportDetailResponse getDetail(UUID id) {
        Report report = getReport(id);
        if (!"completed".equals(report.getStatus())) {
            throw new ResourceNotFoundException("Report is not yet completed.");
        }
        List<FindingDto> findings = report.getFindings().stream()
                .map(f -> new FindingDto(f.getId(), f.getSeverity(), f.getTitle(), f.getDetail()))
                .toList();
        List<RecommendationDto> recommendations = report.getRecommendations().stream()
                .map(r -> new RecommendationDto(r.getId(), r.getPriority(), r.getTitle(), r.getDetail(), r.getEffort(), r.getImpact()))
                .toList();
        return new ReportDetailResponse(findings, recommendations);
    }

    public String getDownloadUrl(UUID id) {
        Report report = getReport(id);
        if (report.getS3Key() == null) throw new ResourceNotFoundException("Report file not available yet.");
        return s3Service.generatePresignedUrl(report.getS3Key());
    }

    private UUID currentFirmId() {
        return currentUser().getFirm() != null ? currentUser().getFirm().getId() : null;
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
