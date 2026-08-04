package com.bdd.portal.service;

import com.bdd.portal.entity.*;
import com.bdd.portal.engine.DriverManager;
import com.bdd.portal.engine.ScenarioContext;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.repository.ExecutionLogRepository;
import io.cucumber.core.cli.Main;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @Value("${bdd.portal.features-path:src/test/resources/features}")
    private String featuresPath;

    @Value("${bdd.portal.reports-path:target/allure-results}")
    private String reportsPath;

    public void runScenario(ScenarioExecution scenario) {
        log.info("Starting scenario execution {}", scenario.getScenarioExecutionUuid());

        Execution execution = scenario.getExecution();
        
        try {
            List<String> cucumberArgs = new ArrayList<>();
            
            // 1. Target the specific scenario by line number
            Path resolvedPath = Paths.get(featuresPath, scenario.getFeatureUri());
            // However, scenario.getFeatureUri() might just be the URI string like "file:/...".
            // Since we stored it in the DB as a URI or feature path, let's assume it has the relative path if possible.
            // In ScenarioDiscoveryService, we did: feature.getUri().toString() which might be an absolute URI depending on parser.
            // A safer bet is just using the absolute path or extracting it.
            String targetPath = scenario.getFeatureUri();
            if (targetPath.startsWith("file:")) {
                targetPath = targetPath.substring(5); // strip file:
            }
            cucumberArgs.add(targetPath + ":" + scenario.getLineNumber());

            // 2. Glue
            cucumberArgs.add("--glue");
            cucumberArgs.add("com.bdd.portal.engine.magento.stepDefination");

            // 3. Allure plugin
            cucumberArgs.add("--plugin");
            cucumberArgs.add("io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm");
            
            cucumberArgs.add("--plugin");
            cucumberArgs.add("pretty");
            
            // 4. Custom plugin with parameter!
            cucumberArgs.add("--plugin");
            cucumberArgs.add("com.bdd.portal.engine.reporting.DatabaseReportingPlugin:" + scenario.getScenarioExecutionUuid());

            log.info("Executing Cucumber with args: {}", String.join(" ", cucumberArgs));
            
            ScenarioContext.setScenarioExecution(scenario);

            // Run Cucumber
            byte exitStatus = Main.run(cucumberArgs.toArray(new String[0]), Thread.currentThread().getContextClassLoader());
            
            if (exitStatus != 0) {
                log.warn("Cucumber run finished with non-zero exit code: {}", exitStatus);
                throw new RuntimeException("Cucumber execution failed with exit code: " + exitStatus);
            }

        } catch (Exception e) {
            log.error("Failed to execute scenario {}", scenario.getScenarioExecutionUuid(), e);
            throw new RuntimeException("Scenario execution failed", e);
        } finally {
            ScenarioContext.clear();
            // Guarantee WebDriver cleanup for this thread.
            DriverManager.quitDriver();
            DriverManager.removeBrowserType();
        }
    }
    
    public void forceStopExecution(Long executionId) {
        log.info("Force stopping execution {}", executionId);
        // DB update logic for cancellation (to be implemented if needed)
        executionRepository.findById(executionId).ifPresent(exec -> {
            exec.setStatus(ExecutionStatus.CANCELLED);
            executionRepository.save(exec);
            notificationService.broadcastExecutionUpdate(exec);
        });
    }
}
