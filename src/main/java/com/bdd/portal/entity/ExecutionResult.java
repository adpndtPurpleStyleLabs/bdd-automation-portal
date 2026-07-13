package com.bdd.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "execution_result")
@Getter
@Setter
public class ExecutionResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "execution_id", nullable = false)
    private Execution execution;

    @Column(nullable = false)
    private String scenarioName;

    private String status; // PASSED, FAILED, SKIPPED

    private Long durationMs;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    // Relative path to screenshot if scenario failed
    private String screenshotPath;
}
