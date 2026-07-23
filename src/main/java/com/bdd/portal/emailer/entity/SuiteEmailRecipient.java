package com.bdd.portal.emailer.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "suite_email_recipients")
@Data
public class SuiteEmailRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "suite_id", nullable = false)
    private Long suiteId; // Maps to ScheduledJob ID (Test Suite)

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "notify_on_pass")
    private boolean notifyOnPass = true;

    @Column(name = "notify_on_fail")
    private boolean notifyOnFail = true;

    @Column(name = "notify_on_cancel")
    private boolean notifyOnCancel = true;

    @Column(name = "notify_on_start")
    private boolean notifyOnStart = true;
}
