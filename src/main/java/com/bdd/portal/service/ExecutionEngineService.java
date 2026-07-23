package com.bdd.portal.service;

import com.bdd.portal.entity.*;
import com.bdd.portal.repository.ExecutionLogRepository;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.engine.DriverManager;
import io.cucumber.core.cli.Main;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutionEngineService {

    private final ExecutionRepository executionRepository;
    private final ExecutionLogRepository executionLogRepository;
    private final WebSocketNotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    
    private final Map<Long, Thread> activeExecutions = new ConcurrentHashMap<>();

    @Value("${bdd.portal.features-path}")
    private String featuresPath;

    @Value("${bdd.portal.reports-path}")
    private String reportsPath;

    @Value("${bdd.portal.selenium-grid-url}")
    private String gridUrl;

    @Value("${vnc.public.base-url:http://localhost:7900}")
    private String publicVncBaseUrl;

    @Async
    public void runExecution(Execution execution) {
        log.info("Starting execution {}", execution.getId());

        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setStartTime(LocalDateTime.now());
        executionRepository.save(execution);
        
        notificationService.sendExecutionStatusUpdate(execution.getId(), ExecutionStatus.RUNNING.name());
        eventPublisher.publishEvent(new com.bdd.portal.event.ExecutionStartedEvent(this, execution));
        logMessage(execution, "INFO", "Execution started for browser: " + execution.getBrowser());

        activeExecutions.put(execution.getId(), Thread.currentThread());
        
        try {
            // Setup Cucumber arguments
            List<String> cucumberArgs = new ArrayList<>();
            
            // Where to look for features
            if (execution.getTargetScenarios() != null && !execution.getTargetScenarios().isEmpty()) {
                for (String scenarioPath : execution.getTargetScenarios()) {
                    cucumberArgs.add(scenarioPath); // e.g. src/test/resources/features/MyFeature.feature:15
                }
            } else if (execution.getFeatureFile() != null) {
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

            // We must use a shared allure results directory because Allure caches the system property statically.
            // All executions in the same JVM will write their results here.
            String allureResultsDir = "target/allure-results";
            System.setProperty("allure.results.directory", allureResultsDir);

            // Redirect System.out to capture Cucumber logs (Note: not thread-safe for concurrent runs, but we capture what we can)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(baos);
            PrintStream oldOut = System.out;
            System.setOut(printStream);

            byte exitStatus;
            try {
                // WebDriver initialization is now handled in Cucumber Hooks per scenario
                logMessage(execution, "INFO", "WebDriver initialization is delegated to Hooks per scenario.");

                // Run Cucumber
                exitStatus = Main.run(cucumberArgs.toArray(new String[0]), Thread.currentThread().getContextClassLoader());
            } catch (Exception e) {
                logMessage(execution, "ERROR", "Failed to execute cucumber: " + e.getMessage());
                throw new RuntimeException("Execution failed due to error", e);
            } finally {
                System.out.flush();
                System.setOut(oldOut);
                
                logMessage(execution, "INFO", "Execution finished, ensuring WebDriver is quit...");
                // Just in case anything was left over, though Hooks should handle it.
                DriverManager.quitDriver();
                DriverManager.removeBrowserType();
            }
            
            // Capture the logs
            String cucumberLogs = baos.toString();
            logMessage(execution, "INFO", cucumberLogs);

            // Generate Allure Report
            logMessage(execution, "INFO", "Generating Allure Report...");
            String reportPathStr = "bdd-reports/allure/" + execution.getId();
            Path reportPath = Paths.get(reportPathStr).toAbsolutePath();
            Path resultsPath = Paths.get(allureResultsDir).toAbsolutePath();
            
            ProcessBuilder processBuilder;
            if (Files.exists(Paths.get("./mvnw"))) {
                processBuilder = new ProcessBuilder(
                        "./mvnw", "allure:report",
                        "-Dallure.results.directory=" + resultsPath.toString(),
                        "-Dallure.report.directory=" + reportPath.toString()
                );
            } else {
                processBuilder = new ProcessBuilder(
                        "allure", "generate",
                        resultsPath.toString(),
                        "-o", reportPath.toString(),
                        "--clean"
                );
            }
            processBuilder.directory(Paths.get(System.getProperty("user.dir")).toFile());
            Process process = processBuilder.start();
            process.waitFor();
            
            // If the report was generated, there should be an index.html
            if (Files.exists(reportPath.resolve("index.html"))) {
                execution.setAllureReportPath("/reports/allure/" + execution.getId() + "/index.html");
            } else {
                logMessage(execution, "WARN", "Allure report index.html not found after generation. Check Maven logs.");
            }

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
            activeExecutions.remove(execution.getId());
            System.clearProperty("current.execution.id");
            DriverManager.removeBrowserType();
        }
    }

    public void forceStopExecution(Long executionId) {
        Thread thread = activeExecutions.get(executionId);
        if (thread != null) {
            log.info("Force stopping execution {}", executionId);
            thread.interrupt();
        }
        updateExecutionFinalState(executionId, ExecutionStatus.CANCELLED, null);
    }

    private void updateExecutionFinalState(Long executionId, ExecutionStatus finalStatus, String allurePath) {
        try {
            Execution exec = executionRepository.findById(executionId).orElse(null);
            if (exec != null && exec.getStatus() != ExecutionStatus.CANCELLED) {
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
                notificationService.broadcastExecutionUpdate(exec);
                
                if (finalStatus == ExecutionStatus.CANCELLED) {
                    eventPublisher.publishEvent(new com.bdd.portal.event.ExecutionCancelledEvent(this, exec));
                } else {
                    eventPublisher.publishEvent(new com.bdd.portal.event.ExecutionCompletedEvent(this, exec));
                }
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
