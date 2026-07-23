package com.bdd.portal.controller;

import com.bdd.portal.entity.ScheduledJob;
import com.bdd.portal.repository.FeatureFileRepository;
import com.bdd.portal.repository.ScheduledJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class ScheduledJobController {

    private final ScheduledJobRepository scheduledJobRepository;
    private final FeatureFileRepository featureFileRepository;
    private final com.bdd.portal.service.TestEnvironmentService testEnvironmentService;

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("features", featureFileRepository.findAll());
        model.addAttribute("environments", testEnvironmentService.getAllEnvironmentNames());
        return "jobs/create";
    }

    @PostMapping("/create")
    public String createJob(@RequestParam String jobName,
                            @RequestParam String environment,
                            @RequestParam String browser,
                            @RequestParam String scheduleType,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledTime,
                            @RequestParam(required = false) String cronExpression,
                            @RequestParam(required = false) List<String> targetScenarios) {

        ScheduledJob job = new ScheduledJob();
        job.setJobName(jobName);
        job.setEnvironment(environment);
        job.setBrowser(browser);
        job.setTargetScenarios(targetScenarios);
        
        if ("ONCE".equals(scheduleType)) {
            job.setScheduledTime(scheduledTime);
            job.setNextRunTime(scheduledTime);
        } else if ("RECURRING".equals(scheduleType)) {
            job.setCronExpression(cronExpression);
            try {
                org.springframework.scheduling.support.CronExpression cron = org.springframework.scheduling.support.CronExpression.parse(cronExpression);
                job.setNextRunTime(cron.next(LocalDateTime.now()));
            } catch (Exception e) {
                // Ignore parsing errors for now, real app should validate
                job.setActive(false); 
            }
        }
        
        scheduledJobRepository.save(job);
        
        return "redirect:/executions?type=SCHEDULED";
    }
}
