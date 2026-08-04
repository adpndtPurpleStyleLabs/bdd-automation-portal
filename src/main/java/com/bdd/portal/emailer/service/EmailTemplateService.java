package com.bdd.portal.emailer.service;

import com.bdd.portal.entity.Execution;
import com.bdd.portal.entity.ExecutionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    public String generateExecutionEmail(Execution execution, String templateName) {
        Context context = new Context();
        context.setVariable("execution", execution);
        
        // Add suite name mapping. Assuming execution.getScheduledJob() has the job name,
        // or we fallback to FeatureFile name.
        String suiteName = "Manual Execution";
        if (execution.getScheduledJob() != null) {
            suiteName = execution.getScheduledJob().getJobName();
        } else if (execution.getFeatureFile() != null) {
            suiteName = execution.getFeatureFile().getName();
        } else if (execution.getTargetFolder() != null) {
            suiteName = execution.getTargetFolder();
        }
        context.setVariable("suiteName", suiteName);

        // Calculate pass percentage
        int total = execution.getPassedScenarios() + execution.getFailedScenarios() + execution.getSkippedScenarios();
        int passPercentage = total == 0 ? 0 : (execution.getPassedScenarios() * 100) / total;
        context.setVariable("passPercentage", passPercentage);
        context.setVariable("totalScenarios", total);

        // Theme
        String theme = "green";
        if (execution.getStatus() == ExecutionStatus.FAILED) {
            theme = "red";
        }
        context.setVariable("theme", theme);

        return templateEngine.process("email/" + templateName, context);
    }
}
