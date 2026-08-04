package com.bdd.portal.emailer.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_queue")
@Data
public class EmailQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "body", columnDefinition = "LONGTEXT")
    private String body;

    @Column(name = "attachments", columnDefinition = "TEXT")
    private String attachments; // Comma separated file paths

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmailStatus status = EmailStatus.PENDING;

    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    public enum EmailStatus {
        PENDING, PROCESSING, SENT, FAILED
    }
}
