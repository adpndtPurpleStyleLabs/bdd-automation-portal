package com.bdd.portal.repository;

import com.bdd.portal.entity.FeatureVersion;
import com.bdd.portal.entity.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeatureVersionRepository extends JpaRepository<FeatureVersion, Long> {
    List<FeatureVersion> findByStatus(VersionStatus status);
    Optional<FeatureVersion> findByFeatureFileIdAndStatus(Long featureFileId, VersionStatus status);
    List<FeatureVersion> findByFeatureFileId(Long featureFileId);
    
    @org.springframework.data.jpa.repository.Query("SELECT SUM(f.scenarioCount) FROM FeatureVersion f WHERE f.status = 'ACTIVE'")
    Long getTotalScenarios();
    
    long countByStatus(VersionStatus status);
}
