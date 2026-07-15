package com.bdd.portal.repository;

import com.bdd.portal.entity.FeatureExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeatureExecutionRepository extends JpaRepository<FeatureExecution, Long> {
    List<FeatureExecution> findByExecutionId(Long executionId);
    
    FeatureExecution findFirstByFeatureNameOrderByStartTimeDesc(String featureName);
}
