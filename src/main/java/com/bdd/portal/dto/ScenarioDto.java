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
    private String type; // "Scenario" or "Scenario Outline"
    private List<String> tags;
    private int stepCount;
    private ExecutionStatus status;
    private Long durationMs;
    private LocalDateTime lastRun;
}
