package com.bdd.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "settings")
@Getter
@Setter
public class Settings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String defaultBrowser = "Chrome";
    private int parallelThreads = 1;
    private String defaultEnvironment = "QA";
    private String screenshotPath = "target/screenshots";
    private String reportPath = "target/allure-reports";
    private String featureFolderPath = "src/test/resources/features";
    private boolean headlessMode = true;
    private int executionTimeoutMinutes = 60;
    private int retryCount = 0;
}
