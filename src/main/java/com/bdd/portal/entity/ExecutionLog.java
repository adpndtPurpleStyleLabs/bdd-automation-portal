package com.bdd.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "execution_log")
@Getter
@Setter
public class ExecutionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "execution_id", nullable = false)
    private Execution execution;

    private LocalDateTime timestamp;

    private String level; // INFO, ERROR, WARN

    @Column(columnDefinition = "TEXT")
    private String message;
}
