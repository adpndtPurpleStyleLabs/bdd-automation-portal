package com.bdd.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_name")
    private String username;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false, updatable = false)
    private LocalDateTime time;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column
    private String browser;

    @Column(columnDefinition = "TEXT")
    private String details;

    @PrePersist
    protected void onCreate() {
        time = LocalDateTime.now();
    }
}
