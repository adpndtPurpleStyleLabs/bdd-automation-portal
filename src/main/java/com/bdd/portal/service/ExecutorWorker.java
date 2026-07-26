package com.bdd.portal.service;

import com.bdd.portal.entity.ExecutionStatus;
import com.bdd.portal.entity.ScenarioExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExecutorWorker {

    private final ScenarioQueueService scenarioQueueService;
    private final ExecutionEngineService executionEngineService;
    
    @Value("${bdd.portal.max-workers:2}")
    private int maxWorkers;
    
    private ExecutorService executorService;
    private final String workerNodeId = "worker-" + UUID.randomUUID().toString().substring(0, 8);
    private final AtomicBoolean isPolling = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(maxWorkers);
        log.info("Initialized ExecutorWorker pool with {} concurrent workers", maxWorkers);
    }
    
    @PreDestroy
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Scheduled(fixedDelay = 2000)
    public void pollQueue() {
        if (!isPolling.compareAndSet(false, true)) {
            return;
        }
        
        try {
            for (int i = 0; i < maxWorkers; i++) {
                executorService.submit(() -> {
                    boolean keepPolling = true;
                    while (keepPolling) {
                        try {
                            String threadWorkerId = workerNodeId + "-" + Thread.currentThread().getId();
                            Optional<ScenarioExecution> scenarioOpt = scenarioQueueService.getNextScenario(threadWorkerId);
                            
                            if (scenarioOpt.isPresent()) {
                                ScenarioExecution scenario = scenarioOpt.get();
                                log.info("Worker {} claimed scenario {} for execution", threadWorkerId, scenario.getScenarioExecutionUuid());
                                
                                try {
                                    executionEngineService.runScenario(scenario);
                                    scenarioQueueService.markScenarioComplete(scenario.getScenarioExecutionUuid(), ExecutionStatus.PASSED);
                                } catch (Exception e) {
                                    log.error("Execution failed for scenario {}", scenario.getScenarioExecutionUuid(), e);
                                    scenarioQueueService.markScenarioComplete(scenario.getScenarioExecutionUuid(), ExecutionStatus.FAILED);
                                }
                            } else {
                                keepPolling = false; // Queue empty, wait for next scheduled tick
                            }
                        } catch (Exception e) {
                            log.error("Worker error during polling", e);
                            keepPolling = false;
                        }
                    }
                });
            }
        } finally {
            isPolling.set(false);
        }
    }
}
