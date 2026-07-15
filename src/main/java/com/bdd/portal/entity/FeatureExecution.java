package com.bdd.portal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feature_execution")
@Getter
@Setter
public class FeatureExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    @JsonIgnore
    private Execution execution;

    @Column(nullable = false)
    private String featureName;

    private String uri;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status = ExecutionStatus.QUEUED;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;

    @OneToMany(mappedBy = "featureExecution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScenarioExecution> scenarios = new ArrayList<>();
}
