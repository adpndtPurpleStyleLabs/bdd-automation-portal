package com.bdd.portal.controller;

import com.bdd.portal.entity.Execution;
import com.bdd.portal.entity.ExecutionStatus;
import com.bdd.portal.entity.ExecutionType;
import com.bdd.portal.entity.FeatureFile;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.repository.FeatureFileRepository;
import com.bdd.portal.repository.TestEnvironmentRepository;
import com.bdd.portal.entity.TestEnvironment;
import com.bdd.portal.service.ExecutionEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionRepository executionRepository;
    private final FeatureFileRepository featureFileRepository;
    private final TestEnvironmentRepository testEnvironmentRepository;
    private final ExecutionEngineService executionEngineService;

    @GetMapping
    public String listExecutions(
            @RequestParam(required = false) ExecutionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        
        Specification<Execution> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null) {
                predicates.add(cb.equal(root.get("executionType"), type));
            }
            if (date != null) {
                predicates.add(cb.between(root.get("startTime"), date.atStartOfDay(), date.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        
        Page<Execution> executionPage = executionRepository.findAll(spec, pageable);
        
        model.addAttribute("executionPage", executionPage);
        model.addAttribute("type", type);
        model.addAttribute("date", date);
        
        Map<String, String> envUrls = testEnvironmentRepository.findAll().stream()
                .collect(Collectors.toMap(TestEnvironment::getName, TestEnvironment::getUrl, (a, b) -> a));
        model.addAttribute("envUrls", envUrls);
        
        if (ExecutionType.SCHEDULED.equals(type)) {
            model.addAttribute("upcomingJobs", com.bdd.portal.config.SpringContext.getBean(com.bdd.portal.repository.ScheduledJobRepository.class).findByActiveTrueOrderByNextRunTimeAsc());
        }
        
        return "executions/list";
    }

    @GetMapping("/{id}")
    public String executionDetail(@PathVariable Long id, Model model) {
        Execution execution = executionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid execution Id:" + id));
        model.addAttribute("execution", execution);
        return "executions/detail";
    }

    @PostMapping("/run/feature/{id}")
    public String runFeature(@PathVariable Long id, 
                           @RequestParam(required = false, defaultValue = "Chrome") String browser,
                           @RequestParam(required = false, defaultValue = "STAGE") String environment,
                           @RequestParam(required = false) String reason,
                           @RequestParam(required = false) String notifyEmails,
                           org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        FeatureFile feature = featureFileRepository.findById(id).orElse(null);
        if (feature == null) {
            return "redirect:/features";
        }
        
        Execution execution = new Execution();
        execution.setFeatureFile(feature);
        execution.setStatus(ExecutionStatus.QUEUED);
        execution.setBrowser(browser);
        execution.setEnvironment(environment);
        execution.setExecutionType(ExecutionType.MANUAL);
        execution.setReason(reason);
        execution.setNotifyEmails(notifyEmails);
        executionRepository.save(execution);
        
        // executionEngineService.runExecution(execution); // Removed: handled by ExecutionQueueManager
        
        com.bdd.portal.config.SpringContext.getBean(com.bdd.portal.service.WebSocketNotificationService.class).broadcastExecutionUpdate(execution);
        
        redirectAttributes.addFlashAttribute("successMessage", "Test Execution has been queued");
        return "redirect:/executions";
    }

    @PostMapping("/run/feature/{id}/scenario/{slug}")
    public String runScenario(@PathVariable Long id, @PathVariable String slug,
                           @RequestParam(required = false, defaultValue = "Chrome") String browser,
                           @RequestParam(required = false, defaultValue = "STAGE") String environment,
                           @RequestParam(required = false) String reason,
                           @RequestParam(required = false) String notifyEmails,
                           org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        FeatureFile feature = featureFileRepository.findById(id).orElse(null);
        if (feature == null) {
            return "redirect:/features";
        }
        
        // Parse feature file to find line number of the scenario by slug
        java.nio.file.Path path = java.nio.file.Paths.get(com.bdd.portal.config.SpringContext.getBean(org.springframework.core.env.Environment.class).getProperty("bdd.portal.features-path"), feature.getRelativePath());
        List<com.bdd.portal.dto.ScenarioDto> scenarios = com.bdd.portal.util.FeatureParserUtil.parseFeatureFile(path);
        
        com.bdd.portal.dto.ScenarioDto targetScenario = scenarios.stream()
                .filter(s -> slug.equals(s.getSlug()))
                .findFirst()
                .orElse(null);
                
        if (targetScenario == null) {
            return "redirect:/features";
        }
        
        Execution execution = new Execution();
        execution.setFeatureFile(feature);
        
        List<String> targetScenariosList = new java.util.ArrayList<>();
        targetScenariosList.add(feature.getRelativePath() + ":" + targetScenario.getLine());
        execution.setTargetScenarios(targetScenariosList);
        
        execution.setStatus(ExecutionStatus.QUEUED);
        execution.setBrowser(browser);
        execution.setEnvironment(environment);
        execution.setExecutionType(ExecutionType.MANUAL);
        execution.setReason(reason);
        execution.setNotifyEmails(notifyEmails);
        executionRepository.save(execution);
        
        com.bdd.portal.config.SpringContext.getBean(com.bdd.portal.service.WebSocketNotificationService.class).broadcastExecutionUpdate(execution);
        
        redirectAttributes.addFlashAttribute("successMessage", "Test Execution has been queued for Scenario: " + targetScenario.getName());
        return "redirect:/executions";
    }
    
    @PostMapping("/run/feature/{id}/scenarios")
    public String runMultipleScenarios(@PathVariable Long id, @RequestParam List<String> slugs,
                           @RequestParam(required = false, defaultValue = "Chrome") String browser,
                           @RequestParam(required = false, defaultValue = "STAGE") String environment,
                           @RequestParam(required = false) String reason,
                           @RequestParam(required = false) String notifyEmails,
                           org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        FeatureFile feature = featureFileRepository.findById(id).orElse(null);
        if (feature == null) {
            return "redirect:/features";
        }
        
        java.nio.file.Path path = java.nio.file.Paths.get(com.bdd.portal.config.SpringContext.getBean(org.springframework.core.env.Environment.class).getProperty("bdd.portal.features-path"), feature.getRelativePath());
        List<com.bdd.portal.dto.ScenarioDto> scenarios = com.bdd.portal.util.FeatureParserUtil.parseFeatureFile(path);
        
        List<String> targetScenariosList = new java.util.ArrayList<>();
        for (String slug : slugs) {
            scenarios.stream()
                .filter(s -> slug.equals(s.getSlug()))
                .findFirst()
                .ifPresent(s -> targetScenariosList.add(feature.getRelativePath() + ":" + s.getLine()));
        }
        
        if (targetScenariosList.isEmpty()) {
            return "redirect:/features";
        }
        
        Execution execution = new Execution();
        execution.setFeatureFile(feature);
        execution.setTargetScenarios(targetScenariosList);
        
        execution.setStatus(ExecutionStatus.QUEUED);
        execution.setBrowser(browser);
        execution.setEnvironment(environment);
        execution.setExecutionType(ExecutionType.MANUAL);
        execution.setReason(reason);
        execution.setNotifyEmails(notifyEmails);
        executionRepository.save(execution);
        
        com.bdd.portal.config.SpringContext.getBean(com.bdd.portal.service.WebSocketNotificationService.class).broadcastExecutionUpdate(execution);
        
        redirectAttributes.addFlashAttribute("successMessage", "Test Execution has been queued for " + targetScenariosList.size() + " Scenarios");
        return "redirect:/executions";
    }
    
    @PostMapping("/run/folder")
    public String runFolder(@RequestParam String folderPath, 
                          @RequestParam(required = false, defaultValue = "Chrome") String browser,
                          @RequestParam(required = false, defaultValue = "STAGE") String environment,
                          @RequestParam(required = false) String reason,
                          @RequestParam(required = false) String notifyEmails,
                          org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        Execution execution = new Execution();
        execution.setTargetFolder(folderPath);
        execution.setStatus(ExecutionStatus.QUEUED);
        execution.setBrowser(browser);
        execution.setEnvironment(environment);
        execution.setExecutionType(ExecutionType.MANUAL);
        execution.setReason(reason);
        execution.setNotifyEmails(notifyEmails);
        executionRepository.save(execution);
        
        // executionEngineService.runExecution(execution); // Removed: handled by ExecutionQueueManager
        
        com.bdd.portal.config.SpringContext.getBean(com.bdd.portal.service.WebSocketNotificationService.class).broadcastExecutionUpdate(execution);
        
        redirectAttributes.addFlashAttribute("successMessage", "Test Execution has been queued");
        return "redirect:/executions";
    }
}
