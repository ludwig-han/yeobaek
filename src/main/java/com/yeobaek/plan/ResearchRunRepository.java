package com.yeobaek.plan;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResearchRunRepository extends JpaRepository<ResearchRun,String> {
    Optional<ResearchRun> findFirstByPlanIdOrderByStartedAtDesc(String planId);
    long countByStartedAtAfter(Instant since);
}
