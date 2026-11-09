package com.pawmotionai.aiinsights.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InsightJpaRepository extends JpaRepository<InsightJpaEntity, UUID> {
}
