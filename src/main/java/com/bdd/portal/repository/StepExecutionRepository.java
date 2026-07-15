package com.bdd.portal.repository;

import com.bdd.portal.entity.StepExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StepExecutionRepository extends JpaRepository<StepExecution, Long> {
    List<StepExecution> findByScenarioExecutionId(Long scenarioExecutionId);
}
