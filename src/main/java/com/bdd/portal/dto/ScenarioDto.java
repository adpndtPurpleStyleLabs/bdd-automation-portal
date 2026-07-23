package com.bdd.portal.dto;

import com.bdd.portal.entity.ExecutionStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioDto {
    private String name;
    private String slug;
    private int line;
    private String type; // "Scenario" or "Scenario Outline"
    private List<String> tags;
    private Integer stepCount = 0;
    private List<String> steps;
    private ExecutionStatus status;
    private ExecutionStatus previousStatus;
    private Long durationMs;
    private LocalDateTime lastRun;
}
