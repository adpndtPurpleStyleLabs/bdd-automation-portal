package com.bdd.portal.repository;

import com.bdd.portal.entity.ScenarioExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface ScenarioExecutionRepository extends JpaRepository<ScenarioExecution, Long> {
    List<ScenarioExecution> findByFeatureExecutionId(Long featureExecutionId);
    Page<ScenarioExecution> findByFeatureExecutionFeatureNameAndScenarioNameOrderByStartTimeDesc(String featureName, String scenarioName, Pageable pageable);
}
