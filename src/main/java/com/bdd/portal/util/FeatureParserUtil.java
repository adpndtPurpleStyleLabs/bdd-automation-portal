package com.bdd.portal.util;

import com.bdd.portal.dto.ScenarioDto;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FeatureParserUtil {

    public static List<ScenarioDto> parseFeatureFile(Path filePath) {
        List<ScenarioDto> scenarios = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(filePath);
            ScenarioDto currentScenario = null;
            List<String> currentTags = new ArrayList<>();

            for (String line : lines) {
                String trimmed = line.trim();
                
                // Collect tags
                if (trimmed.startsWith("@")) {
                    String[] tags = trimmed.split("\\s+");
                    for (String t : tags) {
                        if (t.startsWith("@")) {
                            currentTags.add(t);
                        }
                    }
                } 
                // Detect Scenario or Scenario Outline
                else if (trimmed.startsWith("Scenario:") || trimmed.startsWith("Scenario Outline:")) {
                    if (currentScenario != null) {
                        scenarios.add(currentScenario);
                    }
                    currentScenario = new ScenarioDto();
                    currentScenario.setTags(new ArrayList<>(currentTags));
                    
                    if (trimmed.startsWith("Scenario:")) {
                        currentScenario.setType("Scenario");
                        currentScenario.setName(trimmed.substring("Scenario:".length()).trim());
                    } else {
                        currentScenario.setType("Scenario Outline");
                        currentScenario.setName(trimmed.substring("Scenario Outline:".length()).trim());
                    }
                    currentTags.clear(); // Clear tags for the next scenario
                } 
                // Detect steps
                else if (currentScenario != null && (
                        trimmed.startsWith("Given ") || trimmed.startsWith("When ") || 
                        trimmed.startsWith("Then ") || trimmed.startsWith("And ") || 
                        trimmed.startsWith("But ") || trimmed.startsWith("* "))) {
                    currentScenario.setStepCount(currentScenario.getStepCount() + 1);
                }
            }
            
            // Add the last scenario
            if (currentScenario != null) {
                scenarios.add(currentScenario);
            }
            
        } catch (IOException e) {
            log.error("Failed to parse feature file: {}", filePath, e);
        }
        return scenarios;
    }
}
