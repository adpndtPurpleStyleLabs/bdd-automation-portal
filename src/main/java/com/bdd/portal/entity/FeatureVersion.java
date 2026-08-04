package com.bdd.portal.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feature_version")
@Getter
@Setter
public class FeatureVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_file_id", nullable = false)
    private FeatureFile featureFile;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false, length = 64)
    private String fileHash;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VersionStatus status = VersionStatus.ACTIVE;

    @Column(length = 1000)
    private String description;

    private String tags;

    private int scenarioCount;

    private int stepCount;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "featureVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Scenario> scenarios = new ArrayList<>();
}
