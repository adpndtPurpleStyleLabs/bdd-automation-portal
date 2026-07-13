package com.bdd.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "feature_file")
@Getter
@Setter
public class FeatureFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String relativePath;

    private String folder;

    @Column(length = 1000)
    private String description;

    private String tags;

    private int scenarioCount;

    private LocalDateTime lastModified;

    private boolean enabled = true;
}
