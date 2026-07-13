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

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User startedBy;

    private String browser;
    private String environment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status = ExecutionStatus.QUEUED;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Long durationMs; // in milliseconds

    private int passedScenarios;
    private int failedScenarios;
    private int skippedScenarios;

    private String allureReportPath;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExecutionResult> results = new ArrayList<>();

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ExecutionLog> logs = new ArrayList<>();

    public void addResult(ExecutionResult result) {
        results.add(result);
        result.setExecution(this);
    }
}
