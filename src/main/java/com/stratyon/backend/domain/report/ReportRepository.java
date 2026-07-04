package com.stratyon.backend.domain.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    List<Report> findByFirmIdOrderByCreatedAtDesc(UUID firmId);

    List<Report> findByFirmIdAndStatusOrderByCompletedAtDesc(UUID firmId, String status);

    long countByFirmId(UUID firmId);

    Optional<Report> findByIdAndFirmId(UUID id, UUID firmId);

    @Query("SELECT r FROM Report r WHERE r.firm.id = :firmId AND r.status = 'completed' ORDER BY r.completedAt ASC")
    List<Report> findCompletedByFirmIdOrderByCompletedAt(UUID firmId);

    @Query(value = """
            SELECT TO_CHAR(DATE_TRUNC('month', created_at), 'YYYY-MM') AS month,
                   COUNT(*) AS created,
                   COUNT(CASE WHEN status = 'completed' THEN 1 END) AS completed
            FROM reports
            WHERE firm_id = :firmId
              AND created_at >= :since
            GROUP BY DATE_TRUNC('month', created_at)
            ORDER BY DATE_TRUNC('month', created_at)
            """, nativeQuery = true)
    List<Object[]> countByMonthSince(UUID firmId, OffsetDateTime since);
}
