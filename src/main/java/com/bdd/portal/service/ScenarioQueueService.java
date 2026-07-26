package com.bdd.portal.service;

import com.bdd.portal.entity.Execution;
import com.bdd.portal.entity.ExecutionStatus;
import com.bdd.portal.entity.ScenarioExecution;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.repository.ScenarioExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScenarioQueueService {

    private final ScenarioExecutionRepository scenarioExecutionRepository;
    private final ExecutionRepository executionRepository;
    private final WebSocketNotificationService notificationService;

    @Transactional
    public Optional<ScenarioExecution> getNextScenario(String workerId) {
        // Optimistically lock by updating exactly one queued scenario
        int updated = scenarioExecutionRepository.lockNextQueuedScenario(workerId);
        
        if (updated > 0) {
            // We got one, let's fetch it
            Optional<ScenarioExecution> scenarioOpt = scenarioExecutionRepository.findLockedScenarioByWorker(workerId);
            
            if (scenarioOpt.isPresent()) {
                ScenarioExecution scenario = scenarioOpt.get();
                // status is already RUNNING from the UPDATE query, but we save to trigger JPA lifecycle events
                scenarioExecutionRepository.save(scenario);
                
                // Update parent execution stats (could be optimized or done periodically)
                updateExecutionStats(scenario.getExecution().getId());
                
                return Optional.of(scenario);
            }
        }
        
        return Optional.empty();
    }
    
    @Transactional
    public void markScenarioComplete(String scenarioUuid, ExecutionStatus finalStatus) {
        scenarioExecutionRepository.findByScenarioExecutionUuid(scenarioUuid).ifPresent(scenario -> {
            scenario.setStatus(finalStatus);
            scenario.setEndTime(LocalDateTime.now());
            if (scenario.getStartTime() != null) {
                scenario.setDurationMs(java.time.Duration.between(scenario.getStartTime(), scenario.getEndTime()).toMillis());
            }
            scenarioExecutionRepository.save(scenario);
            
            updateExecutionStats(scenario.getExecution().getId());
        });
    }

    private void updateExecutionStats(Long executionId) {
        executionRepository.findById(executionId).ifPresent(execution -> {
            long running = scenarioExecutionRepository.findByExecutionId(executionId).stream()
                .filter(s -> s.getStatus() == ExecutionStatus.RUNNING)
                .count();
                
            long passed = scenarioExecutionRepository.findByExecutionId(executionId).stream()
                .filter(s -> s.getStatus() == ExecutionStatus.PASSED)
                .count();
                
            long failed = scenarioExecutionRepository.findByExecutionId(executionId).stream()
                .filter(s -> s.getStatus() == ExecutionStatus.FAILED)
                .count();
                
            long queued = scenarioExecutionRepository.findByExecutionId(executionId).stream()
                .filter(s -> s.getStatus() == ExecutionStatus.QUEUED)
                .count();
                
            long skipped = scenarioExecutionRepository.findByExecutionId(executionId).stream()
                .filter(s -> s.getStatus() == ExecutionStatus.SKIPPED)
                .count();
                
            execution.setRunningScenarios((int) running);
            execution.setPassedScenarios((int) passed);
            execution.setFailedScenarios((int) failed);
            execution.setQueuedScenarios((int) queued);
            execution.setSkippedScenarios((int) skipped);
            
            if (queued == 0 && running == 0) {
                if (failed > 0) {
                    execution.setStatus(ExecutionStatus.FAILED);
                } else {
                    execution.setStatus(ExecutionStatus.PASSED);
                }
                execution.setEndTime(LocalDateTime.now());
                if (execution.getStartTime() != null) {
                    execution.setDurationMs(java.time.Duration.between(execution.getStartTime(), execution.getEndTime()).toMillis());
                }
            } else if (execution.getStatus() != ExecutionStatus.RUNNING && running > 0) {
                execution.setStatus(ExecutionStatus.RUNNING);
                if (execution.getStartTime() == null) {
                    execution.setStartTime(LocalDateTime.now());
                }
            }
            
            executionRepository.save(execution);
            notificationService.broadcastExecutionUpdate(execution);
        });
    }
}
