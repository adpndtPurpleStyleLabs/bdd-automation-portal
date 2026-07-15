package com.bdd.portal.repository;

import com.bdd.portal.entity.ScenarioExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScenarioExecutionRepository extends JpaRepository<ScenarioExecution, Long> {
    List<ScenarioExecution> findByFeatureExecutionId(Long featureExecutionId);
}
