package com.bdd.portal.controller;

import com.bdd.portal.entity.Execution;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.repository.FeatureFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final FeatureFileRepository featureFileRepository;
    private final ExecutionRepository executionRepository;

    @GetMapping("/")
    public String dashboard(Model model) {
        long totalFeatures = featureFileRepository.count();
        long totalExecutions = executionRepository.count();
        Long totalScenarios = featureFileRepository.getTotalScenarios();
        if (totalScenarios == null) totalScenarios = 0L;

        model.addAttribute("totalFeatures", totalFeatures);
        model.addAttribute("totalScenarios", totalScenarios);
        model.addAttribute("totalExecutions", totalExecutions);
        
        List<Execution> recent = executionRepository.findTop10ByOrderByStartTimeDesc();
        model.addAttribute("recentExecutions", recent);
        
        // Group by module (folder)
        java.util.Map<String, com.bdd.portal.entity.Execution> moduleExecutions = new java.util.LinkedHashMap<>();
        for (com.bdd.portal.entity.Execution ex : recent) {
            String module = ex.getTargetFolder() != null ? ex.getTargetFolder() : 
                           (ex.getFeatureFile() != null ? ex.getFeatureFile().getFolder() : "All");
            if (module != null && !moduleExecutions.containsKey(module)) {
                moduleExecutions.put(module, ex);
            }
        }
        model.addAttribute("moduleExecutions", moduleExecutions.values());
        
        return "dashboard";
    }
}
