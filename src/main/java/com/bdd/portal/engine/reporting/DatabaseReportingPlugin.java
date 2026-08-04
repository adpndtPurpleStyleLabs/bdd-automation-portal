package com.bdd.portal.engine.reporting;

import com.bdd.portal.config.SpringContext;
import com.bdd.portal.entity.*;
import com.bdd.portal.repository.*;
import com.bdd.portal.service.WebSocketNotificationService;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

@Slf4j
public class DatabaseReportingPlugin implements ConcurrentEventListener {

    private ScenarioExecutionRepository scenarioExecutionRepository;
    private StepExecutionRepository stepExecutionRepository;
    private WebSocketNotificationService notificationService;

    private final String scenarioExecutionUuid;
    private Long scenarioId;
    
    // Instance-level state tracking the currently running step
    public static final ThreadLocal<Long> currentStepId = new ThreadLocal<>();

    public DatabaseReportingPlugin(String scenarioExecutionUuid) {
        this.scenarioExecutionUuid = scenarioExecutionUuid;
    }

    private void initBeans() {
        if (scenarioExecutionRepository == null) {
            scenarioExecutionRepository = SpringContext.getBean(ScenarioExecutionRepository.class);
            stepExecutionRepository = SpringContext.getBean(StepExecutionRepository.class);
            notificationService = SpringContext.getBean(WebSocketNotificationService.class);
        }
        
        if (scenarioId == null) {
            scenarioExecutionRepository.findByScenarioExecutionUuid(scenarioExecutionUuid)
                .ifPresent(s -> scenarioId = s.getId());
        }
    }

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::handleTestRunStarted);
        publisher.registerHandlerFor(TestStepStarted.class, this::handleTestStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::handleTestStepFinished);
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
        publisher.registerHandlerFor(WriteEvent.class, this::handleWriteEvent);
        publisher.registerHandlerFor(EmbedEvent.class, this::handleEmbedEvent);
    }

    private void handleTestRunStarted(TestRunStarted event) {
        initBeans();
        log.info("Cucumber Test Run Started for scenario UUID: {}", scenarioExecutionUuid);
    }

    private void handleTestStepStarted(TestStepStarted event) {
        initBeans();
        if (scenarioId == null) return;
        
        ScenarioExecution scenario = scenarioExecutionRepository.findById(scenarioId).orElse(null);
        if (scenario != null) {
            StepExecution step = new StepExecution();
            step.setScenarioExecution(scenario);
            step.setStepUuid(java.util.UUID.randomUUID().toString());
            
            if (event.getTestStep() instanceof PickleStepTestStep) {
                PickleStepTestStep pickleStep = (PickleStepTestStep) event.getTestStep();
                step.setStepName(pickleStep.getStep().getText());
                step.setKeyword(pickleStep.getStep().getKeyword());
                step.setLineNumber(pickleStep.getStep().getLine());
            } else if (event.getTestStep() instanceof HookTestStep) {
                HookTestStep hookStep = (HookTestStep) event.getTestStep();
                step.setStepName(hookStep.getHookType().toString() + " Hook");
                step.setKeyword("Hook");
                step.setLineNumber(0);
            } else {
                return;
            }
            
            step.setStatus(ExecutionStatus.RUNNING);
            
            step = stepExecutionRepository.save(step);
            currentStepId.set(step.getId());
            
            // Broadcast live step start if you want real-time UI
            notificationService.sendExecutionLog(scenario.getExecution().getId(), "Running step: " + step.getStepName());
        }
    }

    private void handleTestStepFinished(TestStepFinished event) {
        initBeans();
        Long stepId = currentStepId.get();
        if (stepId != null) {
            StepExecution step = stepExecutionRepository.findById(stepId).orElse(null);
            
            if (step != null) {
                if (event.getResult().getDuration() != null) {
                    step.setDurationMs(event.getResult().getDuration().toMillis());
                }
                
                switch (event.getResult().getStatus()) {
                    case PASSED:
                        step.setStatus(ExecutionStatus.PASSED);
                        break;
                    case FAILED:
                        step.setStatus(ExecutionStatus.FAILED);
                        if (event.getResult().getError() != null) {
                            Throwable error = event.getResult().getError();
                            String errMsg = error.getMessage() != null ? error.getMessage() : error.getClass().getName();
                            
                            StackTraceElement failingElement = null;
                            for (StackTraceElement el : error.getStackTrace()) {
                                if (el.getClassName().startsWith("com.bdd.portal") && !el.getClassName().contains("DatabaseReportingPlugin")) {
                                    failingElement = el;
                                    break;
                                }
                            }
                            if (failingElement == null && error.getStackTrace().length > 0) {
                                failingElement = error.getStackTrace()[0];
                            }
                            
                            if (failingElement != null) {
                                String failedLocation = "Location: " + failingElement.getClassName() + "." + failingElement.getMethodName() + 
                                                      " (" + failingElement.getFileName() + ":" + failingElement.getLineNumber() + ")";
                                errMsg = errMsg + "\n\n" + failedLocation;
                            }
                            
                            // Strip non-ASCII characters to prevent DB encoding errors (e.g. Cucumber's ✽)
                            errMsg = errMsg.replaceAll("[^\\x00-\\x7F]", "");
                            step.setErrorMessage(errMsg);
                            
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            error.printStackTrace(pw);
                            
                            String stackTraceString = sw.toString();
                            // Strip non-ASCII characters to prevent DB encoding errors (e.g. Cucumber's ✽)
                            stackTraceString = stackTraceString.replaceAll("[^\\x00-\\x7F]", "");
                            
                            if (stackTraceString.length() > 60000) {
                                stackTraceString = stackTraceString.substring(0, 60000) + "... [TRUNCATED]";
                            }
                            step.setStackTrace(stackTraceString);
                        }
                        break;
                    case SKIPPED:
                        step.setStatus(ExecutionStatus.SKIPPED);
                        break;
                    default:
                        step.setStatus(ExecutionStatus.FAILED);
                }
                
                stepExecutionRepository.save(step);
            }
            currentStepId.remove();
        }
    }

    private void handleTestCaseFinished(TestCaseFinished event) {
        // Handled by ExecutorWorker directly (QueueService.markScenarioComplete) 
        // to avoid duplicate DB calls, but we can capture additional info if needed.
    }

    private void handleWriteEvent(WriteEvent event) {
        initBeans();
        Long stepId = currentStepId.get();
        if (stepId != null) {
            StepExecution step = stepExecutionRepository.findById(stepId).orElse(null);
            if (step != null) {
                String existing = step.getStepLog() == null ? "" : step.getStepLog() + "\n";
                step.setStepLog(existing + event.getText());
                stepExecutionRepository.save(step);
            }
        }
    }

    private void handleEmbedEvent(EmbedEvent event) {
        initBeans();
        Long stepId = currentStepId.get();
        if (stepId != null) {
            StepExecution step = stepExecutionRepository.findById(stepId).orElse(null);
            if (step != null) {
                if (event.getMediaType().startsWith("image/")) {
                    String base64 = java.util.Base64.getEncoder().encodeToString(event.getData());
                    String imgTag = "\n[" + java.time.LocalDateTime.now().toString() + "] [SCREENSHOT] data:" + event.getMediaType() + ";base64," + base64 + "\n";
                    String existing = step.getStepLog() == null ? "" : step.getStepLog();
                    step.setStepLog(existing + imgTag);
                    stepExecutionRepository.save(step);
                } else if (event.getMediaType().startsWith("text/")) {
                    String text = new String(event.getData(), java.nio.charset.StandardCharsets.UTF_8);
                    String existing = step.getStepLog() == null ? "" : step.getStepLog() + "\n";
                    step.setStepLog(existing + text);
                    stepExecutionRepository.save(step);
                }
            }
        }
    }
}
