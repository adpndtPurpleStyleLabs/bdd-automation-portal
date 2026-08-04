package com.bdd.portal.repository;

import com.bdd.portal.entity.ScenarioExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface ScenarioExecutionRepository extends JpaRepository<ScenarioExecution, Long> {
    List<ScenarioExecution> findByFeatureExecutionId(Long featureExecutionId);
    Page<ScenarioExecution> findByFeatureExecutionFeatureNameAndScenarioNameOrderByStartTimeDesc(String featureName, String scenarioName, Pageable pageable);
    
    Optional<ScenarioExecution> findByScenarioExecutionUuid(String uuid);
    
    List<ScenarioExecution> findByExecutionId(Long executionId);

    @Modifying
    @Query(value = "UPDATE scenario_execution SET status = 'RUNNING', worker_id = :workerId, start_time = NOW() WHERE status = 'QUEUED' AND worker_id IS NULL ORDER BY id ASC LIMIT 1", nativeQuery = true)
    int lockNextQueuedScenario(@Param("workerId") String workerId);

    @Query(value = "SELECT * FROM scenario_execution WHERE worker_id = :workerId AND status = 'RUNNING' ORDER BY start_time DESC LIMIT 1", nativeQuery = true)
    Optional<ScenarioExecution> findLockedScenarioByWorker(@Param("workerId") String workerId);
}
