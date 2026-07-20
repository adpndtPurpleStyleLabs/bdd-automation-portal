package com.bdd.portal.emailer.service;

import com.bdd.portal.emailer.entity.EmailQueue;
import com.bdd.portal.emailer.repository.EmailQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class EmailQueueProcessor {

    private final EmailQueueRepository emailQueueRepository;
    private final EmailService emailService;
    private static final int MAX_RETRIES = 3;

    @Scheduled(fixedDelay = 60000) // Every minute
    @Transactional
    public void processQueue() {
        List<EmailQueue> pendingEmails = emailQueueRepository.findByStatusIn(
                Arrays.asList(EmailQueue.EmailStatus.PENDING, EmailQueue.EmailStatus.FAILED)
        );

        for (EmailQueue email : pendingEmails) {
            if (email.getRetryCount() >= MAX_RETRIES) {
                continue; // Skip if max retries reached
            }

            email.setStatus(EmailQueue.EmailStatus.PROCESSING);
            emailQueueRepository.save(email);

            try {
                List<java.io.File> attachmentFiles = null;
                if (email.getAttachments() != null && !email.getAttachments().isEmpty()) {
                    attachmentFiles = new java.util.ArrayList<>();
                    String[] paths = email.getAttachments().split(",");
                    for (String path : paths) {
                        java.io.File file = new java.io.File(System.getProperty("user.dir") + "/bdd-reports/" + path.replace("/reports/", ""));
                        if (file.exists()) {
                            attachmentFiles.add(file);
                        }
                    }
                }
                
                emailService.sendEmail(email.getRecipient(), email.getSubject(), email.getBody(), attachmentFiles);
                
                email.setStatus(EmailQueue.EmailStatus.SENT);
                email.setSentAt(LocalDateTime.now());
                email.setLastError(null);
            } catch (Exception e) {
                log.error("Failed to send email to {}", email.getRecipient(), e);
                email.setStatus(EmailQueue.EmailStatus.FAILED);
                email.setLastError(e.getMessage());
                email.setRetryCount(email.getRetryCount() + 1);
            }
            
            emailQueueRepository.save(email);
        }
    }
}
