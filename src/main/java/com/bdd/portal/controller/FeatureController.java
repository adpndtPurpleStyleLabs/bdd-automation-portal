package com.bdd.portal.controller;

import com.bdd.portal.entity.FeatureFile;
import com.bdd.portal.entity.FeatureVersion;
import com.bdd.portal.entity.Scenario;
import com.bdd.portal.entity.VersionStatus;
import com.bdd.portal.repository.FeatureFileRepository;
import com.bdd.portal.repository.FeatureVersionRepository;
import com.bdd.portal.repository.ScenarioRepository;
import com.bdd.portal.service.FeatureScannerService;
import com.bdd.portal.service.TestEnvironmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bdd.portal.dto.ScenarioDto;
import com.bdd.portal.entity.FeatureExecution;
import com.bdd.portal.repository.FeatureExecutionRepository;
import com.bdd.portal.repository.ScenarioExecutionRepository;
import com.bdd.portal.entity.ScenarioExecution;

import java.nio.file.Path;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestParam;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

@Controller
@RequestMapping("/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureFileRepository featureFileRepository;
    private final FeatureVersionRepository featureVersionRepository;
    private final ScenarioRepository scenarioRepository;
    private final FeatureScannerService featureScannerService;
    private final FeatureExecutionRepository featureExecutionRepository;
    private final ScenarioExecutionRepository scenarioExecutionRepository;
    private final TestEnvironmentService testEnvironmentService;

    @Value("${bdd.portal.features-path}")
    private String featuresPath;

    @GetMapping
    public String listFeatures(Model model) {
        List<FeatureVersion> activeVersions = featureVersionRepository.findByStatus(VersionStatus.ACTIVE);
        
        // Group by folder for tree view using the associated FeatureFile
        Map<String, List<FeatureVersion>> grouped = activeVersions.stream()
                .collect(Collectors.groupingBy(fv -> fv.getFeatureFile().getFolder() != null ? fv.getFeatureFile().getFolder() : "Root"));
        
        model.addAttribute("featuresByFolder", grouped);
        model.addAttribute("environments", testEnvironmentService.getAllEnvironmentNames());
        return "features/list";
    }

    @GetMapping("/{moduleSlug}/{featureSlug}")
    public String viewFeatureDetails(@PathVariable String moduleSlug, @PathVariable String featureSlug, Model model) {
        FeatureFile feature = featureFileRepository.findByModuleSlugAndSlug(moduleSlug, featureSlug).orElse(null);
        if (feature == null) {
            return "redirect:/features";
        }
        
        FeatureVersion activeVersion = featureVersionRepository.findByFeatureFileIdAndStatus(feature.getId(), VersionStatus.ACTIVE).orElse(null);
        if (activeVersion == null) {
            return "redirect:/features";
        }
        
        List<Scenario> activeScenarios = scenarioRepository.findByFeatureVersionIdAndStatus(activeVersion.getId(), VersionStatus.ACTIVE);
        
        // We still need to construct DTOs for the UI if it relies on them, or just use the Scenario entity.
        // The UI currently expects a list of ScenarioDto, or we can adapt it to expect Scenario entity.
        // Let's create ScenarioDto list from activeScenarios to minimize UI changes.
        List<ScenarioDto> dtos = activeScenarios.stream().map(s -> {
            ScenarioDto dto = new ScenarioDto();
            dto.setName(s.getScenarioName());
            dto.setLine(s.getLineNumber());
            dto.setSlug(s.getSlug());
            return dto;
        }).collect(Collectors.toList());
        
        List<FeatureExecution> topExecutions = featureExecutionRepository.findTop5ByFeatureNameOrderByStartTimeDesc(Path.of(feature.getRelativePath()).getFileName().toString());
        
        for (ScenarioDto dto : dtos) {
            List<ScenarioExecution> scenarioHistory = new java.util.ArrayList<>();
            for (FeatureExecution fe : topExecutions) {
                if (fe.getScenarios() != null) {
                    fe.getScenarios().stream()
                        .filter(se -> se.getScenarioName().equals(dto.getName()))
                        .findFirst()
                        .ifPresent(scenarioHistory::add);
                }
            }
            if (!scenarioHistory.isEmpty()) {
                ScenarioExecution current = scenarioHistory.get(0);
                dto.setStatus(current.getStatus());
                dto.setDurationMs(current.getDurationMs());
                dto.setLastRun(current.getStartTime());
                
                if (scenarioHistory.size() > 1) {
                    dto.setPreviousStatus(scenarioHistory.get(1).getStatus());
                }
            }
        }
        
        model.addAttribute("feature", feature);
        model.addAttribute("featureVersion", activeVersion);
        model.addAttribute("scenarios", dtos);
        model.addAttribute("environments", testEnvironmentService.getAllEnvironmentNames());
        return "features/detail";
    }

    @GetMapping("/{moduleSlug}/{featureSlug}/{scenarioSlug}")
    public String viewScenarioDetails(@PathVariable String moduleSlug, @PathVariable String featureSlug, @PathVariable String scenarioSlug, 
                                      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size, Model model) {
        FeatureFile feature = featureFileRepository.findByModuleSlugAndSlug(moduleSlug, featureSlug).orElse(null);
        if (feature == null) {
            return "redirect:/features";
        }
        
        FeatureVersion activeVersion = featureVersionRepository.findByFeatureFileIdAndStatus(feature.getId(), VersionStatus.ACTIVE).orElse(null);
        if (activeVersion == null) {
            return "redirect:/features";
        }

        List<Scenario> activeScenarios = scenarioRepository.findByFeatureVersionIdAndStatus(activeVersion.getId(), VersionStatus.ACTIVE);
        
        Scenario scenarioEntity = activeScenarios.stream()
                .filter(s -> scenarioSlug.equals(s.getSlug()))
                .findFirst()
                .orElse(null);
                
        if (scenarioEntity == null) {
            return "redirect:/features/" + moduleSlug + "/" + featureSlug;
        }
        
        List<ScenarioDto> parsedScenarios = com.bdd.portal.util.FeatureParserUtil.parseFeatureContent(activeVersion.getContent(), feature.getRelativePath());
        ScenarioDto scenario = parsedScenarios.stream()
                .filter(s -> scenarioSlug.equals(s.getSlug()))
                .findFirst()
                .orElse(null);
        
        if (scenario == null) {
            // fallback if slug mismatch
            scenario = new ScenarioDto();
            scenario.setName(scenarioEntity.getScenarioName());
            scenario.setLine(scenarioEntity.getLineNumber());
            scenario.setSlug(scenarioEntity.getSlug());
        }

        List<FeatureExecution> topExecutions = featureExecutionRepository.findTop5ByFeatureNameOrderByStartTimeDesc(Path.of(feature.getRelativePath()).getFileName().toString());
        List<ScenarioExecution> scenarioHistoryList = new java.util.ArrayList<>();
        final String targetScenarioName = scenario.getName();
        for (FeatureExecution fe : topExecutions) {
            if (fe.getScenarios() != null) {
                fe.getScenarios().stream()
                    .filter(se -> se.getScenarioName().equals(targetScenarioName))
                    .findFirst()
                    .ifPresent(scenarioHistoryList::add);
            }
        }
        if (!scenarioHistoryList.isEmpty()) {
            ScenarioExecution current = scenarioHistoryList.get(0);
            scenario.setStatus(current.getStatus());
            scenario.setDurationMs(current.getDurationMs());
            scenario.setLastRun(current.getStartTime());
            
            if (scenarioHistoryList.size() > 1) {
                scenario.setPreviousStatus(scenarioHistoryList.get(1).getStatus());
            }
        }
        
        Page<ScenarioExecution> executionHistory = scenarioExecutionRepository.findByFeatureExecutionFeatureNameAndScenarioNameOrderByStartTimeDesc(
                Path.of(feature.getRelativePath()).getFileName().toString(), scenario.getName(), PageRequest.of(page, size));
        
        model.addAttribute("feature", feature);
        model.addAttribute("featureVersion", activeVersion);
        model.addAttribute("scenario", scenario);
        model.addAttribute("executionHistory", executionHistory);
        model.addAttribute("environments", testEnvironmentService.getAllEnvironmentNames());
        return "features/scenario_detail";
    }
    
    @PostMapping("/rescan")
    public String rescanFeatures() {
        featureScannerService.scanFeatures();
        return "redirect:/features";
    }

    @PostMapping("/api/rescan/start")
    public org.springframework.http.ResponseEntity<Void> startRescanAsync(@org.springframework.web.bind.annotation.RequestParam String scanId) {
        new Thread(() -> {
            try {
                // Short sleep to ensure client has subscribed
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            featureScannerService.scanInMemory(scanId);
        }).start();
        return org.springframework.http.ResponseEntity.ok().build();
    }

    @PostMapping("/api/rescan/save")
    public org.springframework.http.ResponseEntity<Void> saveRescan(@org.springframework.web.bind.annotation.RequestParam String scanId) {
        featureScannerService.saveInMemoryScan(scanId);
        return org.springframework.http.ResponseEntity.ok().build();
    }
}
