package com.bdd.portal.emailer.event;

import com.bdd.portal.emailer.entity.EmailQueue;
import com.bdd.portal.emailer.entity.SuiteEmailRecipient;
import com.bdd.portal.emailer.repository.EmailQueueRepository;
import com.bdd.portal.emailer.repository.SuiteEmailRecipientRepository;
import com.bdd.portal.emailer.service.EmailTemplateService;
import com.bdd.portal.entity.Execution;
import com.bdd.portal.event.ExecutionCancelledEvent;
import com.bdd.portal.event.ExecutionCompletedEvent;
import com.bdd.portal.event.ExecutionStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.modulith.events.ApplicationModuleListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionEventListener {

    private final SuiteEmailRecipientRepository recipientRepository;
    private final EmailQueueRepository emailQueueRepository;
    private final EmailTemplateService emailTemplateService;

    @EventListener
    @Async
    public void onExecutionStarted(ExecutionStartedEvent event) {
        Execution execution = event.getExecution();
        if (execution.getScheduledJob() == null) return;

        List<SuiteEmailRecipient> recipients = recipientRepository.findBySuiteId(execution.getScheduledJob().getId());
        for (SuiteEmailRecipient recipient : recipients) {
            if (recipient.isNotifyOnStart()) {
                queueEmail(recipient.getEmail(), 
                          "Execution Started: " + execution.getScheduledJob().getJobName(),
                          emailTemplateService.generateExecutionEmail(execution, "execution-started.html"),
                          null);
            }
        }
    }

    @EventListener
    @Async
    public void onExecutionCompleted(ExecutionCompletedEvent event) {
        Execution execution = event.getExecution();
        if (execution.getScheduledJob() != null) {
            List<SuiteEmailRecipient> recipients = recipientRepository.findBySuiteId(execution.getScheduledJob().getId());
            for (SuiteEmailRecipient recipient : recipients) {
                boolean isPassed = execution.getStatus() == com.bdd.portal.entity.ExecutionStatus.PASSED;
                if ((isPassed && recipient.isNotifyOnPass()) || (!isPassed && recipient.isNotifyOnFail())) {
                    String status = isPassed ? "Passed" : "Failed";
                    String attachments = execution.getAllureReportPath() != null ? execution.getAllureReportPath() : null;
                    queueEmail(recipient.getEmail(), 
                              "Execution " + status + ": " + execution.getScheduledJob().getJobName(),
                              emailTemplateService.generateExecutionEmail(execution, "execution-completed.html"),
                              attachments);
                }
            }
        }
        
        // Handle manual execution notifications
        if (execution.getNotifyEmails() != null && !execution.getNotifyEmails().trim().isEmpty()) {
            boolean isPassed = execution.getStatus() == com.bdd.portal.entity.ExecutionStatus.PASSED;
            String status = isPassed ? "Passed" : "Failed";
            String title = execution.getTargetFolder() != null ? execution.getTargetFolder() : (execution.getFeatureFile() != null ? execution.getFeatureFile().getName() : "Manual Run");
            String attachments = execution.getAllureReportPath() != null ? execution.getAllureReportPath() : null;
            
            String rawEmails = execution.getNotifyEmails().trim();
            if (rawEmails.startsWith("[")) {
                rawEmails = rawEmails.replaceAll("\\[|\\]|\\{|\\}|\"value\":|\"", "");
            }
            
            String[] emails = rawEmails.split(",");
            for (String email : emails) {
                if (!email.trim().isEmpty()) {
                    queueEmail(email.trim(), 
                              "Manual Execution " + status + ": " + title,
                              emailTemplateService.generateExecutionEmail(execution, "execution-completed.html"),
                              attachments);
                }
            }
        }
    }

    @EventListener
    @Async
    public void onExecutionCancelled(ExecutionCancelledEvent event) {
        Execution execution = event.getExecution();
        if (execution.getScheduledJob() == null) return;

        List<SuiteEmailRecipient> recipients = recipientRepository.findBySuiteId(execution.getScheduledJob().getId());
        for (SuiteEmailRecipient recipient : recipients) {
            if (recipient.isNotifyOnCancel()) {
                queueEmail(recipient.getEmail(), 
                          "Execution Cancelled: " + execution.getScheduledJob().getJobName(),
                          emailTemplateService.generateExecutionEmail(execution, "execution-cancelled.html"),
                          null);
            }
        }
    }

    private void queueEmail(String to, String subject, String body, String attachments) {
        EmailQueue queue = new EmailQueue();
        queue.setRecipient(to);
        queue.setSubject(subject);
        queue.setBody(body);
        queue.setAttachments(attachments);
        emailQueueRepository.save(queue);
    }
}
