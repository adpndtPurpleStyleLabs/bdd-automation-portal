package com.bdd.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "execution")
@Getter
@Setter
public class Execution {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "feature_file_id")
    private FeatureFile featureFile;

    // Could be a folder path instead of a specific feature file
    private String targetFolder;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "execution_target_scenarios", joinColumns = @JoinColumn(name = "execution_id"))
    @Column(name = "scenario_path")
    private List<String> targetScenarios = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "scheduled_job_id")
    private ScheduledJob scheduledJob;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User startedBy;

    private String gridUrl;
    private String seleniumSessionId;
    private String vncUrl;

    // New Grid metadata
    private String gridNodeId;
    private String gridNodeUri;
    private String containerId;
    private String noVncUrl;
    private String browserVersion;
    private String platform;

    private String browser;
    private String environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status = ExecutionStatus.QUEUED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionType executionType = ExecutionType.MANUAL;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Long durationMs; // in milliseconds

    private int passedScenarios;
    private int failedScenarios;
    private int skippedScenarios;

    private String allureReportPath;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeatureExecution> featureExecutions = new ArrayList<>();

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExecutionLog> logs = new ArrayList<>();

    public void addFeatureExecution(FeatureExecution featureExecution) {
        featureExecutions.add(featureExecution);
        featureExecution.setExecution(this);
    }
}
