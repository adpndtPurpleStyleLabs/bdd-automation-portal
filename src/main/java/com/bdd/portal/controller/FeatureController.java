package com.bdd.portal.controller;


import com.bdd.portal.entity.FeatureFile;
import com.bdd.portal.repository.FeatureFileRepository;
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
import com.bdd.portal.util.FeatureParserUtil;

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
    private final FeatureScannerService featureScannerService;
    private final FeatureExecutionRepository featureExecutionRepository;
    private final ScenarioExecutionRepository scenarioExecutionRepository;
    private final TestEnvironmentService testEnvironmentService;

    @Value("${bdd.portal.features-path}")
    private String featuresPath;

    @GetMapping
    public String listFeatures(Model model) {
        List<FeatureFile> features = featureFileRepository.findAll();
        
        // Group by folder for tree view
        Map<String, List<FeatureFile>> grouped = features.stream()
                .collect(Collectors.groupingBy(f -> f.getFolder() != null ? f.getFolder() : "Root"));
        
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
        Path path = Paths.get(featuresPath, feature.getRelativePath());
        List<ScenarioDto> scenarios = FeatureParserUtil.parseFeatureFile(path);
        
        List<FeatureExecution> topExecutions = featureExecutionRepository.findTop5ByFeatureNameOrderByStartTimeDesc(path.getFileName().toString());
        
        for (ScenarioDto dto : scenarios) {
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
        model.addAttribute("scenarios", scenarios);
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
        Path path = Paths.get(featuresPath, feature.getRelativePath());
        List<ScenarioDto> scenarios = FeatureParserUtil.parseFeatureFile(path);
        
        ScenarioDto scenario = scenarios.stream()
                .filter(s -> scenarioSlug.equals(s.getSlug()))
                .findFirst()
                .orElse(null);
                
        if (scenario == null) {
            return "redirect:/features/" + moduleSlug + "/" + featureSlug;
        }

        List<FeatureExecution> topExecutions = featureExecutionRepository.findTop5ByFeatureNameOrderByStartTimeDesc(Path.of(feature.getRelativePath()).getFileName().toString());
        List<ScenarioExecution> scenarioHistoryList = new java.util.ArrayList<>();
        for (FeatureExecution fe : topExecutions) {
            if (fe.getScenarios() != null) {
                fe.getScenarios().stream()
                    .filter(se -> se.getScenarioName().equals(scenario.getName()))
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
