package com.stratyon.backend.domain.report;

import com.stratyon.backend.domain.report.dto.CreateReportRequest;
import com.stratyon.backend.domain.report.dto.ReportDetailResponse;
import com.stratyon.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Report>>> listReports() {
        return ResponseEntity.ok(ApiResponse.of(reportService.listReports()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Report>> createReport(@Valid @RequestBody CreateReportRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(reportService.createReport(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Report>> getReport(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(reportService.getReport(id)));
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<ApiResponse<ReportDetailResponse>> getDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(reportService.getDetail(id)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<Map<String, String>>> getDownloadUrl(@PathVariable UUID id) {
        String url = reportService.getDownloadUrl(id);
        return ResponseEntity.ok(ApiResponse.of(Map.of("url", url)));
    }
}
