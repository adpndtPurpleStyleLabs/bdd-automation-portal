package com.bdd.portal.controller;

import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.repository.FeatureFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final FeatureFileRepository featureFileRepository;
    private final ExecutionRepository executionRepository;

    @GetMapping("/")
    public String dashboard(Model model) {
        long totalFeatures = featureFileRepository.count();
        long totalExecutions = executionRepository.count();
        
        model.addAttribute("totalFeatures", totalFeatures);
        model.addAttribute("totalExecutions", totalExecutions);
        model.addAttribute("recentExecutions", executionRepository.findTop10ByOrderByStartTimeDesc());
        
        return "dashboard";
    }
}
