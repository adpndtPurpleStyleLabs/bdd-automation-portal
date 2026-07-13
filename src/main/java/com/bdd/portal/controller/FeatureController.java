package com.bdd.portal.controller;

import com.bdd.portal.entity.FeatureFile;
import com.bdd.portal.repository.FeatureFileRepository;
import com.bdd.portal.service.FeatureScannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/features")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureFileRepository featureFileRepository;
    private final FeatureScannerService featureScannerService;

    @GetMapping
    public String listFeatures(Model model) {
        List<FeatureFile> features = featureFileRepository.findAll();
        
        // Group by folder for tree view
        Map<String, List<FeatureFile>> featuresByFolder = features.stream()
                .filter(FeatureFile::isEnabled)
                .collect(Collectors.groupingBy(f -> f.getFolder() != null ? f.getFolder() : "Root"));
                
        model.addAttribute("featuresByFolder", featuresByFolder);
        return "features/list";
    }

    @GetMapping("/{id}")
    public String featureDetail(@PathVariable Long id, Model model) {
        FeatureFile feature = featureFileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid feature Id:" + id));
        model.addAttribute("feature", feature);
        return "features/detail";
    }
    
    @PostMapping("/rescan")
    public String rescanFeatures() {
        featureScannerService.scanFeatures();
        return "redirect:/features";
    }
}
