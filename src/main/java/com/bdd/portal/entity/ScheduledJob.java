package com.bdd.portal.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "scheduled_jobs")
@Data
public class ScheduledJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobName;
    private String environment;
    private String browser;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_target_scenarios", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "scenario_path")
    private List<String> targetScenarios; // Stores path:lineNumber or tag

    private String cronExpression; // Null if one-time
    private LocalDateTime scheduledTime; // For one-time

    private boolean active = true;

    private LocalDateTime nextRunTime;
    private LocalDateTime lastRunTime;
    
    private LocalDateTime createdAt = LocalDateTime.now();
}
