package com.bdd.portal.service;

import com.bdd.portal.entity.FeatureFile;
import com.bdd.portal.entity.FeatureVersion;
import com.bdd.portal.entity.Scenario;
import com.bdd.portal.entity.VersionStatus;
import com.bdd.portal.repository.FeatureFileRepository;
import com.bdd.portal.repository.FeatureVersionRepository;
import com.bdd.portal.repository.ScenarioRepository;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureScannerService {

    private final FeatureFileRepository featureFileRepository;
    private final FeatureVersionRepository featureVersionRepository;
    private final ScenarioRepository scenarioRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${bdd.portal.features-path}")
    private String featuresPath;

    // Store in-memory scan results keyed by scanId - Not heavily used anymore due to complex relations, but kept for compatibility
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
        // With the new architecture, in-memory scans for preview are trickier to persist directly.
        // Usually, users trigger a real scan. We will just trigger a real scan here.
        doScan(scanId, true);
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
        
        Set<Long> processedFeatureIds = new HashSet<>();

        for (Path path : featurePaths) {
            Long featureId = processFeatureFile(path, rootPath, usedFeatureSlugs, usedScenarioSlugs, seenFeatureNames, seenScenarioNames, duplicateFeatures, duplicateScenarios, scanId, persist);
            if (featureId == null) {
                allHealthy = false;
            } else {
                processedFeatureIds.add(featureId);
            }
        }

        // Handle deleted files: Archive active versions of features that were not processed
        if (persist) {
            List<FeatureFile> allFeatures = featureFileRepository.findAll();
            for (FeatureFile feature : allFeatures) {
                if (!processedFeatureIds.contains(feature.getId())) {
                    // This feature is no longer on disk. Archive its active version.
                    Optional<FeatureVersion> activeVersionOpt = featureVersionRepository.findByFeatureFileIdAndStatus(feature.getId(), VersionStatus.ACTIVE);
                    if (activeVersionOpt.isPresent()) {
                        FeatureVersion activeVersion = activeVersionOpt.get();
                        activeVersion.setStatus(VersionStatus.INACTIVE);
                        featureVersionRepository.save(activeVersion);
                        
                        List<Scenario> activeScenarios = scenarioRepository.findByFeatureVersionIdAndStatus(activeVersion.getId(), VersionStatus.ACTIVE);
                        activeScenarios.forEach(s -> s.setStatus(VersionStatus.INACTIVE));
                        scenarioRepository.saveAll(activeScenarios);
                        log.info("Archived Feature: {} as it was removed from disk.", feature.getRelativePath());
                    }
                }
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

        log.info("Feature scan completed.");
    }

    private Long processFeatureFile(Path filePath, Path rootPath, Set<String> usedFeatureSlugs, Set<String> usedScenarioSlugs, Set<String> seenFeatureNames, Set<String> seenScenarioNames, List<String> duplicateFeatures, List<String> duplicateScenarios, String scanId, boolean persist) {
        try {
            String relativePath = rootPath.relativize(filePath).toString();
            String folder = filePath.getParent() != null ? rootPath.relativize(filePath.getParent()).toString() : "";
            if (folder.isEmpty()) {
                folder = "Root";
            } else {
                folder = folder.replace("\\", "/");
            }
            
            String moduleSlug = makeSlug(folder.split("/")[0], new HashSet<>());

            byte[] fileBytes = Files.readAllBytes(filePath);
            String fileHash = calculateSHA256(fileBytes);
            String content = new String(fileBytes);

            FeatureFile featureFile = featureFileRepository.findByRelativePath(relativePath)
                    .orElse(new FeatureFile());

            featureFile.setRelativePath(relativePath);
            featureFile.setFolder(folder);
            featureFile.setModuleSlug(moduleSlug);
            
            if (featureFile.getCurrentVersion() == null) {
                featureFile.setCurrentVersion(1);
            }

            if (persist) {
                if (featureFile.getId() != null) {
                    Optional<FeatureVersion> activeVersionOpt = featureVersionRepository.findByFeatureFileIdAndStatus(featureFile.getId(), VersionStatus.ACTIVE);
                    if (activeVersionOpt.isPresent()) {
                        FeatureVersion activeVersion = activeVersionOpt.get();
                        if (activeVersion.getFileHash().equals(fileHash)) {
                            // Unchanged. Skip processing.
                            return featureFile.getId();
                        } else {
                            // Changed. Archive active version.
                            activeVersion.setStatus(VersionStatus.INACTIVE);
                            featureVersionRepository.save(activeVersion);
                            
                            List<Scenario> activeScenarios = scenarioRepository.findByFeatureVersionIdAndStatus(activeVersion.getId(), VersionStatus.ACTIVE);
                            activeScenarios.forEach(s -> s.setStatus(VersionStatus.INACTIVE));
                            scenarioRepository.saveAll(activeScenarios);
                            
                            featureFile.setCurrentVersion(featureFile.getCurrentVersion() + 1);
                        }
                    }
                }
                
                // Create new version
                FeatureVersion newVersion = new FeatureVersion();
                newVersion.setFeatureFile(featureFile);
                newVersion.setVersion(featureFile.getCurrentVersion());
                newVersion.setFileHash(fileHash);
                newVersion.setContent(content);
                newVersion.setStatus(VersionStatus.ACTIVE);
                
                parseFeatureContent(content, filePath, featureFile, newVersion, usedFeatureSlugs, usedScenarioSlugs, seenFeatureNames, seenScenarioNames, duplicateFeatures, duplicateScenarios, scanId, persist);
                
                return featureFile.getId();
            } else {
                // Not persisting, just parse in-memory to validate and send progress to UI
                FeatureVersion fakeVersion = new FeatureVersion();
                parseFeatureContent(content, filePath, featureFile, fakeVersion, usedFeatureSlugs, usedScenarioSlugs, seenFeatureNames, seenScenarioNames, duplicateFeatures, duplicateScenarios, scanId, persist);
                return -1L;
            }

        } catch (Exception e) {
            log.error("Error processing feature file: {}", filePath, e);
            if (scanId != null) {
                sendWebSocketEvent(scanId, Map.of(
                    "type", "FEATURE_FAILED",
                    "featureName", filePath.getFileName().toString(),
                    "message", e.getMessage() != null ? e.getMessage() : "Unknown error"
                ));
            }
            return null;
        }
    }

    private void parseFeatureContent(String content, Path filePath, FeatureFile featureFile, FeatureVersion newVersion, Set<String> usedFeatureSlugs, Set<String> usedScenarioSlugs, Set<String> seenFeatureNames, Set<String> seenScenarioNames, List<String> duplicateFeatures, List<String> duplicateScenarios, String scanId, boolean persist) {
        String[] lines = content.split("\\r?\\n");
        String name = filePath.getFileName().toString();
        List<String> tags = new ArrayList<>();
        
        List<Scenario> parsedScenarios = new ArrayList<>();
        
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
                
                String scSlug = makeSlug(scenarioName, usedScenarioSlugs);
                
                Scenario scenario = new Scenario();
                scenario.setFeatureVersion(newVersion);
                scenario.setScenarioName(scenarioName);
                scenario.setLineNumber(lineNumber);
                scenario.setSlug(scSlug);
                scenario.setStatus(VersionStatus.ACTIVE);
                parsedScenarios.add(scenario);
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

        newVersion.setTags(String.join(" ", tags));
        newVersion.setScenarioCount(parsedScenarios.size());
        newVersion.setStepCount(stepCount);
        
        String descStr = description.toString().trim();
        if (descStr.length() > 1000) {
            descStr = descStr.substring(0, 997) + "...";
        }
        newVersion.setDescription(descStr);

        if (persist) {
            featureFileRepository.save(featureFile);
            FeatureVersion savedVersion = featureVersionRepository.save(newVersion);
            for (Scenario s : parsedScenarios) {
                s.setFeatureVersion(savedVersion);
            }
            scenarioRepository.saveAll(parsedScenarios);
        }
        
        if (scanId != null) {
            sendWebSocketEvent(scanId, Map.of(
                "type", "FEATURE_COMPLETED",
                "featureName", featureFile.getName(),
                "scenarioCount", newVersion.getScenarioCount(),
                "status", "SUCCESS"
            ));
        }
    }
    
    private String calculateSHA256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
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
