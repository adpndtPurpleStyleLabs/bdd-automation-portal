package com.bdd.portal.controller;

import com.bdd.portal.entity.Execution;
import com.bdd.portal.entity.ExecutionStatus;
import com.bdd.portal.entity.ExecutionType;
import com.bdd.portal.entity.FeatureFile;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.repository.FeatureFileRepository;
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

@Controller
@RequestMapping("/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionRepository executionRepository;
    private final FeatureFileRepository featureFileRepository;
    private final ExecutionEngineService executionEngineService;

    @GetMapping
    public String listExecutions(
            @RequestParam(required = false) ExecutionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startTime"));
        
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
        executionRepository.save(execution);
        
        executionEngineService.runExecution(execution);
        
        redirectAttributes.addFlashAttribute("successMessage", "Test Execution has been queued");
        return "redirect:/executions/" + execution.getId();
    }
    
    @PostMapping("/run/folder")
    public String runFolder(@RequestParam String folderPath, 
                          @RequestParam(required = false, defaultValue = "Chrome") String browser,
                          @RequestParam(required = false, defaultValue = "STAGE") String environment,
                          org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        Execution execution = new Execution();
        execution.setTargetFolder(folderPath);
        execution.setStatus(ExecutionStatus.QUEUED);
        execution.setBrowser(browser);
        execution.setEnvironment(environment);
        execution.setExecutionType(ExecutionType.MANUAL);
        executionRepository.save(execution);
        
        executionEngineService.runExecution(execution);
        
        redirectAttributes.addFlashAttribute("successMessage", "Test Execution has been queued");
        return "redirect:/executions/" + execution.getId();
    }
}
