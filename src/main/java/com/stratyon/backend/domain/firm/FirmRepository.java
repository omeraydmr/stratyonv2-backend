package com.stratyon.backend.domain.firm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FirmRepository extends JpaRepository<Firm, UUID> {}
