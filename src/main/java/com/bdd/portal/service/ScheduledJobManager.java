package com.bdd.portal.service;

import com.bdd.portal.entity.Execution;
import com.bdd.portal.entity.ExecutionStatus;
import com.bdd.portal.entity.ExecutionType;
import com.bdd.portal.entity.ScheduledJob;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.repository.ScheduledJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobManager {

    private final ScheduledJobRepository scheduledJobRepository;
    private final ExecutionRepository executionRepository;
    private final WebSocketNotificationService notificationService;

    // Run every minute at the 0th second
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processScheduledJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledJob> dueJobs = scheduledJobRepository.findByActiveTrueAndNextRunTimeLessThanEqual(now);

        for (ScheduledJob job : dueJobs) {
            log.info("Triggering scheduled job: {}", job.getJobName());

            Execution execution = new Execution();
            execution.setScheduledJob(job);
            execution.setBrowser(job.getBrowser());
            execution.setEnvironment(job.getEnvironment());
            execution.setExecutionType(ExecutionType.SCHEDULED);
            execution.setStatus(ExecutionStatus.QUEUED);
            
            if (job.getTargetScenarios() != null) {
                execution.setTargetScenarios(new ArrayList<>(job.getTargetScenarios()));
            }

            executionRepository.save(execution);
            notificationService.broadcastExecutionUpdate(execution);

            // Update job status
            job.setLastRunTime(now);

            if (job.getCronExpression() != null && !job.getCronExpression().trim().isEmpty()) {
                try {
                    CronExpression cron = CronExpression.parse(job.getCronExpression());
                    LocalDateTime next = cron.next(now);
                    job.setNextRunTime(next);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid cron expression for job {}: {}", job.getId(), job.getCronExpression());
                    job.setActive(false);
                }
            } else {
                // One-time job
                job.setActive(false);
            }
            
            scheduledJobRepository.save(job);
        }
    }
}
