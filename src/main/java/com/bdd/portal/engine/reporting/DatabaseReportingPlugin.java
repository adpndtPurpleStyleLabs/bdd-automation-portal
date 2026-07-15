package com.bdd.portal.engine.reporting;

import com.bdd.portal.config.SpringContext;
import com.bdd.portal.entity.*;
import com.bdd.portal.repository.ExecutionRepository;
import com.bdd.portal.repository.FeatureExecutionRepository;
import com.bdd.portal.repository.ScenarioExecutionRepository;
import com.bdd.portal.repository.StepExecutionRepository;
import com.bdd.portal.service.WebSocketNotificationService;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DatabaseReportingPlugin implements ConcurrentEventListener {

    private ExecutionRepository executionRepository;
    private FeatureExecutionRepository featureExecutionRepository;
    private ScenarioExecutionRepository scenarioExecutionRepository;
    private StepExecutionRepository stepExecutionRepository;
    private WebSocketNotificationService notificationService;

    // We store mappings using URIs and UUIDs from Cucumber events
    private final Map<String, FeatureExecution> featureMap = new ConcurrentHashMap<>();
    private final Map<String, ScenarioExecution> scenarioMap = new ConcurrentHashMap<>();
    private final Map<String, StepExecution> stepMap = new ConcurrentHashMap<>();
    public static final ThreadLocal<Long> currentStepId = new ThreadLocal<>();

    private Long currentExecutionId;

    public DatabaseReportingPlugin() {
        // Initialization can't fetch beans yet because Spring might not be fully up if this runs too early,
        // but since we invoke Main.run inside an Async method, SpringContext is definitely ready.
    }

    private void initBeans() {
        if (executionRepository == null) {
            executionRepository = SpringContext.getBean(ExecutionRepository.class);
            featureExecutionRepository = SpringContext.getBean(FeatureExecutionRepository.class);
            scenarioExecutionRepository = SpringContext.getBean(ScenarioExecutionRepository.class);
            stepExecutionRepository = SpringContext.getBean(StepExecutionRepository.class);
            notificationService = SpringContext.getBean(WebSocketNotificationService.class);

            // Get current execution ID from system properties or a thread local.
            // For simplicity, we pass it via System Property in ExecutionEngineService
            String execIdStr = System.getProperty("current.execution.id");
            if (execIdStr != null) {
                currentExecutionId = Long.parseLong(execIdStr);
            }
        }
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::handleTestRunStarted);
        publisher.registerHandlerFor(TestSourceRead.class, this::handleTestSourceRead);
        publisher.registerHandlerFor(TestCaseStarted.class, this::handleTestCaseStarted);
        publisher.registerHandlerFor(TestStepStarted.class, this::handleTestStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::handleTestStepFinished);
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::handleTestRunFinished);
    }

    private void handleTestRunStarted(TestRunStarted event) {
        initBeans();
        log.info("Cucumber Test Run Started");
    }

    private void handleTestSourceRead(TestSourceRead event) {
        initBeans();
        // Here we know a feature file is being read. We can create a FeatureExecution in QUEUED state.
        if (currentExecutionId == null) return;

        Optional<Execution> executionOpt = executionRepository.findById(currentExecutionId);
        if (executionOpt.isEmpty()) return;

        String uri = event.getUri().toString();
        // Get feature name by parsing or just using URI as name initially
        String featureName = uri.substring(uri.lastIndexOf('/') + 1);

        FeatureExecution featureExecution = new FeatureExecution();
        featureExecution.setExecution(executionOpt.get());
        featureExecution.setUri(uri);
        featureExecution.setFeatureName(featureName);
        featureExecution.setStatus(ExecutionStatus.QUEUED);

        featureExecution = featureExecutionRepository.save(featureExecution);
        featureMap.put(uri, featureExecution);
        
        notificationService.sendExecutionLog(currentExecutionId, "Feature loaded: " + featureName);
        notificationService.sendExecutionStatusUpdate(currentExecutionId, "FEATURE_LOADED");
    }

    private void handleTestCaseStarted(TestCaseStarted event) {
        initBeans();
        if (currentExecutionId == null) return;
        
        String uri = event.getTestCase().getUri().toString();
        FeatureExecution featureExecution = featureMap.get(uri);
        
        if (featureExecution != null) {
            // If feature was queued, mark it running
            if (featureExecution.getStatus() == ExecutionStatus.QUEUED) {
                featureExecution.setStatus(ExecutionStatus.RUNNING);
                featureExecution.setStartTime(LocalDateTime.now());
                featureExecutionRepository.save(featureExecution);
                notificationService.sendExecutionStatusUpdate(currentExecutionId, "FEATURE_RUNNING");
            }
            
            ScenarioExecution scenario = new ScenarioExecution();
            scenario.setFeatureExecution(featureExecution);
            scenario.setScenarioName(event.getTestCase().getName());
            scenario.setLineNumber(event.getTestCase().getLocation().getLine());
            scenario.setStatus(ExecutionStatus.RUNNING);
            scenario.setStartTime(LocalDateTime.now());
            
            scenario = scenarioExecutionRepository.save(scenario);
            scenarioMap.put(event.getTestCase().getId().toString(), scenario);
            
            notificationService.sendExecutionStatusUpdate(currentExecutionId, "SCENARIO_RUNNING");
        }
    }

    private void handleTestStepStarted(TestStepStarted event) {
        initBeans();
        if (currentExecutionId == null) return;
        
        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep pickleStep = (PickleStepTestStep) event.getTestStep();
            
            String scenarioId = event.getTestCase().getId().toString();
            ScenarioExecution scenario = scenarioMap.get(scenarioId);
            
            if (scenario != null) {
                StepExecution step = new StepExecution();
                step.setScenarioExecution(scenario);
                step.setStepName(pickleStep.getStep().getText());
                step.setKeyword(pickleStep.getStep().getKeyword());
                step.setLineNumber(pickleStep.getStep().getLine());
                step.setStatus(ExecutionStatus.RUNNING);
                
                step = stepExecutionRepository.save(step);
                stepMap.put(event.getTestStep().getId().toString(), step);
                currentStepId.set(step.getId());
            }
        }
    }

    private void handleTestStepFinished(TestStepFinished event) {
        initBeans();
        if (currentExecutionId == null) return;
        
        if (event.getTestStep() instanceof PickleStepTestStep) {
            String stepId = event.getTestStep().getId().toString();
            StepExecution step = stepMap.get(stepId);
            
            if (step != null) {
                StepExecution latestStep = stepExecutionRepository.findById(step.getId()).orElse(step);
                latestStep.setDurationMs(event.getResult().getDuration().toMillis());
                
                switch (event.getResult().getStatus()) {
                    case PASSED:
                        latestStep.setStatus(ExecutionStatus.PASSED);
                        break;
                    case FAILED:
                        latestStep.setStatus(ExecutionStatus.FAILED);
                        if (event.getResult().getError() != null) {
                            latestStep.setErrorMessage(event.getResult().getError().getMessage());
                            // Add stack trace logic here if needed
                        }
                        break;
                    case SKIPPED:
                        latestStep.setStatus(ExecutionStatus.SKIPPED);
                        break;
                    default:
                        latestStep.setStatus(ExecutionStatus.FAILED);
                }
                
                
                stepExecutionRepository.save(latestStep);
                notificationService.sendExecutionStatusUpdate(currentExecutionId, "STEP_FINISHED");
            }
            currentStepId.remove();
        }
    }

    private void handleTestCaseFinished(TestCaseFinished event) {
        initBeans();
        if (currentExecutionId == null) return;
        
        String scenarioId = event.getTestCase().getId().toString();
        ScenarioExecution scenario = scenarioMap.get(scenarioId);
        
        if (scenario != null) {
            scenario.setEndTime(LocalDateTime.now());
            scenario.setDurationMs(event.getResult().getDuration().toMillis());
            
            switch (event.getResult().getStatus()) {
                case PASSED:
                    scenario.setStatus(ExecutionStatus.PASSED);
                    break;
                case FAILED:
                    scenario.setStatus(ExecutionStatus.FAILED);
                    break;
                case SKIPPED:
                    scenario.setStatus(ExecutionStatus.SKIPPED);
                    break;
                default:
                    scenario.setStatus(ExecutionStatus.FAILED);
            }
            
            scenarioExecutionRepository.save(scenario);
            notificationService.sendExecutionStatusUpdate(currentExecutionId, "SCENARIO_FINISHED");
            
            // Increment passed/failed counts on Execution
            Optional<Execution> executionOpt = executionRepository.findById(currentExecutionId);
            if (executionOpt.isPresent()) {
                Execution exec = executionOpt.get();
                if (scenario.getStatus() == ExecutionStatus.PASSED) {
                    exec.setPassedScenarios(exec.getPassedScenarios() + 1);
                } else if (scenario.getStatus() == ExecutionStatus.FAILED) {
                    exec.setFailedScenarios(exec.getFailedScenarios() + 1);
                } else {
                    exec.setSkippedScenarios(exec.getSkippedScenarios() + 1);
                }
                executionRepository.save(exec);
            }
        }
    }

    private void handleTestRunFinished(TestRunFinished event) {
        initBeans();
        if (currentExecutionId == null) return;
        
        // Finish any running features
        for (FeatureExecution feature : featureMap.values()) {
            if (feature.getStatus() == ExecutionStatus.RUNNING) {
                // Determine status based on scenarios
                boolean hasFailed = scenarioExecutionRepository.findByFeatureExecutionId(feature.getId())
                        .stream().anyMatch(s -> s.getStatus() == ExecutionStatus.FAILED);
                
                feature.setStatus(hasFailed ? ExecutionStatus.FAILED : ExecutionStatus.PASSED);
                feature.setEndTime(LocalDateTime.now());
                if (feature.getStartTime() != null) {
                    feature.setDurationMs(java.time.Duration.between(feature.getStartTime(), feature.getEndTime()).toMillis());
                }
                featureExecutionRepository.save(feature);
            }
        }
        
        notificationService.sendExecutionStatusUpdate(currentExecutionId, "TEST_RUN_FINISHED");
    }
}
