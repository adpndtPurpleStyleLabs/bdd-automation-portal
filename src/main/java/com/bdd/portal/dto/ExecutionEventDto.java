package com.bdd.portal.dto;

import com.bdd.portal.entity.Execution;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class ExecutionEventDto {
    private Long id;
    private String target;
    private String environment;
    private String browser;
    private String type;
    private String startTime;
    private String duration;
    private String status;
    private String allureReportPath;

    public static ExecutionEventDto fromExecution(Execution execution) {
        ExecutionEventDto dto = new ExecutionEventDto();
        dto.setId(execution.getId());
        
        if (execution.getFeatureFile() != null) {
            dto.setTarget(execution.getFeatureFile().getName());
        } else {
            dto.setTarget(execution.getTargetFolder());
        }
        
        dto.setEnvironment(execution.getEnvironment());
        dto.setBrowser(execution.getBrowser());
        dto.setType(execution.getExecutionType().name());
        
        if (execution.getStartTime() != null) {
            dto.setStartTime(execution.getStartTime().format(DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss")));
        } else {
            dto.setStartTime("-");
        }
        
        if (execution.getDurationMs() != null) {
            dto.setDuration(String.format("%.1fs", execution.getDurationMs() / 1000.0));
        } else {
            dto.setDuration("-");
        }
        
        dto.setStatus(execution.getStatus().name());
        dto.setAllureReportPath(execution.getAllureReportPath());
        
        return dto;
    }
}
