package com.bdd.portal.service;

import com.bdd.portal.entity.FeatureFile;
import com.bdd.portal.repository.FeatureFileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureScannerService {

    private final FeatureFileRepository featureFileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${bdd.portal.features-path}")
    private String featuresPath;

    // Store in-memory scan results keyed by scanId
    private final Map<String, List<FeatureFile>> inMemoryScans = new ConcurrentHashMap<>();

    @PostConstruct
    public void scanOnStartup() {
        log.info("Running initial feature scan...");
        scanFeatures();
    }

    public void scanFeatures() {
        doScan(null, true);
    }

    public void scanInMemory(String scanId) {
        doScan(scanId, false);
    }

    public void saveInMemoryScan(String scanId) {
        List<FeatureFile> results = inMemoryScans.remove(scanId);
        if (results != null) {
            featureFileRepository.saveAll(results);
            log.info("Saved in-memory scan {} to database.", scanId);
        }
    }

    private void doScan(String scanId, boolean persist) {
        Path rootPath = Paths.get(featuresPath);
        if (!Files.exists(rootPath)) {
            log.warn("Feature path {} does not exist. Creating...", featuresPath);
            try {
                Files.createDirectories(rootPath);
            } catch (IOException e) {
                log.error("Failed to create feature directory", e);
                return;
            }
        }

        List<FeatureFile> existingFeatures = featureFileRepository.findAll();
        existingFeatures.forEach(f -> f.setEnabled(false));

        List<Path> featurePaths = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".feature"))
                 .forEach(featurePaths::add);
        } catch (IOException e) {
            log.error("Error scanning feature files", e);
        }

        if (scanId != null) {
            sendWebSocketEvent(scanId, Map.of(
                "type", "SCAN_STARTED",
                "totalFeatures", featurePaths.size()
            ));
        }

        Set<String> usedFeatureSlugs = new HashSet<>();
        Set<String> usedScenarioSlugs = new HashSet<>();
        Set<String> seenFeatureNames = new HashSet<>();
        Set<String> seenScenarioNames = new HashSet<>();
        List<String> duplicateFeatures = new ArrayList<>();
        List<String> duplicateScenarios = new ArrayList<>();
        boolean allHealthy = true;

        for (Path path : featurePaths) {
            boolean healthy = processFeatureFile(path, rootPath, existingFeatures, usedFeatureSlugs, usedScenarioSlugs, seenFeatureNames, seenScenarioNames, duplicateFeatures, duplicateScenarios, scanId);
            if (!healthy) {
                allHealthy = false;
            }
        }

        if (scanId != null) {
            sendWebSocketEvent(scanId, Map.of(
                "type", "SCAN_COMPLETED",
                "healthy", allHealthy,
                "duplicateFeatures", duplicateFeatures,
                "duplicateScenarios", duplicateScenarios
            ));
        }

        if (persist) {
            featureFileRepository.saveAll(existingFeatures);
            log.info("Feature scan completed and persisted.");
        } else if (scanId != null) {
            inMemoryScans.put(scanId, existingFeatures);
            log.info("Feature scan {} completed in-memory.", scanId);
        }
    }

    private boolean processFeatureFile(Path filePath, Path rootPath, List<FeatureFile> existingFeatures, Set<String> usedFeatureSlugs, Set<String> usedScenarioSlugs, Set<String> seenFeatureNames, Set<String> seenScenarioNames, List<String> duplicateFeatures, List<String> duplicateScenarios, String scanId) {
        try {
            String relativePath = rootPath.relativize(filePath).toString();
            String folder = filePath.getParent() != null ? rootPath.relativize(filePath.getParent()).toString() : "";
            if (folder.isEmpty()) {
                folder = "Root";
            } else {
                folder = folder.replace("\\", "/");
            }
            
            String moduleSlug = makeSlug(folder.split("/")[0], new HashSet<>());

            BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
            LocalDateTime lastModified = LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), ZoneId.systemDefault());

            FeatureFile featureFile = existingFeatures.stream()
                    .filter(f -> f.getRelativePath().equals(relativePath))
                    .findFirst()
                    .orElse(new FeatureFile());

            featureFile.setRelativePath(relativePath);
            featureFile.setFolder(folder);
            featureFile.setModuleSlug(moduleSlug);
            featureFile.setLastModified(lastModified);
            featureFile.setEnabled(true);
            
            String fileName = filePath.getFileName().toString();
            
            if (scanId != null) {
                sendWebSocketEvent(scanId, Map.of(
                    "type", "FEATURE_FOUND",
                    "featureName", fileName,
                    "path", relativePath
                ));
            }

            parseFeatureContent(filePath, featureFile, usedFeatureSlugs, usedScenarioSlugs, seenFeatureNames, seenScenarioNames, duplicateFeatures, duplicateScenarios, scanId);

            if (featureFile.getId() == null) {
                existingFeatures.add(featureFile);
            }
            
            if (scanId != null) {
                sendWebSocketEvent(scanId, Map.of(
                    "type", "FEATURE_COMPLETED",
                    "featureName", fileName,
                    "scenarioCount", featureFile.getScenarioCount(),
                    "status", "SUCCESS"
                ));
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error processing feature file: {}", filePath, e);
            if (scanId != null) {
                sendWebSocketEvent(scanId, Map.of(
                    "type", "FEATURE_FAILED",
                    "featureName", filePath.getFileName().toString(),
                    "message", e.getMessage() != null ? e.getMessage() : "Unknown error"
                ));
            }
            return false;
        }
    }

    private void parseFeatureContent(Path filePath, FeatureFile featureFile, Set<String> usedFeatureSlugs, Set<String> usedScenarioSlugs, Set<String> seenFeatureNames, Set<String> seenScenarioNames, List<String> duplicateFeatures, List<String> duplicateScenarios, String scanId) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        String name = filePath.getFileName().toString();
        List<String> tags = new ArrayList<>();
        
        // Use a new list and replace it entirely to avoid Hibernate detached collection issues during update
        List<FeatureFile.FeatureScenario> parsedScenarios = new ArrayList<>();
        
        int stepCount = 0;
        StringBuilder description = new StringBuilder();
        boolean inFeature = false;
        boolean pastDescription = false;
        
        int lineNumber = 0;

        for (String line : lines) {
            lineNumber++;
            String trimmed = line.trim();
            if (trimmed.startsWith("@")) {
                String[] lineTags = trimmed.split("\\s+");
                for (String t : lineTags) {
                    if (t.startsWith("@") && !tags.contains(t)) {
                        tags.add(t);
                    }
                }
            } else if (trimmed.startsWith("Feature:")) {
                name = trimmed.substring("Feature:".length()).trim();
                inFeature = true;
            } else if (trimmed.startsWith("Scenario:") || trimmed.startsWith("Scenario Outline:")) {
                String scenarioName = trimmed.replace("Scenario Outline:", "").replace("Scenario:", "").trim();
                if (scenarioName.isEmpty()) {
                    scenarioName = "Unnamed Scenario";
                }
                
                if (!seenScenarioNames.add(scenarioName)) {
                    duplicateScenarios.add(scenarioName);
                }
                
                if (scanId != null) {
                    sendWebSocketEvent(scanId, Map.of(
                        "type", "SCENARIO_FOUND",
                        "featureName", filePath.getFileName().toString(),
                        "scenario", scenarioName
                    ));
                }
                
                String scSlug = makeSlug(scenarioName, usedScenarioSlugs);
                if (scanId != null) {
                    sendWebSocketEvent(scanId, Map.of(
                        "type", "SCENARIO_SLUG_CREATED",
                        "featureName", filePath.getFileName().toString(),
                        "scenarioName", scenarioName,
                        "slug", scSlug
                    ));
                }
                
                FeatureFile.FeatureScenario fs = new FeatureFile.FeatureScenario(scenarioName, lineNumber);
                fs.setSlug(scSlug);
                parsedScenarios.add(fs);
                pastDescription = true;
            } else if (trimmed.startsWith("Given ") || trimmed.startsWith("When ") || 
                       trimmed.startsWith("Then ") || trimmed.startsWith("And ") || 
                       trimmed.startsWith("But ") || trimmed.startsWith("* ")) {
                stepCount++;
                pastDescription = true;
            } else if (inFeature && !pastDescription && !trimmed.isEmpty()) {
                description.append(trimmed).append("\n");
            }
        }

        featureFile.setName(name.isEmpty() ? filePath.getFileName().toString() : name);
        if (!seenFeatureNames.add(featureFile.getName())) {
            duplicateFeatures.add(featureFile.getName());
        }
        
        String featureSlug = makeSlug(featureFile.getName(), usedFeatureSlugs);
        featureFile.setSlug(featureSlug);
        
        if (scanId != null) {
            sendWebSocketEvent(scanId, Map.of(
                "type", "FEATURE_SLUG_CREATED",
                "featureName", filePath.getFileName().toString(),
                "slug", featureSlug
            ));
        }

        featureFile.setTags(String.join(" ", tags));
        featureFile.setScenarioCount(parsedScenarios.size());
        
        featureFile.getScenarios().clear();
        featureFile.getScenarios().addAll(parsedScenarios);
        
        featureFile.setStepCount(stepCount);
        
        String descStr = description.toString().trim();
        if (descStr.length() > 1000) {
            descStr = descStr.substring(0, 997) + "...";
        }
        featureFile.setDescription(descStr);
    }
    
    private String makeSlug(String input, Set<String> usedSlugs) {
        if (input == null || input.isEmpty()) {
            input = "untitled";
        }
        String baseSlug = input.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
                
        String slug = baseSlug;
        int counter = 2;
        while (usedSlugs.contains(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        usedSlugs.add(slug);
        return slug;
    }
    
    private void sendWebSocketEvent(String scanId, Map<String, Object> payload) {
        messagingTemplate.convertAndSend("/topic/scan/" + scanId, (Object) payload);
    }
}
