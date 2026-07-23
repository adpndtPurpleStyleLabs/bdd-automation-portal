package com.bdd.portal.controller;

import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.entity.Execution;
import com.bdd.portal.emailer.entity.EmailQueue;
import com.bdd.portal.emailer.repository.EmailQueueRepository;
import com.bdd.portal.emailer.service.EmailTemplateService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bdd.portal.service.ExecutionEngineService;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class ExecutionRestController {

    private final ExecutionRepository executionRepository;
    private final EmailQueueRepository emailQueueRepository;
    private final EmailTemplateService emailTemplateService;
    private final ExecutionEngineService executionEngineService;

    @PostMapping("/{id}/share")
    public ResponseEntity<?> shareExecutionViaEmail(@PathVariable Long id, @RequestBody Map<String, List<String>> request) {
        Execution execution = executionRepository.findById(id).orElse(null);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        
        List<String> emails = request.get("emails");
        if (emails == null || emails.isEmpty()) {
            return ResponseEntity.badRequest().body("No emails provided");
        }
        
        boolean isPassed = execution.getStatus() == com.bdd.portal.entity.ExecutionStatus.PASSED;
        String status = isPassed ? "Passed" : "Failed";
        String title = execution.getTargetFolder() != null ? execution.getTargetFolder() : (execution.getFeatureFile() != null ? execution.getFeatureFile().getName() : "Manual Run");
        String attachments = execution.getAllureReportPath() != null ? execution.getAllureReportPath() : null;
        
        for (String rawEmail : emails) {
            String clean = rawEmail.trim();
            if (clean.startsWith("[")) {
                clean = clean.replaceAll("\\[|\\]|\\{|\\}|\"value\":|\"", "");
            }
            String[] splitEmails = clean.split(",");
            for (String email : splitEmails) {
                if (!email.trim().isEmpty()) {
                    EmailQueue queue = new EmailQueue();
                    queue.setRecipient(email.trim());
                    queue.setSubject("Execution Report " + status + ": " + title);
                    queue.setBody(emailTemplateService.generateExecutionEmail(execution, "execution-completed.html"));
                    queue.setAttachments(attachments);
                    emailQueueRepository.save(queue);
                }
            }
        }
        
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<?> stopExecution(@PathVariable Long id) {
        Execution execution = executionRepository.findById(id).orElse(null);
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        if (execution.getStatus() != com.bdd.portal.entity.ExecutionStatus.RUNNING && 
            execution.getStatus() != com.bdd.portal.entity.ExecutionStatus.QUEUED) {
            return ResponseEntity.badRequest().body("Execution is not in a stoppable state");
        }
        
        executionEngineService.forceStopExecution(id);
        return ResponseEntity.ok(Map.of("message", "Execution stopped successfully"));
    }

    @GetMapping("/emails/used")
    public ResponseEntity<List<String>> getUsedEmails() {
        List<String> rawEmails = executionRepository.findDistinctNotifyEmails();
        
        Set<String> uniqueEmails = rawEmails.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .flatMap(s -> {
                    String clean = s.trim();
                    if (clean.startsWith("[")) {
                        clean = clean.replaceAll("\\[|\\]|\\{|\\}|\"value\":|\"", "");
                    }
                    return Arrays.stream(clean.split(","));
                })
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
                
        return ResponseEntity.ok(uniqueEmails.stream().sorted().collect(Collectors.toList()));
    }
}
