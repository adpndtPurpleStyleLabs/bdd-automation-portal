package com.bdd.portal.controller;

import com.bdd.portal.entity.Execution;
import com.bdd.portal.entity.ExecutionStatus;
import com.bdd.portal.entity.FeatureFile;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.repository.FeatureFileRepository;
import com.bdd.portal.service.ExecutionEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionRepository executionRepository;
    private final FeatureFileRepository featureFileRepository;
    private final ExecutionEngineService executionEngineService;

    @GetMapping
    public String listExecutions(Model model) {
        List<Execution> running = executionRepository.findByStatusOrderByStartTimeDesc(ExecutionStatus.RUNNING);
        List<Execution> all = executionRepository.findAll();
        
        model.addAttribute("runningExecutions", running);
        model.addAttribute("allExecutions", all);
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
                           @RequestParam(required = false, defaultValue = "STAGE") String environment) {
        
        FeatureFile feature = featureFileRepository.findById(id).orElse(null);
        if (feature == null) {
            return "redirect:/features";
        }
        
        Execution execution = new Execution();
        execution.setFeatureFile(feature);
        execution.setStatus(ExecutionStatus.QUEUED);
        execution.setBrowser(browser);
        execution.setEnvironment(environment);
        executionRepository.save(execution);
        
        executionEngineService.runExecution(execution);
        
        return "redirect:/executions/" + execution.getId();
    }
    
    @PostMapping("/run/folder")
    public String runFolder(@RequestParam String folderPath, 
                          @RequestParam(required = false, defaultValue = "Chrome") String browser,
                          @RequestParam(required = false, defaultValue = "STAGE") String environment) {
        
        Execution execution = new Execution();
        execution.setTargetFolder(folderPath);
        execution.setStatus(ExecutionStatus.QUEUED);
        execution.setBrowser(browser);
        execution.setEnvironment(environment);
        executionRepository.save(execution);
        
        executionEngineService.runExecution(execution);
        
        return "redirect:/executions/" + execution.getId();
    }
}
