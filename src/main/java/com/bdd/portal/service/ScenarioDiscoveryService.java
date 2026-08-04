package com.bdd.portal.service;

import com.bdd.portal.entity.Execution;
import com.bdd.portal.entity.ExecutionStatus;
import com.bdd.portal.entity.FeatureExecution;
import com.bdd.portal.entity.ScenarioExecution;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.repository.FeatureExecutionRepository;
import com.bdd.portal.repository.ScenarioExecutionRepository;
import io.cucumber.core.feature.FeatureParser;
import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.gherkin.Pickle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScenarioDiscoveryService {

    private final ExecutionRepository executionRepository;
    private final FeatureExecutionRepository featureExecutionRepository;
    private final ScenarioExecutionRepository scenarioExecutionRepository;

    @Value("${bdd.portal.features-path:src/test/resources/features}")
    private String featuresPath;

    @Transactional
    public Execution discoverAndQueueScenarios(Execution execution) {
        log.info("Starting scenario discovery for Execution UUID: {}", execution.getExecutionUuid());
        
        List<URI> targetUris = new ArrayList<>();
        
        if (execution.getTargetScenarios() != null && !execution.getTargetScenarios().isEmpty()) {
            for (String scenarioPath : execution.getTargetScenarios()) {
                String[] parts = scenarioPath.split(":");
                Path path = Paths.get(featuresPath, parts[0]);
                targetUris.add(path.toUri());
            }
        } else if (execution.getFeatureFile() != null) {
            Path featurePath = Paths.get(featuresPath, execution.getFeatureFile().getRelativePath());
            targetUris.add(featurePath.toUri());
        } else if (execution.getTargetFolder() != null) {
            Path folderPath = Paths.get(featuresPath, execution.getTargetFolder());
            targetUris.add(folderPath.toUri());
        } else {
            targetUris.add(Paths.get(featuresPath).toUri());
        }

        FeatureParser parser = new FeatureParser(() -> UUID.randomUUID());
        List<Feature> features = new ArrayList<>();
        
        log.info("Target URIs: {}", targetUris);
        
        java.util.Set<Path> uniqueFeaturePaths = new java.util.HashSet<>();
        
        for (URI uri : targetUris) {
            try {
                java.nio.file.Files.walk(Paths.get(uri))
                        .filter(p -> p.toString().endsWith(".feature"))
                        .forEach(uniqueFeaturePaths::add);
            } catch (Exception e) {
                log.error("Failed to walk URI: " + uri, e);
            }
        }
        
        for (Path p : uniqueFeaturePaths) {
            try {
                java.util.Optional<Feature> parsed = parser.parseResource(new io.cucumber.core.resource.Resource() {
                    @Override
                    public URI getUri() {
                        return p.toUri();
                    }
                    @Override
                    public java.io.InputStream getInputStream() throws java.io.IOException {
                        return java.nio.file.Files.newInputStream(p);
                    }
                });
                parsed.ifPresent(f -> {
                    log.info("Parsed feature: {} with {} pickles", f.getUri(), f.getPickles().size());
                    features.add(f);
                });
            } catch (Exception e) {
                log.error("Failed to parse feature: " + p, e);
            }
        }

        List<ScenarioExecution> scenarioExecutions = new ArrayList<>();
        
        for (Feature feature : features) {
            List<Pickle> pickles = feature.getPickles();
            
            if (execution.getTargetScenarios() != null && !execution.getTargetScenarios().isEmpty()) {
                pickles = pickles.stream().filter(p -> {
                    for (String target : execution.getTargetScenarios()) {
                        String[] parts = target.split(":");
                        if (parts.length > 1) {
                            int line = Integer.parseInt(parts[1]);
                            if (p.getLocation().getLine() == line || p.getScenarioLocation().getLine() == line) {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toList());
            }
            
            log.info("Feature {} pickles after filter: {}", feature.getUri(), pickles.size());
            
            if (pickles.isEmpty()) {
                continue; // Skip feature if no target scenarios matched
            }

            // Create FeatureExecution to maintain DB schema constraint and UI hierarchy
            FeatureExecution featureExecution = new FeatureExecution();
            featureExecution.setExecution(execution);
            featureExecution.setUri(feature.getUri().toString());
            featureExecution.setFeatureName(feature.getName().orElse("Unknown"));
            featureExecution.setStatus(ExecutionStatus.QUEUED);
            featureExecution = featureExecutionRepository.save(featureExecution);

            for (Pickle pickle : pickles) {
                ScenarioExecution scenarioExecution = new ScenarioExecution();
                scenarioExecution.setExecution(execution);
                scenarioExecution.setFeatureExecution(featureExecution);
                scenarioExecution.setScenarioExecutionUuid(UUID.randomUUID().toString());
                scenarioExecution.setFeatureName(feature.getName().isPresent() ? feature.getName().get() : "Unknown");
                scenarioExecution.setFeatureUri(feature.getUri().toString());
                scenarioExecution.setScenarioName(pickle.getName());
                scenarioExecution.setLineNumber(pickle.getLocation().getLine());
                scenarioExecution.setBrowser(execution.getBrowser());
                scenarioExecution.setStatus(ExecutionStatus.QUEUED);
                scenarioExecution.setQueuedAt(LocalDateTime.now());
                scenarioExecutions.add(scenarioExecution);
            }
        }
        
        scenarioExecutionRepository.saveAll(scenarioExecutions);
        
        execution.setTotalScenarios(scenarioExecutions.size());
        execution.setQueuedScenarios(scenarioExecutions.size());
        execution.setStatus(ExecutionStatus.QUEUED);
        
        return executionRepository.save(execution);
    }
}
