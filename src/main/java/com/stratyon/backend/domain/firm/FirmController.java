package com.stratyon.backend.domain.firm;

import com.stratyon.backend.domain.firm.dto.CreateFirmRequest;
import com.stratyon.backend.domain.firm.dto.FirmMetricsResponse;
import com.stratyon.backend.domain.firm.dto.UpdateFirmRequest;
import com.stratyon.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/firms")
@RequiredArgsConstructor
public class FirmController {

    private final FirmService firmService;

    @PostMapping
    public ResponseEntity<ApiResponse<Firm>> createFirm(@Valid @RequestBody CreateFirmRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(firmService.createFirm(req)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Firm>> getMyFirm() {
        return ResponseEntity.ok(ApiResponse.of(firmService.getMyFirm()));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Firm>> updateMyFirm(@RequestBody UpdateFirmRequest req) {
        return ResponseEntity.ok(ApiResponse.of(firmService.updateMyFirm(req)));
    }

    @GetMapping("/me/metrics")
    public ResponseEntity<ApiResponse<FirmMetricsResponse>> getMetrics() {
        return ResponseEntity.ok(ApiResponse.of(firmService.getMetrics()));
    }
}
