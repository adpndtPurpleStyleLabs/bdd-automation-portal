package com.bdd.portal.repository;

import com.bdd.portal.entity.Scenario;
import com.bdd.portal.entity.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
    List<Scenario> findByFeatureVersionId(Long featureVersionId);
    List<Scenario> findByFeatureVersionIdAndStatus(Long featureVersionId, VersionStatus status);
}
