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
import com.bdd.portal.util.FeatureParserUtil;

import java.nio.file.Path;
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

    @GetMapping("/{id}")
    public String viewFeatureDetails(@PathVariable Long id, Model model) {
        FeatureFile feature = featureFileRepository.findById(id).orElse(null);
        if (feature == null) {
            return "redirect:/features";
        }
        Path path = Paths.get(featuresPath, feature.getRelativePath());
        List<ScenarioDto> scenarios = FeatureParserUtil.parseFeatureFile(path);
        
        FeatureExecution latestExecution = featureExecutionRepository.findFirstByFeatureNameOrderByStartTimeDesc(feature.getName());
        if (latestExecution != null && latestExecution.getScenarios() != null) {
            for (ScenarioDto dto : scenarios) {
                latestExecution.getScenarios().stream()
                    .filter(se -> se.getScenarioName().equals(dto.getName()))
                    .findFirst()
                    .ifPresent(se -> {
                        dto.setStatus(se.getStatus());
                        dto.setDurationMs(se.getDurationMs());
                        dto.setLastRun(se.getStartTime());
                    });
            }
        }
        
        model.addAttribute("feature", feature);
        model.addAttribute("scenarios", scenarios);
        model.addAttribute("environments", testEnvironmentService.getAllEnvironmentNames());
        return "features/detail";
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
