    package com.bdd.portal.engine.reporting;

import com.bdd.portal.entity.Execution;
import com.bdd.portal.entity.ExecutionStatus;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.service.ExecutionEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionQueueManager {

    private final ExecutionRepository executionRepository;
    private final ExecutionEngineService executionEngineService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${test.environment:dev}")
    private String environment;

    @Value("${bdd.portal.selenium-grid-url:http://localhost:4444/wd/hub}")
    private String gridUrl;

    @Scheduled(fixedDelay = 5000)
    public void processQueue() {
        List<Execution> queuedExecutions = executionRepository.findByStatusOrderByIdAsc(ExecutionStatus.QUEUED);
        if (queuedExecutions.isEmpty()) {
            return;
        }

        // We process each queued execution sequentially to decide if it can run
        java.util.Map<String, java.util.concurrent.atomic.AtomicLong> runningCounts = new java.util.HashMap<>();

        for (Execution execution : queuedExecutions) {
            String browser = execution.getBrowser();
            if (browser == null) {
                browser = "chrome";
            }

            java.util.concurrent.atomic.AtomicLong runningCounter = runningCounts.computeIfAbsent(browser.toLowerCase(), 
                b -> new java.util.concurrent.atomic.AtomicLong(executionRepository.countByStatusAndBrowserIgnoreCase(ExecutionStatus.RUNNING, b)));

            if ("prod".equalsIgnoreCase(environment)) {
                int maxCapacity = getGridCapacityForBrowser(browser);
                
                if (runningCounter.get() < maxCapacity) {
                    startExecution(execution, runningCounter);
                } else {
                    log.debug("Grid capacity reached for {}. Max: {}, Running: {}. Waiting...", browser, maxCapacity, runningCounter.get());
                    // Wait for resources for this browser
                }
            } else {
                // In non-prod environments, use a default generous limit (e.g., 5)
                if (runningCounter.get() < 5) {
                    startExecution(execution, runningCounter);
                }
            }
        }
    }

    private void startExecution(Execution execution, java.util.concurrent.atomic.AtomicLong currentlyRunningCounter) {
        log.info("QueueManager dispatching execution {} from QUEUED to RUNNING", execution.getId());
        execution.setStatus(ExecutionStatus.RUNNING);
        executionRepository.save(execution);
        
        com.bdd.portal.config.SpringContext.getBean(com.bdd.portal.service.WebSocketNotificationService.class).broadcastExecutionUpdate(execution);
        
        currentlyRunningCounter.incrementAndGet();
        
        executionEngineService.runExecution(execution);
    }

    private int getGridCapacityForBrowser(String targetBrowser) {
        try {
            String statusUrl = gridUrl.replace("/wd/hub", "/status");
            GridStatusResponse response = restTemplate.getForObject(statusUrl, GridStatusResponse.class);
            
            if (response != null && response.getValue() != null && response.getValue().getNodes() != null) {
                int capacity = 0;
                for (GridNode node : response.getValue().getNodes()) {
                    if (node.getSlots() != null) {
                        for (GridSlot slot : node.getSlots()) {
                            if (slot.getStereotype() != null && 
                                targetBrowser.equalsIgnoreCase(slot.getStereotype().getBrowserName())) {
                                capacity++;
                            }
                        }
                    }
                }
                return capacity;
            }
        } catch (Exception e) {
            log.error("Failed to query Selenium Grid status from {}: {}", gridUrl, e.getMessage());
        }
        return 0;
    }
}
