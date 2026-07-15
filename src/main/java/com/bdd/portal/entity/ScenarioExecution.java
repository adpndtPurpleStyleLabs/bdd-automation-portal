package com.bdd.portal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scenario_execution")
@Getter
@Setter
public class ScenarioExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_execution_id", nullable = false)
    @JsonIgnore
    private FeatureExecution featureExecution;

    @Column(nullable = false)
    private String scenarioName;

    private Integer lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status = ExecutionStatus.QUEUED;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;

    @OneToMany(mappedBy = "scenarioExecution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StepExecution> steps = new ArrayList<>();
}
