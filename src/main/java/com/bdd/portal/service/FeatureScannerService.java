package com.bdd.portal.service;

import com.bdd.portal.entity.FeatureFile;
import com.bdd.portal.repository.FeatureFileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureScannerService {

    private final FeatureFileRepository featureFileRepository;

    @Value("${bdd.portal.features-path}")
    private String featuresPath;

    @PostConstruct
    public void scanOnStartup() {
        log.info("Running initial feature scan...");
        scanFeatures();
    }

    public void scanFeatures() {
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
        existingFeatures.forEach(f -> f.setEnabled(false)); // Mark all as inactive initially

        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".feature"))
                 .forEach(path -> processFeatureFile(path, rootPath, existingFeatures));
        } catch (IOException e) {
            log.error("Error scanning feature files", e);
        }

        // Save updates (including those marked as disabled because they were deleted)
        featureFileRepository.saveAll(existingFeatures);
        log.info("Feature scan completed.");
    }

    private void processFeatureFile(Path filePath, Path rootPath, List<FeatureFile> existingFeatures) {
        try {
            String relativePath = rootPath.relativize(filePath).toString();
            String folder = filePath.getParent() != null ? rootPath.relativize(filePath.getParent()).toString() : "";
            if (folder.isEmpty()) {
                folder = "Root";
            } else {
                folder = folder.replace("\\", "/");
            }

            BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
            LocalDateTime lastModified = LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), ZoneId.systemDefault());

            FeatureFile featureFile = existingFeatures.stream()
                    .filter(f -> f.getRelativePath().equals(relativePath))
                    .findFirst()
                    .orElse(new FeatureFile());

            featureFile.setRelativePath(relativePath);
            featureFile.setFolder(folder);
            featureFile.setLastModified(lastModified);
            featureFile.setEnabled(true);

            parseFeatureContent(filePath, featureFile);

            if (featureFile.getId() == null) {
                existingFeatures.add(featureFile);
            }
        } catch (IOException e) {
            log.error("Error processing feature file: {}", filePath, e);
        }
    }

    private void parseFeatureContent(Path filePath, FeatureFile featureFile) throws IOException {
        List<String> lines = Files.readAllLines(filePath);
        String name = filePath.getFileName().toString();
        List<String> tags = new ArrayList<>();
        int scenarioCount = 0;
        StringBuilder description = new StringBuilder();
        boolean inFeature = false;
        boolean pastDescription = false;

        for (String line : lines) {
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
                scenarioCount++;
                pastDescription = true;
            } else if (inFeature && !pastDescription && !trimmed.isEmpty()) {
                description.append(trimmed).append("\n");
            }
        }

        featureFile.setName(name.isEmpty() ? filePath.getFileName().toString() : name);
        featureFile.setTags(String.join(" ", tags));
        featureFile.setScenarioCount(scenarioCount);
        
        String descStr = description.toString().trim();
        if (descStr.length() > 1000) {
            descStr = descStr.substring(0, 997) + "...";
        }
        featureFile.setDescription(descStr);
    }
}
