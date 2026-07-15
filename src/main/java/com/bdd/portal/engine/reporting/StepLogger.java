package com.bdd.portal.engine.reporting;

import com.bdd.portal.config.SpringContext;
import com.bdd.portal.entity.StepExecution;
import com.bdd.portal.repository.StepExecutionRepository;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
public class StepLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final String SCREENSHOT_DIR = "src/main/resources/static/screenshots/";
    private static final String SCREENSHOT_URL_PREFIX = "/screenshots/";

    public static void log(String message) {
        log("INFO", message);
    }

    public static void log(String level, String message) {
        Long currentStepId = DatabaseReportingPlugin.currentStepId.get();
        if (currentStepId == null) {
            log.warn("Cannot log step-wise message, no active step found: [{}] {}", level, message);
            return;
        }

        try {
            StepExecutionRepository stepRepo = SpringContext.getBean(StepExecutionRepository.class);
            Optional<StepExecution> stepOpt = stepRepo.findById(currentStepId);
            
            if (stepOpt.isPresent()) {
                StepExecution step = stepOpt.get();
                String timestamp = LocalDateTime.now().format(FORMATTER);
                String logEntry = "[" + timestamp + "] [" + level + "] " + message + "\n";
                
                String existingLogs = step.getStepLog() != null ? step.getStepLog() : "";
                step.setStepLog(existingLogs + logEntry);
                
                stepRepo.save(step);
            }
        } catch (Exception e) {
            log.error("Failed to save step log", e);
        }
    }

    public static void takeScreenshot(WebDriver driver) {
        Long currentStepId = DatabaseReportingPlugin.currentStepId.get();
        if (currentStepId == null) {
            log.warn("Cannot attach screenshot, no active step found.");
            return;
        }

        if (driver instanceof TakesScreenshot) {
            try {
                // Ensure directory exists
                File dir = new File(SCREENSHOT_DIR);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                // Generate filename
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"));
                String filename = "step_screenshot_" + timestamp + ".png";
                File destFile = new File(dir, filename);

                // Capture
                byte[] screenshotData = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                try (FileOutputStream fos = new FileOutputStream(destFile)) {
                    fos.write(screenshotData);
                }

                // Attach to step log
                StepExecutionRepository stepRepo = SpringContext.getBean(StepExecutionRepository.class);
                Optional<StepExecution> stepOpt = stepRepo.findById(currentStepId);
                
                if (stepOpt.isPresent()) {
                    StepExecution step = stepOpt.get();
                    
                    // Log the screenshot path
                    String logTimestamp = LocalDateTime.now().format(FORMATTER);
                    String screenshotPath = SCREENSHOT_URL_PREFIX + filename;
                    String logEntry = "[" + logTimestamp + "] [SCREENSHOT] " + screenshotPath + "\n";
                    
                    String existingLogs = step.getStepLog() != null ? step.getStepLog() : "";
                    step.setStepLog(existingLogs + logEntry);
                    
                    stepRepo.save(step);
                }
            } catch (IOException e) {
                log.error("Failed to save screenshot", e);
            } catch (Exception e) {
                log.error("Failed to attach screenshot to step", e);
            }
        } else {
            log.warn("WebDriver does not support TakesScreenshot");
        }
    }
}
