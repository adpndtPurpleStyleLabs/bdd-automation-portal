package com.bdd.portal.repository;

import com.bdd.portal.entity.FeatureFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeatureFileRepository extends JpaRepository<FeatureFile, Long> {
    Optional<FeatureFile> findByRelativePath(String relativePath);
    Optional<FeatureFile> findByModuleSlugAndSlug(String moduleSlug, String slug);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(f.scenarioCount) FROM FeatureFile f")
    Long getTotalScenarios();
}
