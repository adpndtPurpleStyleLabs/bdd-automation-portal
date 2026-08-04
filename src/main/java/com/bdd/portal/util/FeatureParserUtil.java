package com.bdd.portal.util;

import com.bdd.portal.dto.ExampleDto;
import com.bdd.portal.dto.ScenarioDto;
import com.bdd.portal.dto.StepDto;
import io.cucumber.gherkin.GherkinParser;
import io.cucumber.messages.types.*;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class FeatureParserUtil {

    public static List<ScenarioDto> parseFeatureFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            return parseFeatureContent(content, filePath.toString());
        } catch (IOException e) {
            log.error("Failed to read feature file: {}", filePath, e);
            return new ArrayList<>();
        }
    }

    public static List<ScenarioDto> parseFeatureContent(String content, String sourceName) {
        List<ScenarioDto> scenarioDtos = new ArrayList<>();
        try {
            GherkinParser parser = GherkinParser.builder().build();
            List<Envelope> envelopes = parser.parse(sourceName, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))).collect(Collectors.toList());
            
            for (Envelope envelope : envelopes) {
                if (envelope.getGherkinDocument().isPresent()) {
                    GherkinDocument doc = envelope.getGherkinDocument().get();
                    if (doc.getFeature().isPresent()) {
                        Feature feature = doc.getFeature().get();
                        Set<String> usedScenarioSlugs = new HashSet<>();
                        for (FeatureChild child : feature.getChildren()) {
                            if (child.getScenario().isPresent()) {
                                Scenario scenario = child.getScenario().get();
                                ScenarioDto dto = new ScenarioDto();
                                dto.setName(scenario.getName());
                                // determine type by keyword (Scenario vs Scenario Outline)
                                dto.setType(scenario.getKeyword());
                                dto.setSlug(makeSlug(scenario.getName(), usedScenarioSlugs));
                                dto.setLine(scenario.getLocation().getLine().intValue());
                                
                                List<String> tags = new ArrayList<>();
                                scenario.getTags().forEach(t -> tags.add(t.getName()));
                                dto.setTags(tags);
                                
                                List<StepDto> steps = new ArrayList<>();
                                for (Step step : scenario.getSteps()) {
                                    StepDto stepDto = new StepDto();
                                    stepDto.setKeyword(step.getKeyword().trim());
                                    stepDto.setText(step.getText());
                                    
                                    if (step.getDataTable().isPresent()) {
                                        List<List<String>> table = new ArrayList<>();
                                        step.getDataTable().get().getRows().forEach(row -> {
                                            List<String> rowCells = new ArrayList<>();
                                            row.getCells().forEach(cell -> rowCells.add(cell.getValue()));
                                            table.add(rowCells);
                                        });
                                        stepDto.setDataTable(table);
                                    }
                                    if (step.getDocString().isPresent()) {
                                        stepDto.setDocString(step.getDocString().get().getContent());
                                    }
                                    steps.add(stepDto);
                                }
                                dto.setSteps(steps);
                                dto.setStepCount(steps.size());
                                
                                List<ExampleDto> examples = new ArrayList<>();
                                for (Examples ex : scenario.getExamples()) {
                                    ExampleDto exDto = new ExampleDto();
                                    exDto.setName(ex.getName());
                                    if (ex.getTableHeader().isPresent()) {
                                        List<String> headers = new ArrayList<>();
                                        ex.getTableHeader().get().getCells().forEach(c -> headers.add(c.getValue()));
                                        exDto.setHeaders(headers);
                                    }
                                    List<List<String>> rows = new ArrayList<>();
                                    for (TableRow row : ex.getTableBody()) {
                                        List<String> rowCells = new ArrayList<>();
                                        row.getCells().forEach(c -> rowCells.add(c.getValue()));
                                        rows.add(rowCells);
                                    }
                                    exDto.setRows(rows);
                                    examples.add(exDto);
                                }
                                dto.setExamples(examples);
                                
                                scenarioDtos.add(dto);
                            }
                        }
                    }
                }
            }
        } catch (java.lang.Exception e) {
            log.error("Failed to parse gherkin content", e);
        }
        return scenarioDtos;
    }
    
    private static String makeSlug(String input, Set<String> usedSlugs) {
        if (input == null || input.isEmpty()) {
            input = "untitled";
        }
        String baseSlug = input.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
                
        String slug = baseSlug;
        int counter = 2;
        while (usedSlugs.contains(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        usedSlugs.add(slug);
        return slug;
    }
}
