package com.bdd.portal.service;

import com.bdd.portal.entity.*;
import com.bdd.portal.repository.ExecutionLogRepository;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.repository.ExecutionResultRepository;
import io.cucumber.core.cli.Main;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionEngineService {

    private final ExecutionRepository executionRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final ExecutionResultRepository executionResultRepository;
    private final WebSocketNotificationService notificationService;

    @Value("${bdd.portal.features-path}")
    private String featuresPath;

    @Value("${bdd.portal.reports-path}")
    private String reportsPath;

    @Async
    public void runExecution(Execution execution) {
        log.info("Starting execution {}", execution.getId());

        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartTime(LocalDateTime.now());
        executionRepository.save(execution);
        
        notificationService.sendExecutionStatusUpdate(execution.getId(), ExecutionStatus.RUNNING.name());
        logMessage(execution, "INFO", "Execution started for browser: " + execution.getBrowser());

        try {
            // Setup Cucumber arguments
            List<String> cucumberArgs = new ArrayList<>();
            
            // Where to look for features
            if (execution.getFeatureFile() != null) {
                Path featurePath = Paths.get(featuresPath, execution.getFeatureFile().getRelativePath());
                cucumberArgs.add(featurePath.toString());
            } else if (execution.getTargetFolder() != null) {
                Path folderPath = Paths.get(featuresPath, execution.getTargetFolder());
                cucumberArgs.add(folderPath.toString());
            } else {
                cucumberArgs.add(featuresPath); // Run all
            }

            // Add glue code (step definitions)
            cucumberArgs.add("--glue");
//            cucumberArgs.add("com.bdd.portal.engine.steps");
            cucumberArgs.add("com.bdd.portal.engine.magento.stepDefination");

            // Add Allure plugin
            cucumberArgs.add("--plugin");
            cucumberArgs.add("io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm");
            
            // Add standard output plugin
            cucumberArgs.add("--plugin");
            cucumberArgs.add("pretty");

            logMessage(execution, "INFO", "Executing Cucumber with args: " + String.join(" ", cucumberArgs));

            // Redirect System.out to capture Cucumber logs
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(baos);
            PrintStream oldOut = System.out;
            System.setOut(printStream);

            // Run Cucumber
            byte exitStatus = Main.run(cucumberArgs.toArray(new String[0]), Thread.currentThread().getContextClassLoader());

            System.out.flush();
            System.setOut(oldOut);
            
            // Capture the logs
            String cucumberLogs = baos.toString();
            logMessage(execution, "INFO", cucumberLogs);

            if (exitStatus == 0) {
                execution.setStatus(ExecutionStatus.PASSED);
                logMessage(execution, "INFO", "Execution PASSED");
            } else {
                execution.setStatus(ExecutionStatus.FAILED);
                logMessage(execution, "ERROR", "Execution FAILED with exit code " + exitStatus);
            }

            // In a real scenario, we would parse the cucumber result JSON or Allure results 
            // to populate ExecutionResult table. For this example, we mock a result.
            createMockResult(execution);

        } catch (Exception e) {
            log.error("Execution failed", e);
            execution.setStatus(ExecutionStatus.FAILED);
            logMessage(execution, "ERROR", "Exception during execution: " + e.getMessage());
        } finally {
            execution.setEndTime(LocalDateTime.now());
            execution.setDurationMs(java.time.Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
            executionRepository.save(execution);
            notificationService.sendExecutionStatusUpdate(execution.getId(), execution.getStatus().name());
        }
    }

    private void logMessage(Execution execution, String level, String message) {
        ExecutionLog logEntry = new ExecutionLog();
        logEntry.setExecution(execution);
        logEntry.setLevel(level);
        logEntry.setMessage(message);
        logEntry.setTimestamp(LocalDateTime.now());
        executionLogRepository.save(logEntry);
        
        notificationService.sendExecutionLog(execution.getId(), message);
    }
    
    private void createMockResult(Execution execution) {
        ExecutionResult result = new ExecutionResult();
        result.setExecution(execution);
        result.setScenarioName("Sample Scenario for " + (execution.getFeatureFile() != null ? execution.getFeatureFile().getName() : "Folder"));
        result.setStatus(execution.getStatus() == ExecutionStatus.PASSED ? "PASSED" : "FAILED");
        result.setDurationMs(1500L);
        executionResultRepository.save(result);
        
        if ("PASSED".equals(result.getStatus())) {
            execution.setPassedScenarios(1);
        } else {
            execution.setFailedScenarios(1);
        }
    }
}
