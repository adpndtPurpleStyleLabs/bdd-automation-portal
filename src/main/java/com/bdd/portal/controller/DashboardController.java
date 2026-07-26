package com.bdd.portal.controller;

import com.bdd.portal.entity.Execution;
import com.bdd.portal.entity.VersionStatus;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.repository.FeatureVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final FeatureVersionRepository featureVersionRepository;
    private final ExecutionRepository executionRepository;

    @GetMapping("/")
    public String dashboard(Model model, @RequestParam(defaultValue = "7") int days) {
        long totalFeatures = featureVersionRepository.countByStatus(VersionStatus.ACTIVE);
        long totalExecutions = executionRepository.count();
        Long totalScenarios = featureVersionRepository.getTotalScenarios();
        if (totalScenarios == null) totalScenarios = 0L;

        model.addAttribute("totalFeatures", totalFeatures);
        model.addAttribute("totalScenarios", totalScenarios);
        model.addAttribute("totalExecutions", totalExecutions);
        
        List<Execution> recent = executionRepository.findTop10ByOrderByStartTimeDesc();
        model.addAttribute("recentExecutions", recent);
        
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        List<Execution> chartExecutions = executionRepository.findByStartTimeAfterOrderByStartTimeAsc(cutoff);
        model.addAttribute("chartExecutions", chartExecutions);
        model.addAttribute("chartDays", days);
        
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
