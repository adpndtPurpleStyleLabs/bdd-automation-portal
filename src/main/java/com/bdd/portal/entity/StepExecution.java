package com.bdd.portal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "step_execution")
@Getter
@Setter
public class StepExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_execution_id", nullable = false)
    @JsonIgnore
    private ScenarioExecution scenarioExecution;

    @Column(nullable = false)
    private String stepName;

    private String keyword;
    
    private Integer lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status = ExecutionStatus.QUEUED;

    private Long durationMs;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String stackTrace;

    private String screenshotPath;
}
