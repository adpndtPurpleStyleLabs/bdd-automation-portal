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

    @Value("${vnc.public.base-url:http://localhost:7900}")
    private String publicVncBaseUrl;

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
                // Initialize WebDriver once for the entire execution
                String browser = execution.getBrowser();
                logMessage(execution, "INFO", "Initializing WebDriver for browser: " + browser);
                WebDriver driver;
                
                // If Grid URL is explicitly provided, use Grid, otherwise fallback to local DEV execution
                if (gridUrl != null && !gridUrl.isEmpty()) {
                    java.net.URL gridHubUrl = new java.net.URL(gridUrl);
                    if ("Firefox".equalsIgnoreCase(browser)) {
                        driver = new org.openqa.selenium.remote.RemoteWebDriver(gridHubUrl, new org.openqa.selenium.firefox.FirefoxOptions());
                    } else if ("Edge".equalsIgnoreCase(browser)) {
                        driver = new org.openqa.selenium.remote.RemoteWebDriver(gridHubUrl, new org.openqa.selenium.edge.EdgeOptions());
                    } else {
                        org.openqa.selenium.chrome.ChromeOptions options = new org.openqa.selenium.chrome.ChromeOptions();
                        driver = new org.openqa.selenium.remote.RemoteWebDriver(gridHubUrl, options);
                    }
                    
                    org.openqa.selenium.Capabilities caps = ((org.openqa.selenium.remote.RemoteWebDriver) driver).getCapabilities();
                    execution.setSeleniumSessionId(((org.openqa.selenium.remote.RemoteWebDriver) driver).getSessionId().toString());
                    execution.setBrowserVersion(caps.getBrowserVersion());
                    if (caps.getPlatformName() != null) {
                        execution.setPlatform(caps.getPlatformName().name());
                    }
                    
                    // The backend must simply append the required parameters to the configured base URL
                    String publicVncUrl = publicVncBaseUrl.endsWith("/") ? 
                            publicVncBaseUrl.substring(0, publicVncBaseUrl.length() - 1) : publicVncBaseUrl;
                    publicVncUrl += "/?autoconnect=1&password=secret&resize=scale";
                    
                    execution.setNoVncUrl(publicVncUrl);
                    execution.setVncUrl(publicVncUrl); // Maintained for backward compatibility
                    
                    executionRepository.save(execution);
                    notificationService.broadcastExecutionUpdate(execution);
                    
                    logMessage(execution, "INFO", "WebDriver initialized successfully from Grid.");
                } else {
                    logMessage(execution, "INFO", "Grid URL not found. Initializing local DEV ChromeDriver.");
                    org.openqa.selenium.chrome.ChromeOptions options = new org.openqa.selenium.chrome.ChromeOptions();
                    options.addArguments("--remote-allow-origins=*");
                    options.addArguments("disable-notifications");
                    options.addArguments("start-maximized");
                    options.addArguments("--disable-notifications");
                    driver = io.github.bonigarcia.wdm.WebDriverManager.chromedriver().capabilities(options).create();
                }

                driver.manage().timeouts().pageLoadTimeout(java.time.Duration.ofSeconds(60));
                driver.manage().timeouts().implicitlyWait(java.time.Duration.ofSeconds(10));
                
                DriverManager.setBrowserType(browser);
                DriverManager.setGridUrl(gridUrl);
                DriverManager.setEnvironment(execution.getEnvironment());
                DriverManager.setDriver(driver);

                // Run Cucumber
                exitStatus = Main.run(cucumberArgs.toArray(new String[0]), Thread.currentThread().getContextClassLoader());
            } catch (Exception e) {
                logMessage(execution, "ERROR", "Failed to initialize WebDriver: " + e.getMessage());
                throw new RuntimeException("Execution failed due to WebDriver initialization error", e);
            } finally {
                System.out.flush();
                System.setOut(oldOut);
                
                logMessage(execution, "INFO", "Quitting WebDriver...");
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
                notificationService.broadcastExecutionUpdate(exec);
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
