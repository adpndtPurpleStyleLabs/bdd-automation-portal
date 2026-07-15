package com.bdd.portal.service;

import com.bdd.portal.entity.*;
import com.bdd.portal.repository.ExecutionLogRepository;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.engine.DriverManager;
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
    private final WebSocketNotificationService notificationService;

    @Value("${bdd.portal.features-path}")
    private String featuresPath;

    @Value("${bdd.portal.reports-path}")
    private String reportsPath;

    @Value("${bdd.portal.selenium-grid-url}")
    private String gridUrl;

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
            
            // Add custom database reporting plugin
            cucumberArgs.add("--plugin");
            cucumberArgs.add("com.bdd.portal.engine.reporting.DatabaseReportingPlugin");

            logMessage(execution, "INFO", "Executing Cucumber with args: " + String.join(" ", cucumberArgs));

            // Set execution ID for plugin
            System.setProperty("current.execution.id", execution.getId().toString());

            // Set Allure results directory specifically for this execution
            String allureResultsDir = "target/allure-results/" + execution.getId();
            System.setProperty("allure.results.directory", allureResultsDir);

            // Redirect System.out to capture Cucumber logs
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(baos);
            PrintStream oldOut = System.out;
            System.setOut(printStream);

            // Run Cucumber
            DriverManager.setBrowserType(execution.getBrowser());
            DriverManager.setGridUrl(gridUrl);
            DriverManager.setEnvironment(execution.getEnvironment());
            byte exitStatus = Main.run(cucumberArgs.toArray(new String[0]), Thread.currentThread().getContextClassLoader());

            System.out.flush();
            System.setOut(oldOut);
            
            // Capture the logs
            String cucumberLogs = baos.toString();
            logMessage(execution, "INFO", cucumberLogs);

            // Generate Allure Report
            logMessage(execution, "INFO", "Generating Allure Report...");
            String reportPathStr = "bdd-reports/allure/" + execution.getId();
            Path reportPath = Paths.get(reportPathStr).toAbsolutePath();
            Path resultsPath = Paths.get(allureResultsDir).toAbsolutePath();
            
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "./mvnw", "allure:report",
                    "-Dallure.results.directory=" + resultsPath.toString(),
                    "-Dallure.report.directory=" + reportPath.toString()
            );
            // On Windows, use mvnw.cmd if necessary, but assuming Linux/Mac based on ./mvnw
            processBuilder.directory(Paths.get(System.getProperty("user.dir")).toFile());
            Process process = processBuilder.start();
            process.waitFor();

            execution.setAllureReportPath("/reports/allure/" + execution.getId() + "/index.html");

            ExecutionStatus finalStatus;
            if (exitStatus == 0) {
                finalStatus = ExecutionStatus.PASSED;
                logMessage(execution, "INFO", "Execution PASSED");
            } else {
                finalStatus = ExecutionStatus.FAILED;
                logMessage(execution, "ERROR", "Execution FAILED with exit code " + exitStatus);
            }

            updateExecutionFinalState(execution.getId(), finalStatus, "/reports/allure/" + execution.getId() + "/index.html");
            
        } catch (Exception e) {
            log.error("Execution failed", e);
            logMessage(execution, "ERROR", "Exception during execution: " + e.getMessage());
            updateExecutionFinalState(execution.getId(), ExecutionStatus.FAILED, null);
        } finally {
            System.clearProperty("current.execution.id");
            DriverManager.removeBrowserType();
        }
    }

    private void updateExecutionFinalState(Long executionId, ExecutionStatus finalStatus, String allurePath) {
        try {
            Execution exec = executionRepository.findById(executionId).orElse(null);
            if (exec != null) {
                exec.setStatus(finalStatus);
                exec.setEndTime(LocalDateTime.now());
                if (exec.getStartTime() != null) {
                    exec.setDurationMs(java.time.Duration.between(exec.getStartTime(), exec.getEndTime()).toMillis());
                }
                if (allurePath != null) {
                    exec.setAllureReportPath(allurePath);
                }
                executionRepository.save(exec);
                notificationService.sendExecutionStatusUpdate(executionId, finalStatus.name());
            }
        } catch (Exception e) {
            log.error("Failed to update final execution state", e);
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
}
